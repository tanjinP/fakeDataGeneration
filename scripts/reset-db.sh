#!/usr/bin/env bash

# Reset database - clear all data but keep schema

echo "Resetting database (clearing all data)..."

CURRENT_USER=$(whoami)

# Truncate tables (preserves schema)
psql -U "$CURRENT_USER" fakedata <<EOF
TRUNCATE TABLE proposals CASCADE;
TRUNCATE TABLE activity_logs CASCADE;
SELECT 'activity_logs' as table_name, COUNT(*) as count FROM activity_logs
UNION ALL
SELECT 'proposals', COUNT(*) FROM proposals;
EOF

echo ""
echo "✓ Database reset complete. All tables are empty."
