# Spring Cloud Data Flow - Stream (using OpenShift deploy server)

This project is a demo from my I Code Java 2016 talk entitled "Stream is the new batch"

_This is the SCDF Stream centric implementation in contrast to the [SCDF Task example](https://github.com/donovanmuller/icodejava-spring-bootch-scdf-task)._

This project is a trivial Spring Cloud Data Flow implementation that monitors a directory using the
out-of-the-box [`file`](https://github.com/spring-cloud/spring-cloud-stream-app-starters/tree/master/file/spring-cloud-starter-stream-source-file)
source application. Once a file is received, each line in the file is sent to the custom `uppercase` processor app
as a separate message where it is uppercase'd. This uppercase'd message is then sent to the OOTB [`log`](https://github.com/spring-cloud/spring-cloud-stream-app-starters/tree/master/log/spring-cloud-starter-stream-sink-log) 
sink application where the message value is logged.

See the SCDF [reference documentation](http://docs.spring.io/spring-cloud-dataflow/docs/1.0.0.RELEASE/reference/htmlsingle) for more information

## OpenShift

Instead of using the SCDF Local server, we will now use the OpenShift deployer server.
This allows us to deploy our stream into an OpenShift environment as Docker containers.

There are a few variants of OpenShift but we will be using the community edition: OpenShift Origin.
For more information around OpenShift Origin, please visit [www.openshift.org](https://www.openshift.org)

### Fabric8 Vagrant Box

The easiest way to get a local OpenShift origin instance up and running is by using the Fabric8 Vagrant box.

For more information on installing, running and configuring this instance, please see my blog post [here](http://blog.switchbit.io/spring-cloud-deployer-openshift/#localopenshiftinstance).

### Nexus configuration

In order for the OpenShift deployer server to resolve our apps we will use the provided Nexus
instance. 

Please configure Nexus as per my blog post [here](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile/#nexus).

### Create `icodejava` project

Now that you have a running OpenShift Origin environment and have configured Nexus, you'll need to create a new project
to host our stream apps. To do this, SSH into the Vagrant VM and execute the following:

```
$ vagrant ssh
[vagrant@vagrant vagrant]$ oc login
Server [https://localhost:8443]:  
The server uses a certificate signed by an unknown authority.  
You can bypass the certificate check, but any data you send to the server could be intercepted by others.  
Use insecure connections? (y/n): y

Authentication required for https://localhost:8443 (openshift)  
Username: admin  
Password:  
Login successful.

You have access to the following projects and can switch between them with 'oc project <projectname>':

  * default (current)
  * openshift
  * openshift-infra
  * user-secrets-source-admin

Using project "default".  
Welcome! See 'oc help' to get started.

[vagrant@vagrant vagrant]$ oc new-project icodejava --display-name="I Code Java" --description="I Code Java 2016 Demo"
Now using project "icodejava" on server "https://localhost:8443".

...
```

### Allow hostPath volume plugin

For demonstration purposes we want to use a local persistent volume in the Vagrant VM.
OpenShift provides a `hostPath` volume plugin for this, however, this plugin is [not allowed](https://docs.openshift.org/latest/admin_guide/manage_scc.html#use-the-hostpath-volume-plugin) 
by default.

To switch on support for it, execute the following:

```
[vagrant@vagrant ~]$ oc export scc anyuid | sed 's/allowHostDirVolumePlugin: false/allowHostDirVolumePlugin: true/g' | oc replace -f -
securitycontextconstraints "anyuid" replaced
```

## Kafka

We will use the Kafka binder implementations of the apps, therefore we need a running Kafka broker available inside our `icodejava` project in OpenShift.

Follow the steps [here](http://blog.switchbit.io/spring-cloud-deployer-openshift/#kafka) in my blog post.

## Spring Cloud Data Flow OpenShift Server

To stand up a SCDF OpenShift deployer server, follow the steps in my blog post [here](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile/#bootinguptheserver).
, replacing the `spring.cloud.deployer.kubernetes.namespace` value accordingly.

You should see something like the following:

```
$ java -Dopenshift.url=https://172.28.128.4:8443 \
    -Dkubernetes.master=https://172.28.128.4:8443 \
    -Dkubernetes.trust.certificates=true \
    -Dkubernetes.auth.basic.username=admin \
    -Dkubernetes.auth.basic.password=admin \
    -jar target/spring-cloud-dataflow-server-openshift-1.0.0.BUILD-SNAPSHOT.jar \
    --spring.cloud.deployer.kubernetes.namespace=icodejava \
    --maven.resolvePom=true \
    --maven.remote-repositories.nexus.url=http://nexus.vagrant.f8/content/groups/public \
    --maven.remote-repositories.nexus.auth.username=deployment \
    --maven.remote-repositories.nexus.auth.password=password \
    --maven.requestTimeout=1800000
    
...
    
...  INFO 56806 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 9393 (http)
...  INFO 56806 --- [           main] o.s.c.d.s.k.OpenShiftDataFlowServer      : Started OpenShiftDataFlowServer in 9.963 seconds (JVM running for 10.462)
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
dataflow:>app import --uri http://bit.ly/stream-applications-kafka-maven
Successfully registered applications: [source.tcp, sink.jdbc, source.http, sink.rabbit, source.rabbit, source.ftp, sink.gpfdist, processor.transform, source.sftp, processor.filter, source.file, sink.cassandra, processor.groovy-filter, sink.router, source.trigger, sink.hdfs-dataset, processor.splitter, source.load-generator, processor.tcp-client, sink.file, source.time, source.gemfire, source.twitterstream, sink.tcp, source.jdbc, sink.field-value-counter, sink.redis-pubsub, sink.hdfs, processor.bridge, processor.pmml, processor.httpclient, sink.ftp, source.s3, sink.log, sink.gemfire, sink.aggregate-counter, sink.throughput, source.triggertask, sink.s3, source.gemfire-cq, source.jms, source.tcp-client, processor.scriptable-transform, sink.counter, sink.websocket, source.mongodb, source.mail, processor.groovy-transform, source.syslog]
```

## Deploying and registering uppercase processor

Before we can register our `uppercase` processor, we must deploy the Maven artifact to the Nexus repository.
This is to enable OpenShift to create a Docker image through a `BuildConfig`. For more information around how this works, 
please see [this section](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile/#customdockerfilebuilds) of my blog post.

Deploy the built app into Nexus using the following command:

```
$ mvn -s .settings.xml deploy
```

Now we will register our custom `uppercase` processor:

```
dataflow:>app register --name uppercase --uri "maven://i.code.java:uppercase-processor:1.0-SNAPSHOT" --type processor
Successfully registered application 'processor:uppercase'
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

_TODO_

## Conclusion

Using the SCDF stream approach has the following benefits:

* The stream can conceptually support both "always on" and "batch" type scenarios. For instance, if we created
another stream but swapped out the `file` source with a `http` source, we could then support both file and request based processing,
without the processor application having to be made aware.
* Functionality can be broken into single purpose, highly composable applications



