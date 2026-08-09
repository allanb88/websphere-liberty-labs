# Lab 05: Troubleshoot a Liberty Deployment

## Objective

Practice diagnosing failures instead of guessing.

## Useful commands

~~~bash
docker ps -a
docker logs hello-liberty
docker inspect hello-liberty
docker port hello-liberty
docker exec -it hello-liberty sh
~~~

Inside the container, inspect the Liberty installation and configuration:

~~~bash
ls -la /opt/ibm/wlp
find /config -maxdepth 3 -type f
/opt/ibm/wlp/bin/productInfo version
~~~

## Exercise 1: Port conflict

Start one container on port 9080, then try to start another container using the same host port.

~~~bash
docker run -d --name liberty-a -p 9080:9080 hello-liberty:1.0.0
docker run -d --name liberty-b -p 9080:9080 hello-liberty:1.0.0
~~~

Determine which container failed and why.

Fix it by using another host port:

~~~bash
docker run -d --name liberty-b -p 9081:9080 hello-liberty:1.0.0
~~~

## Exercise 2: Broken application path

Temporarily change the application location in server.xml to a file that does not exist. Rebuild the image and inspect the logs.

Answer:

1. Does the Liberty server start?
2. Does the application start?
3. Which log message identifies the problem?

## Exercise 3: Broken Java application

Change the servlet so that the project fails to compile. Run:

~~~bash
mvn clean package
~~~

Determine whether Docker builds a new image when Maven does not produce a new WAR.

## Exercise 4: HTTP versus HTTPS

Test the HTTP endpoint:

~~~bash
curl -i http://localhost:9080/hello/hello
~~~

Then try HTTPS:

~~~bash
curl -k -i https://localhost:9443/hello/hello
~~~

Explain the purpose of -k in this local test.

## Final challenge

Given only these symptoms, identify the most likely cause:

- The container is running, but the browser cannot connect.
- The browser connects, but returns 404.
- Liberty starts, but the application is not listed in the logs.
- Maven fails before Docker build begins.

## Cleanup

~~~bash
docker rm -f liberty-a liberty-b
~~~

