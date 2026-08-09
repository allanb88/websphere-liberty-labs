# Lab 03: Configure Liberty with server.xml

## Objective

Move from implicit deployment through dropins to explicit Liberty configuration.

## Concepts

- server.xml
- Liberty features
- HTTP endpoints
- Application context roots
- Explicit application configuration

## Step 1: Add server.xml

Create this structure in your application project:

~~~text
hello-app/
├── Dockerfile
├── server.xml
└── target/hello.war
~~~

Create server.xml:

~~~xml
<server description="Hello Liberty Server">

    <featureManager>
        <feature>servlet-6.0</feature>
    </featureManager>

    <httpEndpoint
        id="defaultHttpEndpoint"
        host="*"
        httpPort="9080"
        httpsPort="9443" />

    <webApplication
        id="hello"
        name="hello"
        contextRoot="/hello"
        location="hello.war" />

</server>
~~~

## Step 2: Update the Dockerfile

Use the apps directory for explicitly configured applications:

~~~dockerfile
FROM icr.io/appcafe/websphere-liberty:latest

COPY --chown=1001:0 server.xml /config/
COPY --chown=1001:0 target/hello.war /config/apps/
~~~

Build and run:

~~~bash
docker build -t hello-liberty-configured:1.0 .

docker run -d \
  --name hello-liberty-configured \
  -p 9080:9080 \
  hello-liberty-configured:1.0
~~~

Test it:

~~~bash
curl http://localhost:9080/hello/hello
~~~

## Exercises

1. Change contextRoot to /greeting.
2. Change httpPort to 9081 and update the Docker port mapping.
3. Remove the servlet-6.0 feature and observe the startup behavior.
4. Add a second application with a different context root.
5. Compare the logs from dropins deployment and explicit deployment.

## Questions

1. What is the purpose of featureManager?
2. Why must the application location agree with the file location in the image?
3. What does host="*" mean in a container?
4. Why is explicit configuration generally preferable for production?

## Cleanup

~~~bash
docker rm -f hello-liberty-configured
~~~

