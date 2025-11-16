#!/usr/bin/env bash
set -e

# Database Setup Script for Fake Data Generation Project
# This script works with local PostgreSQL or containerized Postgres

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "Database Setup for Fake Data Generation"
echo "========================================"
echo ""

# Configuration
DB_NAME="fakedata"
SCHEMA_FILE="database/schema.sql"

# Check if we're in the project root
if [ ! -f "$SCHEMA_FILE" ]; then
    echo -e "${RED}Error: schema.sql not found. Please run this script from the project root.${NC}"
    exit 1
fi

# Function to check if PostgreSQL is running on a port
check_postgres_port() {
    if lsof -Pi :5432 -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to detect the PostgreSQL setup type
detect_postgres_setup() {
    # Check for local PostgreSQL
    if command -v psql >/dev/null 2>&1 && check_postgres_port; then
        echo "local"
        return
    fi

    echo "none"
}

# Detect setup
SETUP_TYPE=$(detect_postgres_setup)

echo -e "${YELLOW}Detected setup: $SETUP_TYPE${NC}"
echo ""

case $SETUP_TYPE in
    "local")
        echo "Using local PostgreSQL..."

        # Get the current user
        CURRENT_USER=$(whoami)

        # Check if database exists
        if psql -U "$CURRENT_USER" postgres -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
            echo -e "${YELLOW}Database '$DB_NAME' already exists.${NC}"
            read -p "Do you want to drop and recreate it? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                echo "Dropping database..."
                psql -U "$CURRENT_USER" postgres -c "DROP DATABASE $DB_NAME;"
                echo "Creating database..."
                psql -U "$CURRENT_USER" postgres -c "CREATE DATABASE $DB_NAME;"
            fi
        else
            echo "Creating database '$DB_NAME'..."
            psql -U "$CURRENT_USER" postgres -c "CREATE DATABASE $DB_NAME;"
        fi

        # Apply schema
        echo "Applying schema..."
        psql -U "$CURRENT_USER" "$DB_NAME" -f "$SCHEMA_FILE"

        # Verify tables
        echo ""
        echo "Verifying tables..."
        psql -U "$CURRENT_USER" "$DB_NAME" -c "\dt"

        # Test connection
        echo ""
        echo "Testing connection..."
        VERSION=$(psql -U "$CURRENT_USER" "$DB_NAME" -tAc "SELECT version();")
        echo -e "${GREEN}✓ Connected successfully!${NC}"
        echo "$VERSION"

        # Update application.conf if needed
        if grep -q 'user = "postgres"' src/main/resources/application.conf; then
            echo ""
            echo -e "${YELLOW}Updating application.conf with your username...${NC}"
            sed -i.bak "s/user = \"postgres\"/user = \"$CURRENT_USER\"/" src/main/resources/application.conf
            sed -i.bak 's/password = "postgres"/password = ""/' src/main/resources/application.conf
            rm src/main/resources/application.conf.bak
        fi

        echo ""
        echo -e "${GREEN}✓ Database setup complete!${NC}"
        echo ""
        echo "Connection details:"
        echo "  URL: jdbc:postgresql://localhost:5432/$DB_NAME"
        echo "  User: $CURRENT_USER"
        echo "  Password: (none)"
        ;;

    *)
        echo -e "${RED}Error: No PostgreSQL installation detected.${NC}"
        echo ""
        echo "Please install PostgreSQL:"
        echo "  macOS: brew install postgresql@17"
        echo "  Linux: sudo apt install postgresql-17"
        echo ""
        echo "Then start the service:"
        echo "  macOS: brew services start postgresql@17"
        echo "  Linux: sudo systemctl start postgresql"
        exit 1
        ;;
esac

echo ""
echo "========================================"
echo "Next steps:"
echo "  1. Build the project:  mise exec -- ./gradlew build"
echo "  2. Test connection:    mise exec -- ./gradlew testConnection"
echo "  3. Generate data:      mise exec -- ./gradlew run"
echo "========================================"
