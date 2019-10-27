# build stage
FROM gradle:4.2.1-jdk8 as build-stage
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build

# production stage
FROM openjdk:8-jre-alpine as production-stage

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

COPY --from=build-stage /home/gradle/src/build/libs/hiking-api.jar /app/hiking-api.jar
RUN chmod +x /app/hiking-api.jar

USER $APPLICATION_USER
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "hiking-api.jar"]