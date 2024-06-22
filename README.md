# Introduction
- Features: extension for `git` to manage multiple [repositories](https://github.com/nqminhuit/gis/issues/14) or git submodules.
- Blazing-fast command line application written in `Java` and built on top of latest technologies:
    - `native image` from GraalVM to compile into machine code and run without JVM or JDK.
    - [Eclipse Vert.x](https://vertx.io/) for asynchronuos execution. (*)
- Easy to use:
    - The app is completely bundled into a standalone executable file, no extra dependencies needed.
    - Add `.gis-modules` file if you need to manage multiple repos (not git submodules)
    - Run `gis -h` or `gis <commands> -h` for help

(*): the configs of Vertx were minimized to use as least resource as possible.

# Build from source

## Native image
(require docker/podman to build)

There are 2 GraalVM distributions: [GraalVM CE](https://www.graalvm.org/22.0/docs/getting-started/) and [Mandrel](https://developers.redhat.com/blog/2021/04/14/mandrel-a-specialized-distribution-of-graalvm-for-quarkus). Since this app is written in Java completely, Mandrel is prefered.

```shell script
cd gis
podman build -t gis . || return 1; podman create --name dkgis_ gis:latest; podman cp dkgis_:/app/gis/gis .; podman rm -f dkgis_
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
./gis --help
```

# Comparison

notes:
- `git submodule` commands do not take the root module into account, however `gis` does.
- the data was generated on the same repository, same machine.

## status

![status: git vs gis](assets/git_vs_gis.svg)

command for generating the above numbers:
```shell script
for i in {1..1000}; do { time git submodule foreach git status -sb --ignore-submodules; } 2>> git_st_report done
# took 28s638ms in total

for i in {1..1000}; do { time gis st; } 2>> gis_st_report done
# took 13s654ms in total
```

## fetch

![fetch: git vs gis](assets/fetch_git_vs_gis.svg)

command for generating the above numbers:
```shell script
for i in {1..100}; do { time git submodule foreach git fetch; } 2>> git_fe_report done
# took 29m43s442ms

for i in {1..100}; do { time gis fe; } 2>> gis_fe_report done
# took 5m11s832ms
```

# Code quality

Use Sonarqube to analyze code:
```shell script
podman run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9876:9000 docker.io/sonarqube:9.9.5-community
```

Then go to `http://localhost:9876`
- login (admin/admin), then change your password
- go to `http://localhost:9876/projects` and click "Add a project"
- choose "Manually"
- input "Project key" and "Display name" e.g., "gis" then click "Set Up"
- "Generate a token": enter a name for this token then click "Generate"
- you will get something like this: 302481a5dee289283af983ac713174e2f2ed13da. Click "Continue"
- as shown in the 2nd step, with maven:
    ```shell script
    mvn sonar:sonar -Dsonar.projectKey=gis -Dsonar.host.url=http://localhost:9876 -Dsonar.login=302481a5dee289283af983ac713174e2f2ed13da
    ```
- after the maven command above succcess, you will have a dashboard about `gis` project

# Performance

Measure by [hyperfine](https://github.com/sharkdp/hyperfine)

```bash
| Command          | Mean [ms]       | Min [ms] | Max [ms] | Relative |
|------------------+-----------------+----------+----------+----------|
| gis              | 98.4 ± 5.9      |     89.0 |    110.9 |     1.00 |
| gis branches -nn | 73.4 ± 3.1      |     65.6 |     82.8 |     1.00 |
| gis branches     | 74.2 ± 3.0      |     66.3 |     79.2 |     1.00 |
| gis co br_tst_1  | 4344.0 ± 2407.7 |   1264.6 |   7656.9 |     1.00 |
| gis fetch        | 5095.3 ± 1722.7 |   3494.3 |   8454.7 |     1.00 |
| gis files        | 3578.6 ± 50.8   |   3535.7 |   3715.6 |     1.00 |
| gis status       | 98.6 ± 4.6      |     88.8 |    107.8 |     1.00 |
```

Performance is run with:
- number of submodules: `100`
- avg number of branch per submodules: `52.050`
- avg number of files per submodules: `1002.000`
- avg number of commit per submodules: `155.480`


CPU info:
```
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE    MAXMHZ   MINMHZ      MHZ
  0    0      0    0 0:0:0:0          yes 4600.0000 800.0000 800.0280
  1    0      0    0 0:0:0:0          yes 4600.0000 800.0000 800.1450
  2    0      0    1 4:4:1:0          yes 4600.0000 800.0000 800.3250
  3    0      0    1 4:4:1:0          yes 4600.0000 800.0000 800.4990
  4    0      0    2 8:8:2:0          yes 4600.0000 800.0000 800.2260
  5    0      0    2 8:8:2:0          yes 4600.0000 800.0000 800.0000
  6    0      0    3 12:12:3:0        yes 4600.0000 800.0000 799.5920
  7    0      0    3 12:12:3:0        yes 4600.0000 800.0000 800.0000
  8    0      0    4 16:16:4:0        yes 4600.0000 800.0000 800.0000
  9    0      0    4 16:16:4:0        yes 4600.0000 800.0000 800.0000
 10    0      0    5 20:20:5:0        yes 4600.0000 800.0000 840.4300
 11    0      0    5 20:20:5:0        yes 4600.0000 800.0000 800.0000
 12    0      0    6 28:28:7:0        yes 3300.0000 800.0000 800.0000
 13    0      0    7 29:29:7:0        yes 3300.0000 800.0000 800.0000
 14    0      0    8 30:30:7:0        yes 3300.0000 800.0000 800.0000
 15    0      0    9 31:31:7:0        yes 3300.0000 800.0000 800.0000
```

RAM info:
```
RANGE                                 SIZE  STATE REMOVABLE  BLOCK
0x0000000000000000-0x000000007fffffff   2G online       yes   0-15
0x0000000100000000-0x000000087fffffff  30G online       yes 32-271

Memory block size:       128M
Total online memory:      32G
Total offline memory:      0B
```

gis version: `1.1.4`
