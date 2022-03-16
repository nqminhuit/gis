# Introduction
- Features: extension for `git` to support submodule functions.
- Blazing-fast command line application written in `Java` and built with latest technologies:
    - `native image` from GraalVM to compile into machine code and run without JVM or JDK.
    - [Eclipse Vert.x](https://vertx.io/) for asynchronuos execution. (*)
- Easy to setup:
    - The app is completely bundled into an executable file.
    - No extra dependencies needed.
    - Just download and extract the `tar` file from `release` page.
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
After the steps above, an executable file named `gis` will be created under project directory.

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
| 67ms  |    49ms  |
| 141ms |    106ms |
| 103ms |    56ms  |
| 148ms |    109ms |
| 155ms |    118ms |
| 157ms |    116ms |
| 126ms |    108ms |
| 151ms |    103ms |
| 67ms  |    118ms |
| 148ms |    50ms  |
| 134ms |    50ms  |
|average = **127ms**|average = **89.36ms**|

## git fetch

Since the duration results from `git` command are too obvious, I only run 5 times.

|git|gis|
|---|---|
|`git submodule foreach git fetch`|`gis fe`|
|(not include root module)|(include root module)|
| 22s751ms | 5s550ms |
| 23s503ms | 5s214ms |
| 20s482ms | 3s223ms |
| 21s587ms | 3s284ms |
| 24s75ms | 3s273ms |
| | 3s34ms  |
| | 3s596ms |
| | 3s100ms |
| | 3s157ms |
| | 3s147ms |
| | 3s594ms |
|average = **22s**479.6ms|average = **3s**679.818ms|
