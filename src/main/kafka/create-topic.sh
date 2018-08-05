#!/usr/bin/env bash

KAFKA_HOME=~/git/kafka

eval $(docker-machine env default)

export ZOOKEEPER=`docker-machine ip \`docker-machine active\``:2181

$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list

$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --topic taxis_demand --partitions 8 --replication-factor 1

$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --topic taxis_supply --partitions 8 --replication-factor 1

$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --topic taxis_trips --partitions 8 --replication-factor 1

