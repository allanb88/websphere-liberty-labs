# Lab 02: Deploy Your First Java Application

## Objective

Build a small Java servlet application with Maven, package it as a WAR file, and deploy it to Liberty.

## Concepts

- Java source code
- Maven packaging
- WAR files
- Servlets
- Liberty application deployment

## Step 1: Create the project

Create this directory structure:

~~~text
hello-app/
├── Dockerfile
├── pom.xml
├── server.xml
└── src/main/java/com/example/HelloServlet.java
~~~

Create pom.xml:

~~~xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>hello-app</artifactId>
    <version>1.0</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.release>11</maven.compiler.release>
    </properties>

    <dependencies>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>hello</finalName>
        <plugins>
            <plugin>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
~~~

Create src/main/java/com/example/HelloServlet.java:

~~~java
package com.example;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/plain");
        response.getWriter().println("Hello from WebSphere Liberty!");
    }
}
~~~

## Step 2: Build the WAR

From the hello-app directory:

~~~bash
mvn clean package
~~~

Confirm that Maven created:

~~~text
target/hello.war
~~~

## Step 3: Create server.xml

Create server.xml in the hello-app directory:

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

</server>
~~~

The application uses the Jakarta Servlet 6.0 API, so Liberty must enable the matching servlet feature. The wildcard host allows Docker to forward requests from your computer into the container.

## Step 4: Create a Liberty application image

Create a Dockerfile in the hello-app directory:

~~~dockerfile
FROM icr.io/appcafe/websphere-liberty:latest

COPY --chown=1001:0 server.xml /config/
COPY --chown=1001:0 target/hello.war /config/dropins/
~~~

Build the image:

~~~bash
docker build -t hello-liberty:1.0 .
~~~

Run the application:

~~~bash
docker run -d \
  --name hello-liberty \
  -p 9080:9080 \
hello-liberty:1.0
~~~

## Step 5: Test the application

~~~bash
curl -i http://localhost:9080/hello/hello
~~~

Expected response body:

~~~text
Hello from WebSphere Liberty!
~~~

The first hello is the WAR context root. The second hello is the servlet URL mapping.

Check the deployment logs:

~~~bash
docker logs hello-liberty
~~~

Look for a message showing that the hello application started successfully.

## Exercises

1. Change the response text and rebuild the WAR.
2. Add the current date and time to the response.
3. Add a second servlet mapped to /status.
4. Rename the WAR to greeting.war and observe the new context root.
5. Try to access /hello and /hello/hello. Explain the difference.

## Questions

1. What does Maven produce?
2. What does Liberty do with the WAR file?
3. How is the application context root selected?
4. Why is the Servlet API dependency marked provided?

## Cleanup

~~~bash
docker rm -f hello-liberty
~~~
