# Lab 04: Build a Reusable Liberty Application Image

## Objective

Create a versioned Docker image that contains both the Liberty runtime configuration and your application.

## Why this matters

Manually copying files into a running container is useful for debugging, but it is not repeatable. An application image allows you to create identical containers in development, testing, and production.

## Step 1: Use a versioned application image tag

Build the application image with a meaningful tag:

~~~bash
docker build -t hello-liberty:1.0.0 .
~~~

Run it:

~~~bash
docker run -d \
  --name hello-liberty-v1 \
  -p 9080:9080 \
  hello-liberty:1.0.0
~~~

## Step 2: Inspect the image

~~~bash
docker image ls hello-liberty
docker image inspect hello-liberty:1.0.0
~~~

Check which files were copied into the image:

~~~bash
docker exec hello-liberty-v1 find /config -maxdepth 2 -type f
~~~

## Step 3: Create version 1.1

Change the Java response, rebuild the WAR, and build a new image:

~~~bash
mvn clean package
docker build -t hello-liberty:1.1.0 .
~~~

Run both versions on different host ports:

~~~bash
docker run -d \
  --name hello-liberty-v1-1 \
  -p 9081:9080 \
  hello-liberty:1.1.0
~~~

Test both versions:

~~~bash
curl http://localhost:9080/hello/hello
curl http://localhost:9081/hello/hello
~~~

## Exercises

1. Run two application versions at the same time.
2. Compare their responses.
3. Stop version 1.0 and leave version 1.1 running.
4. Add a README section documenting the image version and application version.
5. Try to run the image on another machine with Docker installed.

## Questions

1. Why should application images be versioned?
2. What changes when you rebuild the image?
3. What is the difference between an application version and a Liberty runtime version?
4. Why is a repeatable image useful in a deployment pipeline?

## Cleanup

~~~bash
docker rm -f hello-liberty-v1 hello-liberty-v1-1
~~~

