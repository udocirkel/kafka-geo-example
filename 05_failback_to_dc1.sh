#!/bin/bash

echo ""
echo "Stopping client app DC1, Kafka replication DC2->DC1"
echo "---------------------------------------------------"
docker compose \
    --profile apps-dc2 \
    --profile repl-dc2-to-dc1 \
    down

echo ""
echo "Starting Kafka replication DC1->DC2, client app DC1"
echo "---------------------------------------------------"
docker compose \
    --profile repl-dc1-to-dc2 \
    --profile apps-dc1 \
    up -d

./wait-stack.sh \
    mirrormaker-dc2 \
    demo-service-dc1

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
