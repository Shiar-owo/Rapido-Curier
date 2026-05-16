FROM eclipse-temurin:17-jre-alpine
    RUN apk add --no-cache git
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]