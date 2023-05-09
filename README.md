
#  <p align="center">Taxi Trip Stats with Akka</p>

![Example Image](akkatax.jpg)

Project based on: https://www.kaggle.com/datasets/elemento/nyc-yellow-taxi-trip-data?select=yellow_tripdata_2016-01.csv  


# Kubernetes setup

## Step 1 Building the Docker image 

SBT plugin being used: sbt-native-package 
Ref: https://www.scala-sbt.org/sbt-native-packager/formats/universal.html

### 1.1 - Generate zip universal package
```console
 
 $ sbt  universal:packageBin
  
```

### 1.2 - Generate Docker Image
```console
 
 $ docker build -t comtudux/yellowtaxiakkastat:0.0.1 .
  
```

## Step 2 Run Kubernetes Deployments

```console
 $ kubectl apply -f .\k8s\
  
```

Restart yellowtaxi-app pod if failed to connect to cassandra.

## ----- For Non Docker/Kubernetes Local Setup -----

### WARNING

To use old local setup remove the references to the cassandra-service in order to use 
default local cassandra connection localhost:9042

```
  basic.contact-points = ["cassandra-service:9042"]
  basic.load-balancing-policy {
    local-datacenter = "datacenter1"
  }
```

### To run the Http Project:
  1) docker-compose up
  2) run com.tudux.taxi.app.TaxiApp
  3) For Swagger Documentation about routes visit: http://localhost:10001/swagger-ui/index.html  

### To Load from sample CSV
run com.tudux.taxi.actors.loader.TaxiStatAppGraphDslLoader

### Considerations 
- java version:  11.0.15.1 2022-04-22 LTS 
- Docker version: 20.10.12, build e91ed57 (Docker Desktop Windows)




