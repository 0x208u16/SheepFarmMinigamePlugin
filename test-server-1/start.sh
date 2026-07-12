#!/bin/bash

cd "$(dirname "$0")"

exec java \
  -Xms512M \
  -Xmx1G \
  -XX:+UseSerialGC \
  -jar server.jar \
  --nogui