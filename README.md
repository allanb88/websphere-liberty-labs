# WebSphere Liberty Labs

This repository contains hands-on exercises for deploying your first Java application to WebSphere Liberty—and taking it further with Docker, Kubernetes, and OpenShift.

The labs are deliberately progressive. Start with a running Liberty container, build a small Java web application, deploy it as a WAR file, configure Liberty, package the result as an application image, and then move toward Kubernetes and OpenShift.

## Prerequisites

- Docker Desktop or Docker Engine
- Java 11 or newer
- Apache Maven
- curl or a web browser
- Git

Verify the tools:

~~~bash
docker --version
java -version
mvn --version
~~~

## Lab sequence

| Lab | Topic | Main outcome |
| --- | --- | --- |
| 01 | Liberty and containers | Start, inspect, stop, and restart Liberty |
| 02 | First Java application | Build and deploy a Java WAR application |
| 03 | Liberty configuration | Understand server.xml, features, ports, and context roots |
| 04 | Application image | Build a reusable Liberty application image |
| 05 | Troubleshooting | Diagnose common deployment and runtime failures |
| 06 | Kubernetes and OpenShift | Translate the application into container orchestration concepts |

## Recommended workflow

For every lab:

1. Read the objective before running commands.
2. Make one change at a time.
3. Check the container logs after each deployment.
4. Write down what changed and why.
5. Complete the questions before moving to the next lab.

## Official references

- [Setting up Liberty](https://www.ibm.com/docs/en/was-liberty/base?topic=setting-up-liberty)
- [Deploying applications in Liberty](https://www.ibm.com/docs/en/was-liberty/core?topic=deploying-applications-in-liberty)
- [Creating Liberty application images](https://www.ibm.com/docs/en/was-liberty/base?topic=container-creating-application-images)
- [Liberty container images](https://www.ibm.com/docs/en/was-liberty/core?topic=images-liberty-container)

