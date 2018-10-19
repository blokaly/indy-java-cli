# Sample java client of HyperLedger Indy

## Prerequisite and Installs

### Tested environment

```console

$ cat /etc/os-release 
  NAME="Ubuntu"
  VERSION="16.04.5 LTS (Xenial Xerus)"
  ID=ubuntu
  ID_LIKE=debian
  PRETTY_NAME="Ubuntu 16.04.5 LTS"
  VERSION_ID="16.04"
  HOME_URL="http://www.ubuntu.com/"
  SUPPORT_URL="http://help.ubuntu.com/"
  BUG_REPORT_URL="http://bugs.launchpad.net/ubuntu/"
  VERSION_CODENAME=xenial
  UBUNTU_CODENAME=xenial

$ uname -ra
 Linux myhost 4.4.0-137-generic #163-Ubuntu SMP Mon Sep 24 13:14:43 UTC 2018 x86_64 x86_64 x86_64 GNU/Linux
 
$ docker --version
  Docker version 18.06.1-ce, build e68fc7a
  
$ java -version 
  java version "1.8.0_172"
  Java(TM) SE Runtime Environment (build 1.8.0_172-b11)
  Java HotSpot(TM) 64-Bit Server VM (build 25.172-b11, mixed mode)   
  
$ gradle -version
  executing gradlew instead of gradle
  
  ------------------------------------------------------------
  Gradle 4.9
  ------------------------------------------------------------
  
  Build time:   2018-07-16 08:14:03 UTC
  Revision:     efcf8c1cf533b03c70f394f270f46a174c738efc
  
  Kotlin DSL:   0.18.4
  Kotlin:       1.2.41
  Groovy:       2.4.12
  Ant:          Apache Ant(TM) version 1.9.11 compiled on March 23 2018
  JVM:          1.8.0_172 (Oracle Corporation 25.172-b11)
  OS:           Linux 4.4.0-137-generic amd64
  
```

### Start local docker test nodes

```console
$ git clone git@github.com:hyperledger/indy-sdk.git

$ cd indy-sdk

$ docker network create --subnet 10.0.0.0/8 indy_pool_network

$ docker build --build-arg pool_ip=10.0.0.2 -f ci/indy-pool.dockerfile -t indy_pool .

$ docker run -d --ip="10.0.0.2" --net=indy_pool_network indy_pool
```

If successful, then `$ docker ps -a` should show the running container.

### Install Indy Library

```console
$ sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 68DB5E88

$ sudo add-apt-repository "deb https://repo.sovrin.org/sdk/deb xenial stable"

$ sudo apt-get update

$ sudo apt-get install -y libindy
```

If successful, then the `/usr/lib/libindy.so` file should be installed locally.

## Run this sample application

Under this `indy-java-cli` folder, run

```console
$ export RUST_LOG=trace
$ export LD_LIBRARY_PATH=/usr/lib/
$ gradle run
```

## References

https://github.com/hyperledger/indy-sdk/blob/master/README.md

https://github.com/hyperledger/indy-sdk/tree/master/wrappers/java

https://github.com/hyperledger/indy-sdk/tree/master/doc/how-tos