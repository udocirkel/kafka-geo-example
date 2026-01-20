#!/bin/bash

echo ""
echo "Stopping all"
echo "------------"
docker compose \
    --profile site-dc1 \
    --profile site-dc2 \
    down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
