#!/bin/bash

echo "🔧 Role Fix Script"
echo "=================="
echo ""

if [ -z "$1" ]; then
    echo "❌ JWT Token Required"
    echo ""
    echo "Usage: ./fix-role-now.sh <JWT_TOKEN>"
    echo ""
    echo "To get a fresh JWT token:"
    echo "  1. Start HTTP server: python3 -m http.server 8000"
    echo "  2. Open browser: http://localhost:8000/clerk-login.html"
    echo "  3. Login and copy the JWT token (tokens expire in ~60 seconds!)"
    echo ""
    exit 1
fi

TOKEN=$1
BASE_URL="http://localhost:8080"

echo "📋 Step 1: Checking current memberships..."
echo "------------------------------------------"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/my-memberships")
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

# Extract organization ID (try to get from response or use default)
ORG_ID=$(echo "$RESPONSE" | jq -r '.memberships[0].organizationId // 1' 2>/dev/null)
IS_ADMIN=$(echo "$RESPONSE" | jq -r '.isAdminInAnyOrganization // false' 2>/dev/null)

echo "📊 Current Status:"
echo "  - Is Admin: $IS_ADMIN"
echo "  - Organization ID: $ORG_ID"
echo ""

if [ "$IS_ADMIN" = "true" ]; then
    echo "✅ User is already ADMIN! Testing users endpoint..."
    echo ""
    curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?page=0&size=10" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?page=0&size=10"
else
    echo "⚠️  User is NOT admin. Fixing role..."
    echo ""
    echo "📋 Step 2: Creating/Updating membership with ADMIN role..."
    echo "----------------------------------------------------------"
    FIX_RESPONSE=$(curl -s -X POST \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"organizationId\": $ORG_ID, \"roleName\": \"ADMIN\"}" \
        "$BASE_URL/api/debug/fix-role")
    
    echo "$FIX_RESPONSE" | jq '.' 2>/dev/null || echo "$FIX_RESPONSE"
    echo ""
    
    SUCCESS=$(echo "$FIX_RESPONSE" | jq -r '.success // false' 2>/dev/null)
    
    if [ "$SUCCESS" = "true" ] || echo "$FIX_RESPONSE" | grep -q "success"; then
        echo "✅ Role fixed successfully!"
        echo ""
        echo "📋 Step 3: Verifying fix..."
        echo "----------------------------"
        sleep 1
        VERIFY_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/my-memberships")
        IS_ADMIN_NOW=$(echo "$VERIFY_RESPONSE" | jq -r '.isAdminInAnyOrganization // false' 2>/dev/null)
        
        if [ "$IS_ADMIN_NOW" = "true" ]; then
            echo "✅ Verified: User is now ADMIN!"
            echo ""
            echo "📋 Step 4: Testing users endpoint..."
            echo "-----------------------------------"
            curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?page=0&size=10" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?page=0&size=10"
        else
            echo "⚠️  Verification failed. Please check the response above."
        fi
    else
        echo "❌ Failed to fix role. Error:"
        echo "$FIX_RESPONSE"
    fi
fi

echo ""
echo "✅ Done!"
