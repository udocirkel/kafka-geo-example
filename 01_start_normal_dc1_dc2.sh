#!/bin/bash

echo ""
echo "Stopping all"
echo "------------"
docker compose \
    --profile clients \
    --profile operating \
    --profile site-dc1 \
    --profile site-dc2 \
    down

echo ""
echo "Starting Kafka Coordinator DC1, Kafka Coordinator DC2"
echo "-----------------------------------------------------"
docker compose \
    --profile coordinator-dc1 \
    --profile coordinator-dc2 \
    up -d

./wait-stack.sh \
    zookeeper-dc1 \
    zookeeper-dc2

echo ""
echo "Starting Kafka Broker DC1, Kafka Broker DC2"
echo "-------------------------------------------"
docker compose \
    --profile broker-dc1 \
    --profile broker-dc2 \
    up -d

./wait-stack.sh \
    kafka1-dc1 \
    kafka1-dc2

echo ""
echo "Starting Kafka UI, Kafka Replicator DC1->DC2, Services DC1"
echo "----------------------------------------------------------"
docker compose \
    --profile operating \
    --profile replicator-dc2 \
    --profile services-dc1 \
    up -d

./wait-stack.sh \
    mirrormaker-dc2 \
    demo-service-dc1

echo ""
echo "Starting Clients"
echo "----------------"
docker compose \
    --profile clients \
    up -d

./wait-stack.sh \
    demo-client

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
