FROM gradle:7-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM openjdk:11
#EXPOSE 8080:8080
EXPOSE 8433:8433
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-docker-sample.jar
ENTRYPOINT ["java","-jar","/app/ktor-docker-sample.jar"]


#FROM openjdk:11.0.16
#EXPOSE 8080:8080
#RUN mkdir /app
#COPY ./build/libs/*-all.jar /app/ktor-docker-sample.jar
#ENTRYPOINT ["java","-jar","/app/ktor-docker-sample.jar"]