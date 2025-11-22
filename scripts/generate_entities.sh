#!/bin/bash

# Script to generate SeaORM entities from database
# Usage: ./scripts/generate_entities.sh

# Load database config from config.toml
DB_HOST=$(grep 'host=' config.toml | cut -d'"' -f2)
DB_PORT=$(grep 'port=' config.toml | cut -d'=' -f2 | tr -d ' ')
DB_USER=$(grep 'user=' config.toml | cut -d'"' -f2)
DB_PASS=$(grep 'password=' config.toml | cut -d'"' -f2)
DB_NAME=$(grep 'db_name=' config.toml | cut -d'"' -f2)

DATABASE_URL="mysql://${DB_USER}:${DB_PASS}@${DB_HOST}:${DB_PORT}/${DB_NAME}"

echo "Generating entities from database: ${DB_NAME}"
echo "Database URL: mysql://${DB_USER}:****@${DB_HOST}:${DB_PORT}/${DB_NAME}"

# Generate entities
sea-orm-cli generate entity \
  -u "${DATABASE_URL}" \
  -o src/entities \
  --with-serde both \
  --expanded-format

if [ $? -eq 0 ]; then
    echo "✓ Entities generated successfully!"
else
    echo "✗ Failed to generate entities"
    echo "Note: If you get schema discovery errors, you may need to:"
    echo "  1. Update sea-orm-cli: cargo install sea-orm-cli --force"
    echo "  2. Check database connection"
    echo "  3. Generate entities manually for problematic tables"
fi
