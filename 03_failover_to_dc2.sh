#!/bin/bash

echo ""
echo "Stopping Kafka replication DC1->DC2"
echo "-----------------------------------"
docker compose \
    --profile repl-dc1-to-dc2 \
    down

echo ""
echo "Starting client app DC2"
echo "-----------------------"
docker compose \
    --profile apps-dc2 \
    up -d

./wait-stack.sh \
    demo-service-dc2

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
