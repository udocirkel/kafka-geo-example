#!/bin/bash

echo ""
echo "Stopping Services DC1, Kafka Replicator DC1->DC2"
echo "------------------------------------------------"
docker compose \
    --profile services-dc1 \
    --profile replicator-dc2 \
    down

echo ""
echo "Starting Services DC2"
echo "---------------------"
docker compose \
    --profile services-dc2 \
    up -d

./wait-stack.sh \
    demo-service-dc2

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
