#!/bin/bash

# Default command is 'start' if not provided as first argument
COMMAND="${1:-start}"

# Run podman-compose with the specified or default command and -d flag
if [ "$COMMAND" = "up" ]; then
  podman-compose --verbose -f podman-compose-dev.yaml up -d
else
  podman-compose --verbose -f podman-compose-dev.yaml "$COMMAND"
fi
