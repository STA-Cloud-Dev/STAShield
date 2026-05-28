ARG BUILD_IMAGE=maven:3.9.11-eclipse-temurin-25
ARG RUNTIME_IMAGE=ghcr.io/sta-cloud-dev/deverlopment:java_25

FROM ${BUILD_IMAGE} AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B test package

FROM ${RUNTIME_IMAGE}
USER root
WORKDIR /opt/stashield
COPY --from=build /workspace/target/STAShield.jar /opt/stashield/STAShield.jar
COPY config/stashield.example.toml /opt/stashield/config/stashield.toml
COPY docker-entrypoint.sh /opt/stashield/docker-entrypoint.sh
WORKDIR /home/container
EXPOSE 25565
ENTRYPOINT ["/bin/sh", "/opt/stashield/docker-entrypoint.sh"]
