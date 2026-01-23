#!/bin/bash

echo "🧪 Testing All Endpoints"
echo "========================"
echo ""

echo "1️⃣  Health Check (No Auth Required):"
echo "-----------------------------------"
curl -s http://localhost:8080/api/health | jq . 2>/dev/null || curl -s http://localhost:8080/api/health
echo -e "\n"

echo "2️⃣  /api/me Without Token (Should Return 401):"
echo "----------------------------------------------"
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" http://localhost:8080/api/me)
http_code=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
body=$(echo "$response" | grep -v "HTTP_STATUS")
echo "Response: $body"
echo "HTTP Status: $http_code"
if [ "$http_code" = "401" ]; then
    echo "✅ Correct: 401 Unauthorized (endpoint exists, needs token)"
elif [ "$http_code" = "404" ]; then
    echo "❌ Error: 404 Not Found (endpoint doesn't exist)"
else
    echo "⚠️  Unexpected status: $http_code"
fi
echo -e "\n"

echo "3️⃣  Services Status:"
echo "-------------------"
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
echo -e "\n"

echo "4️⃣  Backend Logs (Last 5 lines):"
echo "--------------------------------"
docker logs backend-service --tail 5 2>&1 | grep -v "^$"
echo -e "\n"

echo "✅ Basic Tests Complete!"
echo ""
echo "📝 Next Steps:"
echo "   1. Get JWT token from: http://localhost:8000/clerk-login.html"
echo "   2. Test with token:"
echo "      curl -H \"Authorization: Bearer YOUR_TOKEN\" http://localhost:8080/api/me"
echo ""
