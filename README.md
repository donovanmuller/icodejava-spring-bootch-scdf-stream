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

You will see the following event indicating this if you try and deploy a pod without explicitly allowing the plugin:

```
... +0000 UTC   ... +0000 UTC   9         uppercase-uppercase-1          ReplicationController                                          Warning   FailedCreate                     {replication-controller }        Error creating: pods "uppercase-uppercase-1-" is forbidden: unable to validate against any security context constraint: [spec.containers[0].securityContext.volumes[0]: Invalid value: "hostPath": hostPath volumes are not allowed to be used spec.containers[0].securityContext.volumes[0]: Invalid value: "hostPath": hostPath volumes are not allowed to be used]
```

To switch on support for it, execute the following in the Vagrant VM:

```
[vagrant@vagrant ~]$ oc export scc restricted | sed 's/allowHostDirVolumePlugin: false/allowHostDirVolumePlugin: true/g' | oc replace -f -
securitycontextconstraints "restricted" replaced
```

## Kafka

We will use the Kafka binder implementations of the apps, therefore we need a running Kafka broker available inside our `icodejava` project in OpenShift.

Follow the steps [here](http://blog.switchbit.io/spring-cloud-deployer-openshift/#kafka) in my blog post.

## Spring Cloud Data Flow OpenShift Server

To stand up a SCDF OpenShift deployer server, follow the steps in my blog post [here](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile/#bootinguptheserver), 
replacing the `spring.cloud.deployer.kubernetes.namespace` value accordingly.

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
dataflow:>app import --uri http://bit.ly/1-0-2-GA-stream-applications-kafka-maven
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
dataflow:>stream create --name uppercase --definition "file --directory=/tmp/icodejava --mode=lines | uppercase --spring.cloud.deployer.openshift.build.git.uri=https://github.com/donovanmuller/icodejava-spring-bootch-scdf-stream.git --spring.cloud.deployer.openshift.build.git.ref=openshift --spring.cloud.deployer.openshift.build.git.dockerfile=uppercase-processor/src/main/docker | log"
Created new stream 'uppercase'
```

Let's dissect this definition to understand what each app does:

* `file --directory=/tmp/icodejava --mode=lines` - Use the OOTB `file` source app to monitor the 
`/tmp/icodejava` directory _inside the Docker container_ for any files. Once a file is detected, split each line in the file into it's own
message, indicated by `--mode=lines`
* `uppercase --spring.cloud.deployer.openshift.build.git.uri=https://github.com/donovanmuller/icodejava-spring-bootch-scdf-stream.git --spring.cloud.deployer.openshift.build.git.ref=openshift --spring.cloud.deployer.openshift.build.git.dockerfile=uppercase-processor/src/main/docker` - 
Run each message through the `uppercase` processor app, which, as the name suggests, uppercase's each message value.
The extra deployment properties inform OpenShift which remote Git repository to use, which branch on the repo and also the location of the `Dockerfile`.
See [this section](http://blog.switchbit.io/scdf-openshift-deploying-maven-artifacts-with-custom-dockerfile/#customdockerfilebuilds) 
in my blog post for more information.
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

Now we can deploy our stream to OpenShift:

```
dataflow:>stream deploy --name uppercase
Deployed stream 'uppercase'
```

OpenShift will now create `BuildConfig`'s for each of the apps and once the build is completed, will deploy the stream containers.
You should see something similar to this:

![uppercase stream depoyed on OpenShift](/docs/images/uppercase-stream-openshift.png)

To test it we will "drop" a file into the `/vagrant/icodejava` directory inside our Vagrant VM, which is mounted inside the
`file` source container to `/tmp/icodejava`.
Note that the `/vagrant` directory inside the VM is shared to the location of the `Vagrantfile` on your local machine.
So to drop a file, we can just do this on our local machine:

```
$ echo "i\ncode\njava\n2016" > <location of your Vagrantfile>/input.txt
```

> Alternatively, you can drop the file inside the Vagrant VM in the `/vagrant/icodejava` directory.
However, make sure to use `printf` instead of `echo`, as the newline is not correctly interpreted.
I.e. `printf "i\ncode\njava\n2016" > /vagrant/icodejava/input.txt`

If you view the logs of the `log` sink app, you should see the expected output:

![log sink app log](/docs/images/log-sink-log.png)

Note how we didn't have to change any functionality to deploy our stream into OpenShift.
This is one of the most powerful concepts in SCDF, being able to compose and deploy a stream
onto multiple environments without leaking environment configuration into the application source.

## Conclusion

Using the SCDF stream approach has the following benefits:

* The stream can conceptually support both "always on" and "batch" type scenarios. For instance, if we created
another stream but swapped out the `file` source with a `http` source, we could then support both file and request based processing,
without the processor application having to be made aware.
* Functionality can be broken into single purpose, highly composable applications



