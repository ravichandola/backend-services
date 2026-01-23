# 🔄 Understanding System Flows

A beginner-friendly guide to understand how the payment system works.

## 🎯 What This Guide Covers

This guide explains:
- How the system starts up
- How requests flow through the system
- How payments are created and verified
- Basic concepts without technical jargon

## 🚀 System Startup (Simple Version)

When you run `docker-compose up`, here's what happens:

1. **PostgreSQL starts** - Database is ready
2. **pgAdmin starts** - Database management tool
3. **Spring Boot app starts** - Your payment service
4. **Everything connects** - Services talk to each other

**That's it!** The system is ready to accept requests.

> 📖 **Want details?** See [Detailed Startup Flow](../guides/FLOW_DETAILED.md#startup)

## 📥 How a Request Works (Simple Version)

When you send a request to create a payment order:

```
You (Client)
  ↓
  Send: POST /api/payments/create-order
  ↓
API Gateway
  ↓
  Validates your token
  ↓
Backend Service
  ↓
  Creates payment order
  ↓
  Saves to database
  ↓
  Returns order details
  ↓
You (Client)
  Receives: Order ID and details
```

**In plain English:**
1. You ask to create a payment
2. System checks you're allowed
3. System creates the payment
4. System saves it
5. System gives you the payment details

> 📖 **Want details?** See [Detailed Request Flow](../guides/FLOW_DETAILED.md#request-flow)

## 💳 Payment Creation Flow

### Step-by-Step (Simple)

1. **You send request**
   - Amount: ₹500
   - Endpoint: `/api/payments/create-order`

2. **System validates**
   - Checks amount is valid
   - Checks you're authenticated

3. **System creates order**
   - Talks to Razorpay (payment provider)
   - Gets order ID

4. **System saves**
   - Saves order to database
   - Records all details

5. **You get response**
   - Order ID
   - Amount
   - Status

**Example Request:**
```json
POST /api/payments/create-order
{
  "amount": 500
}
```

**Example Response:**
```json
{
  "orderId": "order_abc123",
  "amount": 500,
  "currency": "INR",
  "status": "created"
}
```

> 📖 **Want details?** See [Detailed Payment Flow](../guides/FLOW_DETAILED.md#payment-flow)

## ✅ Payment Verification Flow

After a user pays, you need to verify the payment:

1. **You send verification request**
   - Order ID
   - Payment ID
   - Signature

2. **System verifies**
   - Checks signature is valid
   - Confirms payment is real

3. **System updates**
   - Marks order as paid
   - Saves transaction

4. **You get confirmation**
   - Payment verified
   - Order status updated

> 📖 **Want details?** See [Detailed Verification Flow](../guides/FLOW_DETAILED.md#verification-flow)

## 🔐 Authentication Flow

Every request needs authentication:

1. **You get token** from Clerk (authentication service)
2. **You send request** with token in header
3. **Gateway checks token** - Is it valid?
4. **If valid** - Request goes through
5. **If invalid** - Request is rejected

**Simple rule:** No valid token = No access

> 📖 **Want details?** See [Authentication Guide](../guides/API_TESTING_CONFIG.md#getting-jwt-token)

## 🗄️ Database Flow

When data is saved:

1. **Service layer** decides what to save
2. **Repository layer** talks to database
3. **Database** stores the data
4. **Repository** confirms it's saved

**Example:**
- Save payment order → Goes to `payment_order` table
- Save user → Goes to `users` table
- Save organization → Goes to `organizations` table

> 📖 **Want details?** See [Database Operations](../guides/FLOW_DETAILED.md#database-operations)

## 🎓 Key Concepts

### What is a Flow?
A flow is the path a request takes from start to finish.

### Why Understand Flows?
- Helps you debug issues
- Helps you add new features
- Helps you understand the system

### What's Next?
1. ✅ You understand basic flows
2. 📖 Read [Detailed Flow Documentation](../guides/FLOW_DETAILED.md)
3. 🧪 Try [Testing APIs](./TESTING_APIS.md)
4. 🏗️ Explore [Architecture](../architecture/ARCHITECTURE.md)

---

**Quick Links:**
- [Getting Started](./GETTING_STARTED.md) - Setup guide
- [Testing APIs](./TESTING_APIS.md) - Test the system
- [Detailed Flows](../guides/FLOW_DETAILED.md) - Technical details
