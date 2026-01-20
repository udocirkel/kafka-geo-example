#!/bin/bash

echo ""
echo "Stopping all"
echo "------------"
docker compose \
    --profile site-dc1 \
    --profile site-dc2 \
    down

echo ""
echo "Starting Zookeeper DC1, Zookeeper DC2"
echo "-------------------------------------"
docker compose \
    --profile coordinator-dc1 \
    --profile coordinator-dc2 \
    up -d

./wait-stack.sh \
    zookeeper-dc1 \
    zookeeper-dc2

echo ""
echo "Starting Kafka DC1, Kafka DC2"
echo "-----------------------------"
docker compose \
    --profile kafka-dc1 \
    --profile kafka-dc2 \
    up -d

./wait-stack.sh \
    kafka1-dc1 \
    kafka1-dc2

echo ""
echo "Starting Replication DC1 -> DC2, Kafka UI, Client Apps DC1"
echo "----------------------------------------------------------"
docker compose \
    --profile repl-dc1-to-dc2 \
    --profile kafka-ui \
    --profile apps-dc1 \
    up -d

./wait-stack.sh \
    mirrormaker-dc2 \
    demo-service-dc1

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
