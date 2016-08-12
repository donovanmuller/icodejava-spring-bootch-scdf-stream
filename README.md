# Spring Cloud Data Flow - Stream

This project is a demo from my I Code Java 2016 talk entitled "Stream is the new batch"

_This is the SCDF Stream centric implementation in contrast to the [SCDF Task example](https://github.com/donovanmuller/icodejava-spring-bootch-scdf-task)._

This project is a trivial Spring Cloud Data Flow implementation that monitors a directory using the
out-of-the-box [`file`](https://github.com/spring-cloud/spring-cloud-stream-app-starters/tree/master/file/spring-cloud-starter-stream-source-file)
source application. Once a file is received, each line in the file is sent to the custom `uppercase` processor app
as a separate message where it is uppercase'd. This uppercase'd message is then sent to the OOTB [`log`](https://github.com/spring-cloud/spring-cloud-stream-app-starters/tree/master/log/spring-cloud-starter-stream-sink-log) 
sink application where the message value is logged.

See the SCDF [reference documentation](http://docs.spring.io/spring-cloud-dataflow/docs/1.0.0.RELEASE/reference/htmlsingle) for more information

*Please see the [`openshift`](https://github.com/donovanmuller/icodejava-spring-bootch-scdf-stream/tree/openshift) branch for the OpenShift demo*

## Kafka

We will use the Kafka binder implementations of the apps, therefore we need a running Kafka broker.
The simplest way to stand up a Kafka instance is to use the a Docker image:

```
$ docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=`ipconfig getifaddr en0` --env ADVERTISED_PORT=9092 spotify/kafka
```

_Substitute `ipconfig getifaddr en0` where applicable_

## Spring Cloud Data Flow Local Server

The simplest way to run a SCDF server is using the Local variant, which will run apps locally.
Stand a local server up by first downloading the server and then running it:

```
$ wget http://repo.spring.io/release/org/springframework/cloud/spring-cloud-dataflow-server-local/1.0.0.RELEASE/spring-cloud-dataflow-server-local-1.0.0.RELEASE.jar
$ java -jar spring-cloud-dataflow-server-local-1.0.0.RELEASE.jar
```

## Spring Cloud Data Flow Shell

We will create and deploy our stream definitions using the SCDF Shell (you could also use the [dashboard](http://localhost:9393/dashboard))

```
$ wget http://repo.spring.io/release/org/springframework/cloud/spring-cloud-dataflow-shell/1.0.0.RELEASE/spring-cloud-dataflow-shell-1.0.0.RELEASE.jar 
$ java -jar spring-cloud-dataflow-shell-1.0.0.RELEASE.jar
```

The shell will connect to the SCDF server running at http://localhost:9393 by default:

```
dataflow:>dataflow config server
Successfully targeted http://localhost:9393/
```

## Registering OOTB apps

We will use the out-of-the-box `file` source and `log` sink apps in our stream definition.
Register these apps with the following:

```
dataflow:>app import --uri http://bit.ly/1-0-2-GA-stream-applications-kafka-maven
Successfully registered applications: [source.tcp, sink.jdbc, source.http, sink.rabbit, source.rabbit, source.ftp, sink.gpfdist, processor.transform, source.sftp, processor.filter, source.file, sink.cassandra, processor.groovy-filter, sink.router, source.trigger, sink.hdfs-dataset, processor.splitter, source.load-generator, processor.tcp-client, sink.file, source.time, source.gemfire, source.twitterstream, sink.tcp, source.jdbc, sink.field-value-counter, sink.redis-pubsub, sink.hdfs, processor.bridge, processor.pmml, processor.httpclient, sink.ftp, source.s3, sink.log, sink.gemfire, sink.aggregate-counter, sink.throughput, source.triggertask, sink.s3, source.gemfire-cq, source.jms, source.tcp-client, processor.scriptable-transform, sink.counter, sink.websocket, source.mongodb, source.mail, processor.groovy-transform, source.syslog]
```

## Registering uppercase processor

Now we will register our custom `uppercase` processor:

```
dataflow:>app register --name uppercase --uri "maven://i.code.java:uppercase-processor:1.0-SNAPSHOT" --type processor
Successfully registered application 'processor:uppercase'
```

Notice the `maven://` coordinate, this means the SCDF server will resolve our app using Maven.
This means we need to install our apps to our local Maven repo so that the server can find it:

```
$ ./mvnw install

...

[INFO] Reactor Summary:
[INFO] 
[INFO] spring-bootch-scdf-stream .......................... SUCCESS [  0.270 s]
[INFO] uppercase-processor ................................ SUCCESS [  2.092 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Creating the stream definition

Next we create the stream definition:

```
dataflow:>stream create --name uppercase --definition "file --directory=/tmp/icodejava/file --mode=lines | uppercase | log"
Created new stream 'uppercase'
```

Let's dissect this definition to understand what each app does:

* `file --directory=/tmp/icodejava/file --mode=lines` - Use the OOTB `file` source app to monitor the 
`/tmp/icodejava/file` directory for any files. Once a file is detected, split each line in the file into it's own
message, indicated by `--mode=lines`
* `uppercase` - Run each message through the `uppercase` processor app, which, as the name suggests, uppercases each message value
* `log` - Use the OOTB `log` sink application to simple log the received message (uppercase'd value) to the app log

This is what the `uppercase-processor` looks like:

```
@EnableBinding(Processor.class)
public class UppercaseProcessor {

    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    public String uppercase(String input) {
        return input.toUpperCase();
    }
}
```

## Deploy and test the stream

Now we can deploy our stream:

```
dataflow:>stream deploy --name uppercase
Deployed stream 'uppercase'
```

you should see something similar to the following in the SCDF server log:

```
...  INFO 45810 --- [nio-9393-exec-1] o.s.c.d.spi.local.LocalAppDeployer       : deploying app uppercase.log instance 0
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/uppercase-1470909360177/uppercase.log
...  INFO 45810 --- [nio-9393-exec-1] o.s.c.d.spi.local.LocalAppDeployer       : deploying app uppercase.uppercase instance 0
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/uppercase-1470909360453/uppercase.uppercase
...  INFO 45810 --- [nio-9393-exec-1] o.s.c.d.spi.local.LocalAppDeployer       : deploying app uppercase.file instance 0
   Logs will be in /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/uppercase-1470909361345/uppercase.file
```

all three apps in our stream are now deployed.

Tail the logs of the `uppercase.log` instance so we can see the output once a file is processed:

```
$ tail -f /var/folders/79/c5g_bfkn74d_155b5cpl39q40000gn/T/spring-cloud-dataflow-1138593025964745788/uppercase-1470909360177/uppercase.log/stdout_0.log

...

...  INFO 49416 --- [           main] o.s.c.s.b.k.KafkaMessageChannelBinder$7  : started inbound.uppercase.uppercase.uppercase
...  INFO 49416 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 0
...  INFO 49416 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 2147482647
...  INFO 49416 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 35764 (http)
...  INFO 49416 --- [           main] o.s.c.s.a.l.s.k.LogSinkKafkaApplication  : Started LogSinkKafkaApplication in 13.112 seconds (JVM running for 13.745)
```

Now let's pop a file into the monitored `/tmp/icodejava/file` directory:

```
$ echo "i\ncode\njava\n2016" > /tmp/icodejava/file/input.txt
```

and we should see the following in the application log for the `log` sink app:

```
...

...  INFO 49416 --- [ kafka-binder-1] log.sink                                 : I
...  INFO 49416 --- [ kafka-binder-1] log.sink                                 : CODE
...  INFO 49416 --- [ kafka-binder-1] log.sink                                 : JAVA
...  INFO 49416 --- [ kafka-binder-1] log.sink                                 : 2016
```

## Conclusion

Using the SCDF stream approach has the following benefits:

* The stream can conceptually support both "always on" and "batch" type scenarios. For instance, if we created
another stream but swapped out the `file` source with a `http` source, we could then support both file and request based processing,
without the processor application having to be made aware.
* Functionality can be broken into single purpose, highly composable applications



