ARG BUILD_IMAGE=maven:3.9.11-eclipse-temurin-25
ARG RUNTIME_IMAGE=ghcr.io/sta-cloud-dev/deverlopment:java_25

FROM ${BUILD_IMAGE} AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B test package

FROM ${RUNTIME_IMAGE}
WORKDIR /opt/stashield
COPY --from=build --chown=10001:10001 /workspace/target/STAShield.jar /opt/stashield/STAShield.jar
COPY --chown=10001:10001 config/stashield.example.toml /opt/stashield/config/stashield.toml
USER 10001:10001
EXPOSE 25565
ENTRYPOINT ["java", "-jar", "/opt/stashield/STAShield.jar"]
CMD ["--config", "/opt/stashield/config/stashield.toml"]
