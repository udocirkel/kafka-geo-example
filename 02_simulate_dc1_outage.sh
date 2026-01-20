#!/bin/bash

echo ""
echo "Stopping DC1"
echo "------------"
docker compose \
    --profile kafka-dc1 \
    --profile coordinator-dc1 \
    --profile apps-dc1 \
    down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
