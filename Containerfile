FROM docker.io/maven:3.9.7-eclipse-temurin-21-alpine AS build
COPY pom.xml /app/gis/
WORKDIR /app/gis
RUN mvn -q verify clean --fail-never
COPY . /app/gis
RUN apk add --no-cache git
RUN mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean package

FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1-jdk-21
USER root
WORKDIR /app/gis
COPY --from=build /app/gis/target/gis-*.jar target/
COPY --from=build /app/gis/target/lib target/lib
RUN native-image -march=compatibility -cp target/gis-*.jar "org.nqm.Gis" --no-fallback
RUN mv org.nqm.gis gis
RUN chmod +x gis
RUN ./gis --version
