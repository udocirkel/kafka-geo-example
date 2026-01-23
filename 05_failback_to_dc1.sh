#!/bin/bash

echo ""
echo "Stopping Services DC2, Kafka Replicator DC2->DC1"
echo "------------------------------------------------"
docker compose \
    --profile services-dc2 \
    --profile replicator-dc1 \
    down

echo ""
echo "Starting Services DC1"
echo "---------------------"
docker compose \
    --profile services-dc1 \
    up -d

./wait-stack.sh \
    demo-service-dc1

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
