#!/bin/bash

echo ""
echo "Stopping DC2"
echo "------------"
docker compose \
    --profile site-dc2 \
    down

echo ""
echo "Deleting Kafka data DC2"
echo "-----------------------"
rm -rf ./data/geo_demo/kafka*_dc2
rm -rf ./data/geo_demo/zookeeper_dc2

echo ""
echo "Starting Kafka Coordinator DC2"
echo "------------------------------"
docker compose \
    --profile coordinator-dc2 \
    up -d

./wait-stack.sh \
    zookeeper-dc2

echo ""
echo "Starting Kafka Broker DC2"
echo "-------------------------"
docker compose \
    --profile broker-dc2 \
    up -d

./wait-stack.sh \
    kafka1-dc2

echo ""
echo "Starting Kafka Replicator DC1->DC2"
echo "----------------------------------"
docker compose \
    --profile replicator-dc2 \
    up -d

./wait-stack.sh \
    mirrormaker-dc2

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
