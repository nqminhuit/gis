FROM quay.io/quarkus/ubi-quarkus-mandrel:22.0.0.2-Final-java17

USER root

COPY pom.xml /app/gis/
COPY .mvn /app/gis/.mvn
COPY mvnw /app/gis/
WORKDIR /app/gis
RUN ./mvnw verify clean --fail-never

COPY . /app/gis
RUN ./mvnw clean package
RUN native-image -cp target/gis-1.0.0.jar "org.nqm.GisCommand"
RUN mv org.nqm.giscommand gis
RUN chmod +x gis
