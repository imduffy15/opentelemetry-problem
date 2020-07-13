# OpenTelemetry Problem

The kafka producer doesn't appear to get the traceId of the scope its wrapped in.

Output:

```
[2020-07-13 21:26:09,394: INFO/main] (Main.java:39) - Creating context with traceparent 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
[2020-07-13 21:26:09,543: INFO/main] (Main.java:50) - Context created with traceId TraceId{traceId=0af7651916cd43dd8448eb211c80319c}
[2020-07-13 21:26:09,549: INFO/main] (Main.java:53) - Currect context traceId is TraceId{traceId=0af7651916cd43dd8448eb211c80319c}
[2020-07-13 21:26:14,493: INFO/main] (Main.java:76) - Wanted traceparent not found, got 00-14c008a5563bc59746b0d34d2c21ab83-4284d42494d1a14c-01
```

Expected output:

```
[2020-07-13 21:26:09,394: INFO/main] (Main.java:39) - Creating context with traceparent 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
[2020-07-13 21:26:09,543: INFO/main] (Main.java:50) - Context created with traceId TraceId{traceId=0af7651916cd43dd8448eb211c80319c}
[2020-07-13 21:26:09,549: INFO/main] (Main.java:53) - Currect context traceId is TraceId{traceId=0af7651916cd43dd8448eb211c80319c}
[2020-07-13 21:26:14,493: INFO/main] (Main.java:76) - Wanted traceparent found, exiting
```
