FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:23.1-jdk-21

USER root

COPY pom.xml /app/gis/
COPY .mvn /app/gis/.mvn
COPY mvnw /app/gis/
WORKDIR /app/gis
RUN ./mvnw -q verify clean --fail-never

COPY . /app/gis
RUN ./mvnw -q clean package
RUN native-image -cp target/gis-*.jar "org.nqm.Gis" --initialize-at-run-time=io.netty.handler.ssl.BouncyCastleAlpnSslUtils

RUN mv org.nqm.gis gis
RUN chmod +x gis
