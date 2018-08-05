## Kafka in docker (from spotify/kafka)

docker-machine create default
OR
docker-machine start default

eval $(docker-machine env default)

## Run the docker container
```
docker run -p 2181:2181 -p 9092:9092 --env ADVERTISED_HOST=`docker-machine ip \`docker-machine active\`` --env ADVERTISED_PORT=9092 spotify/kafka
```

## Test with console producer and consumer
```
export ZOOKEEPER=`docker-machine ip \`docker-machine active\``:2181
bin/kafka-console-consumer.sh --zookeeper $ZOOKEEPER --topic test

export KAFKA=`docker-machine ip \`docker-machine active\``:9092
bin/kafka-console-producer.sh --broker-list $KAFKA --topic test
```

List containers in the docker machine
docker ps

log in to container to check inside a docker for logs
docker exec -it <container-id> bash

## Elastic Search and Kibana in docker

Check https://elk-docker.readthedocs.io/

```
docker run -p 5601:5601 -p 9200:9200 -p 5044:5044 -it --name elk_2 sebp/elk
```

### Producers
```
docker build -t taxis-now/demand-producer:0.1 -f Dockerfile/demand .
docker build -t taxis-now/supply-producer:0.1 -f Dockerfile/supply .
```

#### Local
Run from the home of the repo
```
docker run -w "$(pwd)" -v "$(pwd)/data":"$(pwd)/data" \
           -e SOURCE='local' -e KAFKA='192.168.99.100:9092' -e SPARK_MASTER='local[8]' \
           -e ES_NODE='localhost' -e ES_PORT='9200' -e ES_CLUSTER_NAME='elasticsearch' \
        taxis-now/demand-producer:0.1

docker run -w "$(pwd)" -v "$(pwd)/data":"$(pwd)/data" \
           -e SOURCE='local' -e KAFKA='192.168.99.100:9092' -e SPARK_MASTER='local[8]' \
           -e ES_NODE='localhost' -e ES_PORT='9200' -e ES_CLUSTER_NAME='elasticsearch' \
        taxis-now/supply-producer:0.1
```

#### S3
```
docker run -e SOURCE='S3' -e KAFKA='192.168.99.100:9092' -e SPARK_MASTER='local[8]' \
           -e ES_NODE='localhost' -e ES_PORT='9200' -e ES_CLUSTER_NAME='elasticsearch' \
        taxis-now/demand-producer:0.1

docker run -e SOURCE='S3' -e KAFKA='192.168.99.100:9092' -e SPARK_MASTER='local[8]' \
           -e ES_NODE='localhost' -e ES_PORT='9200' -e ES_CLUSTER_NAME='elasticsearch' \
        taxis-now/supply-producer:0.1
```


## Spark
Run spark processors in local
```
java -cp taxis-now-1.0-SNAPSHOT.jar '-DSOURCE=local' '-DKAFKA=192.168.99.100:9092' \
 '-DSPARK_MASTER=local[8]' '-DES_NODE=localhost' '-DES_PORT=9200' '-DES_CLUSTER_NAME=elasticsearch' \
 org.anish.taxisnow.processing.SupplyDemandProcessor
```

```
java -cp taxis-now-1.0-SNAPSHOT.jar '-DSOURCE=local' '-DKAFKA=192.168.99.100:9092' \
 '-DSPARK_MASTER=local[8]' '-DES_NODE=localhost' '-DES_PORT=9200' '-DES_CLUSTER_NAME=elasticsearch' \
 org.anish.taxisnow.processing.TrafficCongestionProcessor
```


