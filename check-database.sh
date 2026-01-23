#!/bin/bash

# Database Connection Checker Script
# This script helps you check which database your services are currently using

echo "🔍 Checking Database Connections..."
echo "=================================="
echo ""

# Check payment-service
echo "📦 Payment Service:"
echo "-------------------"
PROFILE=$(docker-compose exec -T payment-service printenv SPRING_PROFILES_ACTIVE 2>/dev/null || echo "dev")
DB_URL=$(docker-compose logs payment-service 2>/dev/null | grep "Database JDBC URL" | tail -1 | sed 's/.*\[\(.*\)\].*/\1/' || echo "Not found")

if [[ "$DB_URL" == *"supabase"* ]] || [[ "$DB_URL" == *"aws-1-ap-south-1"* ]]; then
    echo "  ✅ Using: Supabase Cloud Database"
    echo "  📍 URL: $DB_URL"
elif [[ "$DB_URL" == *"postgres:5432/appdb"* ]] || [[ "$DB_URL" == *"localhost:5433/appdb"* ]]; then
    echo "  🐳 Using: Local Docker Database"
    echo "  📍 URL: $DB_URL"
else
    echo "  ⚠️  Database URL: $DB_URL"
fi
echo "  🔧 Profile: $PROFILE"
echo ""

# Check backend-service
echo "📦 Backend Service:"
echo "-------------------"
PROFILE_BACKEND=$(docker-compose exec -T backend-service printenv SPRING_PROFILES_ACTIVE 2>/dev/null || echo "dev")
DB_URL_BACKEND=$(docker-compose logs backend-service 2>/dev/null | grep "Database:" | tail -1 | sed 's/.*Database: \(.*\) (.*/\1/' || echo "Not found")

if [[ "$DB_URL_BACKEND" == *"supabase"* ]] || [[ "$DB_URL_BACKEND" == *"aws-1-ap-south-1"* ]]; then
    echo "  ✅ Using: Supabase Cloud Database"
    echo "  📍 URL: $DB_URL_BACKEND"
elif [[ "$DB_URL_BACKEND" == *"postgres:5432/appdb"* ]] || [[ "$DB_URL_BACKEND" == *"localhost:5433/appdb"* ]]; then
    echo "  🐳 Using: Local Docker Database"
    echo "  📍 URL: $DB_URL_BACKEND"
else
    echo "  ⚠️  Database URL: $DB_URL_BACKEND"
fi
echo "  🔧 Profile: $PROFILE_BACKEND"
echo ""

echo "=================================="
echo ""
echo "💡 To switch to Supabase:"
echo "   1. Update docker-compose.yml to set SPRING_PROFILES_ACTIVE=prod"
echo "   2. Restart services: docker-compose restart payment-service backend-service"
echo ""
echo "💡 To connect to Supabase in pgAdmin:"
echo "   See: documentation/setup/SUPABASE_SETUP.md"
