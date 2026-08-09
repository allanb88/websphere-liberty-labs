# Lab 01: Liberty and Containers

## Objective

Learn how to start WebSphere Liberty in Docker and inspect its basic lifecycle.

## Concepts

- Docker image versus Docker container
- Container ports
- Foreground and detached execution
- Container logs
- Starting and stopping a container

## Step 1: Pull the Liberty image

~~~bash
docker pull icr.io/appcafe/websphere-liberty:latest
~~~

The latest tag is convenient for learning, but it can move to a newer Liberty release over time. Check the version included in the local image:

~~~bash
docker image inspect \
  icr.io/appcafe/websphere-liberty:latest \
  --format '{{index .Config.Labels "com.ibm.websphere.liberty.version"}}'
~~~

You can also check the runtime from a temporary container:

~~~bash
docker run --rm \
  --entrypoint /opt/ibm/wlp/bin/productInfo \
  icr.io/appcafe/websphere-liberty:latest version
~~~

## Step 2: Start Liberty

~~~bash
docker run -d \
  --name liberty-lab \
  -p 9080:9080 \
  -p 9443:9443 \
  icr.io/appcafe/websphere-liberty:latest
~~~

The -p options map ports inside the container to ports on your computer.

Check the container:

~~~bash
docker ps
~~~

View the logs:

~~~bash
docker logs -f liberty-lab
~~~

Press Ctrl+C to stop following the logs. This does not stop the container.

## Step 3: Practice the lifecycle

~~~bash
docker stop liberty-lab
docker ps -a
docker start liberty-lab
docker ps
~~~

Inspect the container configuration:

~~~bash
docker inspect liberty-lab
~~~

Open [http://localhost:9080](http://localhost:9080) in a browser. A default Liberty server might not display an application yet; that is expected.

## Exercises

1. Run the container in the foreground instead of detached mode.
2. Change the host port to 9081 while keeping the container port at 9080.
3. Stop the container and determine whether the image still exists.
4. Remove the container and create it again from the same image.

## Questions

1. What is the difference between an image and a container?
2. Which port belongs to your computer, and which port belongs to Liberty?
3. Where do you look when Liberty fails during startup?
4. What information is lost when you remove a container?

## Cleanup

~~~bash
docker rm -f liberty-lab
~~~

