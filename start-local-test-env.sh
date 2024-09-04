#!/usr/bin/env bash
set -e 

docker compose up -d 

CONTAINER_NAME=$(docker ps --format "{{.Names}}" | head -n 1)

TEMP_DIR=$(mktemp -d)
git clone https://github.com/arangodb/example-datasets.git "$TEMP_DIR/example-datasets"
cd "$TEMP_DIR/example-datasets" && git checkout 5936aa1129d8f402e1ccb6f9b22bc3773cf3ee49 && cd -
docker cp "$TEMP_DIR/example-datasets/Graphs/IMDB" "$CONTAINER_NAME:/tmp/example-datasets"
docker exec  "$CONTAINER_NAME" sh -c "cd /tmp/example-datasets && arangorestore --server.endpoint tcp://localhost:8529 --server.username $ARANGODB_USERNAME --server.password $ARANGODB_PASSWORD --server.database IMDB --create-database --include-system-collections"
rm -rf "$TEMP_DIR"
