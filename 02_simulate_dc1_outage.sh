#!/bin/bash

echo ""
echo "Stopping DC1"
echo "------------"
docker compose \
    --profile site-dc1 \
    down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a
