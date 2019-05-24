# Multi-Stage Build

##########
# Stage 1
##########
FROM openjdk:8-jdk-alpine as build

#RUN apk add --no-cache bash && rm -rf /var/cache/apk/*

WORKDIR /workspace/app

#Files needed for maven build in container
COPY mvnw ./Sidora-Solr/mvnw
COPY .mvn ./Sidora-Solr/.mvn
COPY mvnw.cmd ./Sidora-Solr/mvnw.cmd

# Sidora maven build files/src
COPY pom.xml .
COPY test-spring-boot.properties ./test.properties
COPY ./Sidora-Solr/pom.xml ./Sidora-Solr/pom.xml
COPY ./Sidora-Solr/src ./Sidora-Solr/src
COPY ./Sidora-Solr/SpringBoot-Config ./Sidora-Solr/SpringBoot-Config

# Copy pom from other modules so we dont break maven
COPY ./Components/pom.xml ./Components/pom.xml
COPY ./Beans/pom.xml ./Beans/pom.xml
COPY ./Tabular/pom.xml ./Tabular/pom.xml
COPY ./KAR/pom.xml ./KAR/pom.xml
COPY ./Sidora-Batch/pom.xml ./Sidora-Batch/pom.xml
COPY ./Sidora-MCI/pom.xml ./Sidora-MCI/pom.xml
COPY ./Sidora-Test-Utils/pom.xml ./Sidora-Test-Utils/pom.xml

RUN echo $PWD
RUN ls -larth
RUN cd ./Sidora-Solr && ls -larth


# Run Maven Build
RUN cd ./Sidora-Solr && ./mvnw clean install -Dkaraf.home=/app -DskipTests=true -Dcargo.maven.skip=true

RUN ls -larth Sidora-Solr

# Unpack FatJar
RUN mkdir -p ./Sidora-Solr/target/dependency && (cd ./Sidora-Solr/target/dependency; jar -xf ../*.jar)

###########
# Stage 2
###########
FROM openjdk:8-jdk-alpine

# tomcat puts stuff here
#VOLUME /tmp

ARG DEPENDENCY=/workspace/app/Sidora-Solr/target/dependency
ARG CONFIGS=/workspace/app/Sidora-Solr/target/config

# use unpacked dependencies from previous stage
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build ${CONFIGS} /app/config

#EXPOSE 8282

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dkaraf.home=app/", "-cp","app:app/lib/*","edu.si.services.solr.SiServicesSidoraSolrApplication"]



# Build using already compiled jar
#FROM openjdk:8-jdk-alpine
#VOLUME /app
#
#COPY target/*.jar app.jar
#COPY config config
#
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Dkaraf.home=target/","-jar","/app.jar"]