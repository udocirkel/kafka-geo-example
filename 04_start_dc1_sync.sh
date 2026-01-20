#!/bin/bash

echo ""
echo "Starting Zookeeper DC1"
echo "----------------------"
docker compose \
    --profile coordinator-dc1 \
    up -d

./wait-stack.sh \
    zookeeper-dc1

echo ""
echo "Starting Kafka DC1"
echo "------------------"
docker compose \
    --profile kafka-dc1 \
    up -d

./wait-stack.sh \
    kafka1-dc1

echo ""
echo "Starting Replication DC2 -> DC1"
echo "-------------------------------"
docker compose \
    --profile repl-dc2-to-dc1 \
    up -d

./wait-stack.sh \
    mirrormaker-dc1

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
