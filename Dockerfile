# build stage
FROM eclipse-temurin:8-jdk AS build-stage
COPY . /app/src
WORKDIR /app/src
RUN chmod +x gradlew && ./gradlew clean build

# production stage
FROM eclipse-temurin:8-jre AS production-stage

ENV APPLICATION_USER=ktor
RUN adduser --disabled-password --gecos '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

COPY --from=build-stage /app/src/build/libs/hiking-api.jar /app/hiking-api.jar

USER $APPLICATION_USER
WORKDIR /app

CMD ["java", "-server", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "hiking-api.jar"]
