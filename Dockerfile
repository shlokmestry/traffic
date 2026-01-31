# Dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

# Allow overriding the jar path at build time:
# docker build --build-arg JAR_FILE=traffic-service/target/traffic-service-0.0.1-SNAPSHOT.jar -t traffic-service:local .
ARG JAR_FILE=traffic-service/target/*.jar

# Copy the built jar into the image
COPY ${JAR_FILE} app.jar

# Run as non-root for better security
RUN useradd -r -u 10001 appuser
USER 10001

EXPOSE 8081

ENTRYPOINT ["java","-jar","/app/app.jar"]
