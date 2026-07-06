#!/bin/bash
# autopilot/hooks/build-gate.sh
# Verifies project compiles. Called by autopilot-loop verify step.
# Exit 0 = pass, non-zero = fail.

cd "$(git rev-parse --show-toplevel)" || exit 1

echo "=== Build Gate: Maven Compile ==="
mvn compile -q -pl distributor-server,distributor-cli 2>&1

BUILD_EXIT=$?
if [ $BUILD_EXIT -ne 0 ]; then
    echo "BUILD FAILED (exit $BUILD_EXIT)"
    exit $BUILD_EXIT
fi

echo "Build passed"
exit 0
