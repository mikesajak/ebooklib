#!/bin/bash

# Load environment variables from .env file if it exists
if [ -f .env ]; then
  while IFS='=' read -r key value; do
    # Skip comments and empty lines
    if [[ ! "$key" =~ ^# ]] && [[ -n "$key" ]]; then
      export "$key=$value"
    fi
  done < .env
fi

# Default command is 'start' if not provided as first argument
COMMAND="${1:-start}"

# Run podman-compose with the specified or default command and -d flag
if [ "$COMMAND" = "up" ]; then
  podman-compose --verbose -f podman-compose-dev.yaml up -d
else
  podman-compose --verbose -f podman-compose-dev.yaml "$COMMAND"
fi
