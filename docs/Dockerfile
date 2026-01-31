# Dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar into the image
# (You may need to update the jar name/path to match your build output)
COPY build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","/app/app.jar"]
