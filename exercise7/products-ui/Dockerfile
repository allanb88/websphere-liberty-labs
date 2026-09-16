# Stage 1: build the WAR with Maven. Nothing is installed on the host —
# this container has its own JDK and Maven and downloads dependencies itself.
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package

# Stage 2: bake the WAR and server.xml into a Liberty runtime image.
FROM icr.io/appcafe/websphere-liberty:latest
COPY --chown=1001:0 server.xml /config/
COPY --chown=1001:0 --from=build /build/target/products-ui.war /config/apps/
