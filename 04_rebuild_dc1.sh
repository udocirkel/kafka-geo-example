#!/bin/bash

echo ""
echo "Stopping DC1"
echo "------------"
docker compose \
    --profile site-dc1 \
    down

echo ""
echo "Deleting Kafka data DC1"
echo "-----------------------"
rm -rf ./data/geo_demo/kafka*_dc1
rm -rf ./data/geo_demo/zookeeper_dc1

echo ""
echo "Starting Kafka Coordinator DC1"
echo "------------------------------"
docker compose \
    --profile coordinator-dc1 \
    up -d

./wait-stack.sh \
    zookeeper-dc1

echo ""
echo "Starting Kafka Broker DC1"
echo "-------------------------"
docker compose \
    --profile broker-dc1 \
    up -d

./wait-stack.sh \
    kafka1-dc1

echo ""
echo "Starting Kafka Replicator DC2->DC1"
echo "----------------------------------"
docker compose \
    --profile replicator-dc1 \
    up -d

./wait-stack.sh \
    mirrormaker-dc1

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
