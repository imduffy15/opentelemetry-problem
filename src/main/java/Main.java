import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.TracingContextUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Pattern;

public class Main {
    private static final String CONFIGURATION_FILE = "config";
    private static final String CONFIGURATION_FILE_DEFAULT = "config.properties";
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Pattern ENV_REPLACEMENT = Pattern.compile("[^a-zA-Z0-9_]");

    private static final String topic = "duffy-test";

    public static void main(String[] args) {
        Properties kafkaProps = loadConfigurationFile();
        KafkaProducer producer = new KafkaProducer(kafkaProps);
        KafkaConsumer consumer = new KafkaConsumer(kafkaProps);

        String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

        log.info("Creating context with traceparent {}", traceparent);

        Context context = OpenTelemetry.getPropagators().getHttpTextFormat().extract(Context.current(), traceparent, (carrier, key) -> {
            if (key.equals("traceparent")) {
                return carrier;
            } else {
                return null;
            }
        });

        Span span = TracingContextUtils.getSpan(context);
        log.info("Context created with traceId {}", span.getContext().getTraceId());

        try (Scope scope = TracingContextUtils.currentContextWith(span)) {
            log.info("Currect context traceId is {}", TracingContextUtils.getCurrentSpan().getContext().getTraceId());
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, "hello", "world");
            producer.send(record);
        }
        producer.flush();
        producer.close();

        consumer.subscribe(Collections.singleton(topic));

        boolean running = true;

        while (running) {
            final ConsumerRecords<String, String> consumerRecords =
                    consumer.poll(1000);

            for (ConsumerRecord<String, String> record : consumerRecords) {
                Header traceparentHeader = record.headers().lastHeader("traceparent");
                if(traceparentHeader != null) {
                    String foundtraceparent = new String(traceparentHeader.value(), StandardCharsets.UTF_8);
                    if (foundtraceparent.equals(traceparent)) {
                        log.info("Wanted traceparent found, exiting");
                        running = false;
                    } else {
                        log.info("Wanted traceparent not found, got {}" , foundtraceparent);
                    }
                } else {
                    log.info("No traceparent found");
                }
            }
            consumer.commitAsync();
        }
        consumer.close();

        log.info("Shutting down.....");
    }

    private static Properties loadConfigurationFile() {
        final Properties properties = new Properties();

        // Reading from system property first and from env after
        String configurationFilePath =
                System.getProperty(CONFIGURATION_FILE, CONFIGURATION_FILE_DEFAULT);
        if (null == configurationFilePath) {
            configurationFilePath =
                    System.getenv(propertyNameToEnvironmentVariableName(CONFIGURATION_FILE));
        }
        if (null == configurationFilePath) {
            return properties;
        }

        // Normalizing tilde (~) paths for unix systems
        configurationFilePath =
                configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));

        // Configuration properties file is optional
        final File configurationFile = new File(configurationFilePath);
        if (!configurationFile.exists()) {
            log.error("Configuration file '{}' not found.", configurationFilePath);
            return properties;
        }

        try (final FileReader fileReader = new FileReader(configurationFile)) {
            properties.load(fileReader);
        } catch (final FileNotFoundException fnf) {
            log.error("Configuration file '{}' not found.", configurationFilePath);
        } catch (final IOException ioe) {
            log.error(
                    "Configuration file '{}' cannot be accessed or correctly parsed.", configurationFilePath);
        }

        return properties;
    }

    private static String propertyNameToEnvironmentVariableName(final String setting) {
        return ENV_REPLACEMENT
                .matcher(setting.toUpperCase())
                .replaceAll("_");
    }
}
