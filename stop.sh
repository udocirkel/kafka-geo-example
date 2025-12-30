#!/bin/bash

echo ""
echo "Stopping Kafka container"
echo "------------------------"
docker compose down

echo ""
echo "Checking container status"
echo "-------------------------"
docker ps -a

echo ""
