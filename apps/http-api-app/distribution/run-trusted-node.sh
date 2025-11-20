#!/bin/bash
# Bisq Trusted Node Launcher
# Reads configuration from trusted-node.properties and starts the node
#
# Usage:
#   ./run-trusted-node.sh                    # Uses trusted-node.properties
#   PROPERTIES_FILE=alice.properties ./run-trusted-node.sh  # Uses custom file

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Use custom properties file if specified, otherwise use default
if [ -z "$PROPERTIES_FILE" ]; then
    PROPERTIES_FILE="$SCRIPT_DIR/trusted-node.properties"
else
    # If relative path, make it relative to script directory
    if [[ "$PROPERTIES_FILE" != /* ]]; then
        PROPERTIES_FILE="$SCRIPT_DIR/$PROPERTIES_FILE"
    fi
fi

# Check if properties file exists
if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "❌ Error: Properties file not found: $PROPERTIES_FILE"
    echo "Please create it from the template or check the README.md"
    exit 1
fi

echo "Using configuration: $PROPERTIES_FILE"
echo ""

# Function to read property from file
get_property() {
    local key=$1
    local value
    value=$(grep "^${key}=" "$PROPERTIES_FILE" | head -n 1 | cut -d'=' -f2- | tr -d '\r')
    # Note: We don't check $? here because missing optional properties (like port) are valid
    echo "$value"
}

# Read configuration
APP_NAME=$(get_property "appName")
PASSWORD=$(get_property "password")
PORT=$(get_property "port")
DEV_MODE=$(get_property "devMode")
TRANSPORT_TYPES=$(get_property "transportTypes")

# Validate required fields
if [ -z "$APP_NAME" ]; then
    echo "❌ Error: appName is not set in trusted-node.properties"
    exit 1
fi

if [ -z "$PASSWORD" ] || [ "$PASSWORD" = "CHANGE_ME_TO_A_STRONG_PASSWORD" ]; then
    echo "❌ Error: Please set a strong password in trusted-node.properties"
    exit 1
fi

# Build JAVA_OPTS
JAVA_OPTS="-Dapplication.appName=$APP_NAME"
JAVA_OPTS="$JAVA_OPTS -Dapplication.websocket.password=$PASSWORD"
JAVA_OPTS="$JAVA_OPTS -Dapplication.devMode=${DEV_MODE:-false}"

if [ -n "$PORT" ]; then
    JAVA_OPTS="$JAVA_OPTS -Dapplication.websocket.server.port=$PORT"
fi

# Parse transport types
if [ -n "$TRANSPORT_TYPES" ]; then
    IFS=',' read -ra TYPES <<< "$TRANSPORT_TYPES"
    INDEX=0
    for TYPE in "${TYPES[@]}"; do
        TYPE=$(echo "$TYPE" | xargs) # trim whitespace
        JAVA_OPTS="$JAVA_OPTS -Dapplication.network.supportedTransportTypes.$INDEX=$TYPE"
        INDEX=$((INDEX + 1))
    done
fi

# Determine data directory based on OS
if [[ "$OSTYPE" == "darwin"* ]]; then
    DATA_DIR="~/Library/Application Support/$APP_NAME"
else
    DATA_DIR="~/.local/share/$APP_NAME"
fi

# Display configuration
echo "=========================================="
echo "Starting Bisq Trusted Node"
echo "=========================================="
echo "Instance Name: $APP_NAME"
echo "Port: ${PORT:-8090}"
echo "Transport Types: ${TRANSPORT_TYPES:-TOR,CLEAR}"
echo "Data Directory: $DATA_DIR"
echo "=========================================="
echo ""

# Export JAVA_OPTS and run
export JAVA_OPTS
exec "$SCRIPT_DIR/bin/http-api-app"

