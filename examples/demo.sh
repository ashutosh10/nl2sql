#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(dirname "$SCRIPT_DIR")
cd "$ROOT_DIR"

# Run the CLI with a sample NL query
mvn -q -pl :examples exec:java -Dexec.args="What is the deployment frequency of terraform deployments for repo X in July 2025?"
