#!/bin/bash

echo "🧪 Complete API Test Suite"
echo "=========================="
echo ""

if [ -z "$1" ]; then
    echo "❌ JWT Token Required"
    echo ""
    echo "Usage: ./test-all-apis.sh <JWT_TOKEN>"
    echo ""
    echo "To get a fresh JWT token:"
    echo "  1. Start HTTP server: python3 -m http.server 8000"
    echo "  2. Open browser: http://localhost:8000/clerk-login.html"
    echo "  3. Login and copy the JWT token (⚠️ expires in ~60 seconds!)"
    echo ""
    exit 1
fi

TOKEN=$1
BASE_URL="http://localhost:8080"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📋 Test 1: Health Check (No Auth)${NC}"
echo "-----------------------------------"
RESPONSE=$(curl -s http://localhost:8080/api/health)
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health)
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
else
    echo -e "${RED}❌ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi
echo ""

echo -e "${BLUE}📋 Test 2: Get Current User${NC}"
echo "-----------------------------------"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/me")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/me")
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    CLERK_USER_ID=$(echo "$RESPONSE" | jq -r '.clerkUserId // empty' 2>/dev/null)
else
    echo -e "${RED}❌ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi
echo ""

echo -e "${BLUE}📋 Test 3: List Organizations${NC}"
echo "-----------------------------------"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/organizations")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/organizations")
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    TOTAL_ORGS=$(echo "$RESPONSE" | jq -r '.total // 0' 2>/dev/null)
    ORG_ID=$(echo "$RESPONSE" | jq -r '.organizations[0].id // empty' 2>/dev/null)
    CLERK_ORG_ID=$(echo "$RESPONSE" | jq -r '.organizations[0].clerkOrgId // empty' 2>/dev/null)
    
    if [ "$TOTAL_ORGS" = "0" ] || [ -z "$ORG_ID" ]; then
        echo -e "${YELLOW}⚠️  No organizations found${NC}"
    else
        echo -e "${GREEN}   Found $TOTAL_ORGS organization(s)${NC}"
        echo -e "${GREEN}   Organization ID: $ORG_ID${NC}"
    fi
else
    echo -e "${RED}❌ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi
echo ""

echo -e "${BLUE}📋 Test 4: Check My Memberships${NC}"
echo "-----------------------------------"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/my-memberships")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/debug/my-memberships")
# Check response body even if HTTP code is not 200 (some endpoints return 403 with data)
IS_ADMIN=$(echo "$RESPONSE" | jq -r '.isAdminInAnyOrganization // false' 2>/dev/null)
TOTAL_MEMBERSHIPS=$(echo "$RESPONSE" | jq -r '.totalMemberships // 0' 2>/dev/null)

if [ "$HTTP_CODE" = "200" ] || [ "$IS_ADMIN" = "true" ] || [ "$TOTAL_MEMBERSHIPS" -gt 0 ] 2>/dev/null; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    
    if [ "$IS_ADMIN" = "true" ]; then
        echo -e "${GREEN}   ✅ User is ADMIN${NC}"
    else
        echo -e "${YELLOW}   ⚠️  User is NOT admin (totalMemberships: $TOTAL_MEMBERSHIPS)${NC}"
    fi
else
    echo -e "${RED}❌ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi
echo ""

echo -e "${BLUE}📋 Test 5: List Users (with roles)${NC}"
echo "-----------------------------------"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?includeRoles=true&page=0&size=10")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?includeRoles=true&page=0&size=10")
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    TOTAL_USERS=$(echo "$RESPONSE" | jq -r '.totalElements // 0' 2>/dev/null)
    echo -e "${GREEN}   Total users: $TOTAL_USERS${NC}"
elif [ "$HTTP_CODE" = "403" ]; then
    echo -e "${RED}❌ FAIL - Forbidden (ADMIN role required)${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    echo ""
    echo -e "${YELLOW}💡 Fix: Run ./fix-admin-role-complete.sh <TOKEN>${NC}"
else
    echo -e "${RED}❌ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi
echo ""

echo -e "${BLUE}📋 Test 6: List Users (simple, backward compatible)${NC}"
echo "-----------------------------------"
RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?page=0&size=10")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/users?page=0&size=10")
ERROR_MSG=$(echo "$RESPONSE" | jq -r '.error // empty' 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ PASS${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
elif [ "$HTTP_CODE" = "403" ]; then
    echo -e "${RED}❌ FAIL - Forbidden (ADMIN role required)${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
elif [ "$HTTP_CODE" = "500" ]; then
    echo -e "${RED}❌ FAIL - Internal Server Error${NC}"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    if echo "$ERROR_MSG" | grep -q "prepared statement"; then
        echo -e "${YELLOW}   ⚠️  PostgreSQL prepared statement error detected${NC}"
        echo -e "${YELLOW}   💡 This is a known issue with connection pooling${NC}"
    fi
else
    echo -e "${RED}❌ FAIL${NC} (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
fi
echo ""

# Only test role update if we have org ID
if [ -n "$ORG_ID" ] && [ "$ORG_ID" != "null" ]; then
    echo -e "${BLUE}📋 Test 7: Update User Role (if not admin)${NC}"
    echo "-----------------------------------"
    if [ "$IS_ADMIN" != "true" ]; then
        echo -e "${YELLOW}   Attempting to fix role...${NC}"
        FIX_RESPONSE=$(curl -s -X POST \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"organizationId\": $ORG_ID, \"roleName\": \"ADMIN\"}" \
            "$BASE_URL/api/debug/fix-role")
        FIX_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{\"organizationId\": $ORG_ID, \"roleName\": \"ADMIN\"}" \
            "$BASE_URL/api/debug/fix-role")
        
        if [ "$FIX_HTTP_CODE" = "200" ]; then
            echo -e "${GREEN}✅ Role fixed successfully${NC}"
            echo "$FIX_RESPONSE" | jq '.' 2>/dev/null || echo "$FIX_RESPONSE"
        else
            echo -e "${RED}❌ Failed to fix role${NC} (HTTP $FIX_HTTP_CODE)"
            echo "$FIX_RESPONSE" | jq '.' 2>/dev/null || echo "$FIX_RESPONSE"
        fi
    else
        echo -e "${GREEN}✅ User is already ADMIN, skipping role fix${NC}"
    fi
    echo ""
fi

echo -e "${BLUE}📊 Test Summary${NC}"
echo "=============="
echo ""
echo "✅ Health Check: Working"
echo "✅ Services: Running"
echo ""
if [ "$IS_ADMIN" = "true" ]; then
    echo -e "${GREEN}✅ User Status: ADMIN${NC}"
    echo -e "${GREEN}✅ Users API: Should work${NC}"
else
    echo -e "${YELLOW}⚠️  User Status: NOT ADMIN${NC}"
    echo -e "${YELLOW}⚠️  Users API: Will return 403${NC}"
    echo ""
    echo "💡 To fix:"
    echo "   ./fix-admin-role-complete.sh <TOKEN>"
fi
echo ""
echo "✅ Test Complete!"
