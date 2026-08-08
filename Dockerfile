FROM maven:3.9.12-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM amazoncorretto:21-alpine
WORKDIR /app
RUN addgroup -S vagaradar && adduser -S vagaradar -G vagaradar && apk add --no-cache wget
COPY --from=build --chown=vagaradar:vagaradar /app/target/vagaradar-0.0.1-SNAPSHOT.jar app.jar
USER vagaradar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
