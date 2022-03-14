# Introduction
- Blazing-fast command line application written in `Java`, built with `native image` technology from GraalVM.
- features: extension for `git` to support submodule functions.

# Build from source

## Native image
(require docker to build)

There are 2 GraalVM distributions: [GraalVM CE](https://www.graalvm.org/22.0/docs/getting-started/) and [Mandrel](https://developers.redhat.com/blog/2021/04/14/mandrel-a-specialized-distribution-of-graalvm-for-quarkus). Since this app is written in Java completely, Mandrel is prefered.

```shell script
cd gis
docker build -t gis .
docker create --name dkgis_ gis:latest; docker cp dkgis_:/app/gis/gis .; docker rm -f dkgis_;
```
An executable file named `gis` will be created under project directory.

## JVM

```shell script
cd gis
mvn clean package
```
The executable jar file will be created at `target/gis-<version>.jar`

# Usage

For more details, just run:
```shell script
./gis
```
