# Introduction
- Features: extension for `git` to support submodule functions.
- Blazing-fast command line application written in `Java` and built with latest technologies:
    - `native image` from GraalVM to compile into machine code and run without JVM or JDK.
    - [Eclipse Vert.x](https://vertx.io/) for asynchronuos execution. (*)
- Easy to setup:
    - No extra dependencies.
    - Just download and extract the `tar` file.
    - Add the file to your shell `path` for quick access.

(*): the configs of Vertx were minimized to use as least resource as possible.

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

# Comparison
## git status
|git|gis|
|---|---|
|`git submodule foreach git status -sb --ignore-submodules`|`gis st`|
|(not include root module)|(include root module)|
|took 67ms  |   took 49ms  |
|took 141ms |   took 106ms |
|took 103ms |   took 56ms  |
|took 148ms |   took 109ms |
|took 155ms |   took 118ms |
|took 157ms |   took 116ms |
|took 126ms |   took 108ms |
|took 151ms |   took 103ms |
|took 67ms  |   took 118ms |
|took 148ms |   took 50ms  |
|took 134ms |   took 50ms  |
|average = **127ms**|average = **89.36ms**|

## git fetch

Since the duration results from `git` command are too obvious, I only run 5 times.

|git|gis|
|---|---|
|`git submodule foreach git fetch`|`gis fe`|
|(not include root module)|(include root module)|
|took 22s751ms |took 5s550ms |
|took 23s503ms |took 5s214ms |
|took 20s482ms |took 3s223ms |
|took 21s587ms |took 3s284ms |
|took 24s75ms |took 3s273ms |
|(not needed) |took 3s34ms  |
|(not needed) |took 3s596ms |
|(not needed) |took 3s100ms |
|(not needed) |took 3s157ms |
|(not needed) |took 3s147ms |
|(not needed) |took 3s594ms |
|average = **22s**479.6ms|average = **3s**679.818ms|
