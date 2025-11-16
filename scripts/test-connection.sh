#!/usr/bin/env bash

# Test database connection
# This script verifies that PostgreSQL is accessible

echo "Testing PostgreSQL connection..."
echo ""

# Try local PostgreSQL first
if command -v psql >/dev/null 2>&1; then
    CURRENT_USER=$(whoami)
    if psql -U "$CURRENT_USER" fakedata -c "SELECT version();" 2>/dev/null; then
        echo ""
        echo "✓ Successfully connected to local PostgreSQL!"
        echo "  Database: fakedata"
        echo "  User: $CURRENT_USER"
        exit 0
    fi
fi

# Try with postgres user (Docker/Podman)
if PGPASSWORD=postgres psql -U postgres -h localhost fakedata -c "SELECT version();" 2>/dev/null; then
    echo ""
    echo "✓ Successfully connected to containerized PostgreSQL!"
    echo "  Database: fakedata"
    echo "  User: postgres"
    exit 0
fi

# Connection failed
echo "✗ Failed to connect to PostgreSQL"
echo ""
echo "Please run: ./scripts/setup-db.sh"
exit 1
