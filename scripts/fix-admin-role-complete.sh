#!/bin/bash

echo "🔧 Complete Admin Role Fix Script"
echo "=================================="
echo ""

if [ -z "$1" ]; then
    echo "❌ JWT Token Required"
    echo ""
    echo "Usage: ./fix-admin-role-complete.sh <JWT_TOKEN>"
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

echo "📋 Step 1: Decoding JWT to extract organization info..."
echo "--------------------------------------------------------"

# Extract payload from JWT (second part between dots)
PAYLOAD=$(echo $TOKEN | cut -d. -f2)

# Add padding if needed for base64 decode
case $((${#PAYLOAD} % 4)) in
    2) PAYLOAD="${PAYLOAD}==" ;;
    3) PAYLOAD="${PAYLOAD}=" ;;
esac

# Decode base64 (may fail on some systems, so we'll try)
DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null || echo "")

if [ -z "$DECODED" ]; then
    echo "⚠️  Could not decode JWT automatically"
    echo "Please extract organization info manually from: https://jwt.io"
    echo ""
    read -p "Enter Clerk Org ID (from o.id in JWT): " CLERK_ORG_ID
    read -p "Enter Organization Name (optional): " ORG_NAME
else
    # Try to extract using jq if available, otherwise use grep
    if command -v jq &> /dev/null; then
        CLERK_ORG_ID=$(echo "$DECODED" | jq -r '.o.id // empty' 2>/dev/null)
        ORG_NAME=$(echo "$DECODED" | jq -r '.o.slg // "Payment Organization"' 2>/dev/null)
    else
        # Fallback: use grep to find org ID
        CLERK_ORG_ID=$(echo "$DECODED" | grep -o '"id":"org_[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
        ORG_NAME="Payment Organization"
    fi
fi

if [ -z "$CLERK_ORG_ID" ]; then
    echo "❌ Could not extract Clerk Org ID from JWT"
    echo ""
    echo "Please decode your JWT at https://jwt.io and find the 'o.id' field"
    echo "Then run:"
    echo "  ./fix-admin-role-complete.sh <TOKEN> <CLERK_ORG_ID>"
    exit 1
fi

echo "✅ Found Clerk Org ID: $CLERK_ORG_ID"
echo "   Organization Name: ${ORG_NAME:-Payment Organization}"
echo ""

echo "📋 Step 2: Checking existing organizations..."
echo "--------------------------------------------"
ORG_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/organizations")
TOTAL_ORGS=$(echo "$ORG_RESPONSE" | grep -o '"total":[0-9]*' | cut -d: -f2 || echo "0")

if [ "$TOTAL_ORGS" = "0" ] || [ -z "$TOTAL_ORGS" ]; then
    echo "⚠️  No organizations found. Creating organization..."
    echo ""
    
    CREATE_ORG_RESPONSE=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "{
          \"clerkOrgId\": \"$CLERK_ORG_ID\",
          \"name\": \"${ORG_NAME:-Payment Organization}\",
          \"slug\": \"payment-org\"
        }" \
        "$BASE_URL/api/test/organizations")
    
    echo "$CREATE_ORG_RESPONSE" | jq '.' 2>/dev/null || echo "$CREATE_ORG_RESPONSE"
    echo ""
    
    # Extract organization ID from response
    ORG_ID=$(echo "$CREATE_ORG_RESPONSE" | jq -r '.organization.id // empty' 2>/dev/null)
    
    if [ -z "$ORG_ID" ]; then
        # Try to get from organizations list again
        sleep 1
        ORG_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/organizations")
        ORG_ID=$(echo "$ORG_RESPONSE" | jq -r '.organizations[0].id // empty' 2>/dev/null)
    fi
else
    echo "✅ Organizations found: $TOTAL_ORGS"
    echo "$ORG_RESPONSE" | jq '.' 2>/dev/null || echo "$ORG_RESPONSE"
    echo ""
    
    # Try to find organization by clerkOrgId
    ORG_ID=$(echo "$ORG_RESPONSE" | jq -r ".organizations[] | select(.clerkOrgId == \"$CLERK_ORG_ID\") | .id" 2>/dev/null)
    
    if [ -z "$ORG_ID" ]; then
        # Use first organization
        ORG_ID=$(echo "$ORG_RESPONSE" | jq -r '.organizations[0].id // empty' 2>/dev/null)
    fi
fi

if [ -z "$ORG_ID" ]; then
    echo "❌ Could not determine organization ID"
    echo "Please check the organizations list manually:"
    echo "  curl -H \"Authorization: Bearer $TOKEN\" $BASE_URL/api/debug/organizations"
    exit 1
fi

echo "✅ Using Organization ID: $ORG_ID"
echo ""

echo "📋 Step 3: Checking current memberships..."
echo "------------------------------------------"
MEMBERSHIP_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/my-memberships")
IS_ADMIN=$(echo "$MEMBERSHIP_RESPONSE" | jq -r '.isAdminInAnyOrganization // false' 2>/dev/null)

echo "$MEMBERSHIP_RESPONSE" | jq '.' 2>/dev/null || echo "$MEMBERSHIP_RESPONSE"
echo ""

if [ "$IS_ADMIN" = "true" ]; then
    echo "✅ User is already ADMIN!"
    echo ""
    echo "📋 Step 4: Testing users endpoint..."
    echo "-----------------------------------"
    USERS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?includeRoles=true&page=0&size=10")
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?includeRoles=true&page=0&size=10")
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ Users endpoint working!"
        echo "$USERS_RESPONSE" | jq '.' 2>/dev/null || echo "$USERS_RESPONSE"
    else
        echo "❌ Users endpoint failed with HTTP $HTTP_CODE"
        echo "$USERS_RESPONSE"
    fi
else
    echo "⚠️  User is NOT admin. Fixing role..."
    echo ""
    
    echo "📋 Step 4: Creating/Updating membership with ADMIN role..."
    echo "----------------------------------------------------------"
    FIX_RESPONSE=$(curl -s -X POST \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"organizationId\": $ORG_ID, \"roleName\": \"ADMIN\"}" \
        "$BASE_URL/api/debug/fix-role")
    
    echo "$FIX_RESPONSE" | jq '.' 2>/dev/null || echo "$FIX_RESPONSE"
    echo ""
    
    SUCCESS=$(echo "$FIX_RESPONSE" | jq -r '.success // false' 2>/dev/null)
    
    if [ "$SUCCESS" = "true" ] || echo "$FIX_RESPONSE" | grep -q "success\|updated\|created"; then
        echo "✅ Role fixed successfully!"
        echo ""
        
        echo "📋 Step 5: Verifying fix..."
        echo "----------------------------"
        sleep 1
        VERIFY_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/my-memberships")
        IS_ADMIN_NOW=$(echo "$VERIFY_RESPONSE" | jq -r '.isAdminInAnyOrganization // false' 2>/dev/null)
        
        if [ "$IS_ADMIN_NOW" = "true" ]; then
            echo "✅ Verified: User is now ADMIN!"
            echo ""
            
            echo "📋 Step 6: Testing users endpoint..."
            echo "-----------------------------------"
            USERS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?includeRoles=true&page=0&size=10")
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?includeRoles=true&page=0&size=10")
            
            if [ "$HTTP_CODE" = "200" ]; then
                echo "✅ Users endpoint working!"
                echo "$USERS_RESPONSE" | jq '.' 2>/dev/null || echo "$USERS_RESPONSE"
            else
                echo "❌ Users endpoint failed with HTTP $HTTP_CODE"
                echo "$USERS_RESPONSE"
            fi
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
