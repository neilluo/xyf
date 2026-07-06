#!/bin/bash
# autopilot/hooks/post-edit.sh
# Runs after code changes. Exit 0 = pass, non-zero = fail.

cd "$(git rev-parse --show-toplevel)" || exit 1

echo "=== Post-Edit: Compile Check ==="
mvn compile -q -pl distributor-server,distributor-cli 2>&1
COMPILE_EXIT=$?

if [ $COMPILE_EXIT -ne 0 ]; then
    echo "COMPILE FAILED"
    exit $COMPILE_EXIT
fi

# Check for common anti-patterns
echo "=== Anti-pattern scan ==="
ISSUES=0

# Check for mock residue in non-mock files
if grep -rn "pending_exchange_\|fake-token\|mock-video-id" distributor-server/src/main/java/ --include="*.java" | grep -v MockUploader | grep -v "/test/"; then
    echo "WARNING: Mock patterns found in production code"
    ISSUES=$((ISSUES + 1))
fi

# Check for readAllBytes on video paths
if grep -rn "readAllBytes\|new byte\[.*fileSize\]" distributor-server/src/main/java/ --include="*.java"; then
    echo "WARNING: Potential full-file-load-to-memory detected"
    ISSUES=$((ISSUES + 1))
fi

if [ $ISSUES -gt 0 ]; then
    echo "Post-edit: $ISSUES warnings found (non-blocking)"
fi

echo "Post-edit passed"
exit 0
