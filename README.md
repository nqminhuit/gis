# Introduction
- Features: extension for `git` to manage multiple [repositories](https://github.com/nqminhuit/gis/issues/14) or git submodules.
- Blazing-fast command line application written in `Java` and built on top of latest technologies:
    - `native image` from GraalVM to compile into machine code and run without JVM or JDK.
    - Java 21's Virtual Threads: lightweight threads that reduce the effort of writing, maintaining, and debugging high-throughput concurrent applications.
- Easy to use:
    - The app is completely bundled into a standalone executable file, no extra dependencies needed.
    - Add `.gis-modules` file if you need to manage multiple repos (not git submodules)
    - Run `gis -h` or `gis <commands> -h` for help

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

```
| Command          | Mean [ms]       | Min [ms] | Max [ms] | Relative |
|------------------+-----------------+----------+----------+----------|
| gis              | 161.1 ± 28.2    |    129.2 |    216.0 |     1.00 |
| gis branches -nn | 146.0 ± 29.7    |     81.8 |    230.3 |     1.00 |
| gis branches     | 145.5 ± 27.1    |     68.0 |    183.4 |     1.00 |
| gis co br_tst_1  | 1047.8 ± 69.1   |    981.4 |   1204.2 |     1.00 |
| gis fetch        | 7043.6 ± 1983.0 |   3848.4 |  10246.1 |     1.00 |
| gis files        | 3472.5 ± 70.4   |   3402.4 |   3663.2 |     1.00 |
| gis status       | 152.5 ± 23.9    |    113.6 |    196.9 |     1.00 |
```


Performance is run with:
- number of submodules: `100`
- avg number of branch per submodules: `52.050`
- avg number of files per submodules: `1002.000`
- avg number of commit per submodules: `155.480`


CPU info:
```
CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE    MAXMHZ   MINMHZ      MHZ
  0    0      0    0 0:0:0:0          yes 4600.0000 800.0000 870.5860
  1    0      0    0 0:0:0:0          yes 4600.0000 800.0000 869.3230
  2    0      0    1 4:4:1:0          yes 4600.0000 800.0000 800.2880
  3    0      0    1 4:4:1:0          yes 4600.0000 800.0000 800.4020
  4    0      0    2 8:8:2:0          yes 4600.0000 800.0000 800.1200
  5    0      0    2 8:8:2:0          yes 4600.0000 800.0000 800.0000
  6    0      0    3 12:12:3:0        yes 4600.0000 800.0000 800.0340
  7    0      0    3 12:12:3:0        yes 4600.0000 800.0000 800.0000
  8    0      0    4 16:16:4:0        yes 4600.0000 800.0000 805.9600
  9    0      0    4 16:16:4:0        yes 4600.0000 800.0000 800.7110
 10    0      0    5 20:20:5:0        yes 4600.0000 800.0000 800.0000
 11    0      0    5 20:20:5:0        yes 4600.0000 800.0000 802.9520
 12    0      0    6 28:28:7:0        yes 3300.0000 800.0000 864.4740
 13    0      0    7 29:29:7:0        yes 3300.0000 800.0000 800.0000
 14    0      0    8 30:30:7:0        yes 3300.0000 800.0000 799.8320
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


gis version: `2.0.0-dev`
