#!/usr/bin/env bash

mvn clean package && \
docker build -t taxis-now/supply-producer:0.1 -f Dockerfile/supply . && \
docker build -t taxis-now/demand-producer:0.1 -f Dockerfile/demand . # && \
#docker run -e KAFKA='192.168.99.100:9092' taxis-now/supply-producer:0.1


