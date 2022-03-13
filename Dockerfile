FROM ubuntu:20.04
RUN apt update && apt install -y curl unzip zip gcc zlib1g-dev

RUN curl -s "https://get.sdkman.io" | bash;
SHELL ["/bin/bash", "-c"]
RUN ["/bin/bash", "-c", "source /root/.sdkman/bin/sdkman-init.sh"]

# RUN sdk version
# RUN sdk install java 22.0.0.2.r17-grl
# gu install native-image
COPY . /home/app/gis
WORKDIR /home/app/gis

# RUN ./mvnw clean package
# native-image -cp target/gis-1.0.0.jar "org.nqm.GisCommand"
