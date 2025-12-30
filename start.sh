#!/bin/bash

echo ""
echo "Starting Kafka container"
echo "------------------------"
docker compose up -d

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
