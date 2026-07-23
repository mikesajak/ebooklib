#!/bin/bash
set -e

# Configuration
if [ -n "$COMPOSE_FILE" ]; then
    # Keep pre-configured environment variable
    :
elif [ -f "docker-compose.prod.yml" ]; then
    COMPOSE_FILE="docker-compose.prod.yml"
else
    COMPOSE_FILE="docker-compose.yml"
fi
TAR_FILE="ebooklibrary-backend.tar"

# Print usage information
usage() {
    echo "EbookLibrary Deployment Management Tool"
    echo "Usage: $0 [command]"
    echo ""
    echo "Available Commands:"
    echo "  start | up      Start the containers in detached mode"
    echo "  stop | down     Stop and remove active containers"
    echo "  restart         Restart the containers"
    echo "  update          Update application (pulls from registry, or loads from tar archive if present, then restarts)"
    echo "  logs            View live container logs"
    echo "  status | ps     Check status of the containers"
    echo "  help            Show this help information"
    echo ""
}

# Determine whether to use podman-compose or docker-compose
detect_engine() {
    if command -v podman-compose &> /dev/null; then
        COMPOSE_CMD="podman-compose"
        LOAD_CMD="podman load"
        PRUNE_CMD="podman image prune -f"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
        LOAD_CMD="docker load"
        PRUNE_CMD="docker image prune -f"
    else
        if command -v docker &> /dev/null && docker compose version &> /dev/null; then
            COMPOSE_CMD="docker compose"
            LOAD_CMD="docker load"
            PRUNE_CMD="docker image prune -f"
        elif command -v podman &> /dev/null; then
            echo "Error: Found podman, but podman-compose is missing. Please install it." >&2
            exit 1
        else
            echo "Error: Neither podman-compose nor docker-compose was found on this system!" >&2
            exit 1
        fi
    fi
}

# Ensure configuration files exist
check_files() {
    if [ ! -f "$COMPOSE_FILE" ]; then
        echo "Error: $COMPOSE_FILE not found in the current directory!" >&2
        exit 1
    fi
    if [ ! -f ".env" ]; then
        echo "Warning: .env configuration file not found. Compose might fail if variables are missing." >&2
    fi
}

# Main Command Router
COMMAND="${1:-help}"
detect_engine
check_files

case "$COMMAND" in
    start|up)
        echo "=== Starting EbookLibrary Services ==="
        $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
        echo "Services started successfully."
        ;;
        
    stop|down)
        echo "=== Stopping EbookLibrary Services ==="
        $COMPOSE_CMD -f "$COMPOSE_FILE" down
        echo "Services stopped and containers removed."
        ;;
        
    restart)
        echo "=== Restarting EbookLibrary Services ==="
        $COMPOSE_CMD -f "$COMPOSE_FILE" down
        $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
        echo "Services restarted successfully."
        ;;
        
    update)
        echo "=== Updating EbookLibrary Application ==="
        if [ -f "$TAR_FILE" ]; then
            echo "Found local archive '$TAR_FILE'. Updating via image load..."
            echo "1. Stopping active services..."
            $COMPOSE_CMD -f "$COMPOSE_FILE" down
            
            echo "2. Loading new container image from $TAR_FILE..."
            $LOAD_CMD -i "$TAR_FILE"
            
            echo "3. Starting updated services..."
            $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
        else
            echo "No local archive found. Pulling latest images from registry..."
            echo "1. Pulling backend image..."
            $COMPOSE_CMD -f "$COMPOSE_FILE" pull backend
            
            echo "2. Recreating updated containers..."
            $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
        fi
        
        echo "3. Pruning unused dangling images..."
        $PRUNE_CMD
        echo "=== Update Completed Successfully! ==="
        ;;
        
    logs)
        $COMPOSE_CMD -f "$COMPOSE_FILE" logs -f
        ;;
        
    status|ps)
        $COMPOSE_CMD -f "$COMPOSE_FILE" ps
        ;;
        
    help|--help|-h)
        usage
        ;;
        
    *)
        echo "Error: Unknown command '$COMMAND'" >&2
        usage
        exit 1
        ;;
esac
