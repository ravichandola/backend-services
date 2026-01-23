# 🔧 Fixes and Improvements Log

## Branch: `feature/connect-supabase-again`

This document logs all issues encountered and fixes applied during the database profile switching implementation.

---

## 📋 Summary

**Goal:** Implement automatic database switching between Supabase (prod) and Local Docker Postgres (dev) based on `.env` configuration.

**Date:** January 23, 2026

---

## 🐛 Issues Encountered and Fixes

### Issue #1: Hardcoded Database Configuration in docker-compose.yml

**Problem:**
- `docker-compose.yml` had hardcoded datasource values (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`)
- Could not switch between dev and prod databases automatically
- Required manual changes in multiple places

**Root Cause:**
- Environment variables were hardcoded instead of reading from `.env` file
- No profile-based configuration in docker-compose

**Fix Applied:**
1. Updated `docker-compose.yml` to use environment variable substitution:
   ```yaml
   environment:
     SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
     SUPABASE_DATASOURCE_URL: ${SUPABASE_DATASOURCE_URL:-jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres}
     SUPABASE_DATASOURCE_USERNAME: ${SUPABASE_DATASOURCE_USERNAME:-postgres.kcykicebvvsshyirgldl}
     SUPABASE_DATASOURCE_PASSWORD: ${SUPABASE_DATASOURCE_PASSWORD:-Adminpetsupbase12}
     LOCAL_DATASOURCE_URL: ${LOCAL_DATASOURCE_URL:-jdbc:postgresql://postgres:5432/appdb}
     LOCAL_DATASOURCE_USERNAME: ${LOCAL_DATASOURCE_USERNAME:-appuser}
     LOCAL_DATASOURCE_PASSWORD: ${LOCAL_DATASOURCE_PASSWORD:-apppass}
   ```

2. Updated `.env` file to include `SPRING_PROFILES_ACTIVE` variable

**Files Changed:**
- `docker-compose.yml` (backend-service and payment-service sections)
- `.env` (added `SPRING_PROFILES_ACTIVE`)

---

### Issue #2: Inconsistent Profile Configuration in application.yml

**Problem:**
- `payment-service` used different environment variable names (`SPRING_DATASOURCE_URL`) compared to `backend-service` (`SUPABASE_DATASOURCE_URL`, `LOCAL_DATASOURCE_URL`)
- Inconsistent pattern made it hard to maintain

**Root Cause:**
- Different services had different configuration patterns
- No standardization across services

**Fix Applied:**
1. Updated `src/main/resources/application.yml` (payment-service) to use same pattern as backend-service:
   ```yaml
   # PROD Profile - Supabase Cloud Postgres
   spring:
     config:
       activate:
         on-profile: prod
     datasource:
       url: ${SUPABASE_DATASOURCE_URL:jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres}
       username: ${SUPABASE_DATASOURCE_USERNAME:postgres.kcykicebvvsshyirgldl}
       password: ${SUPABASE_DATASOURCE_PASSWORD:Adminpetsupbase12}
   
   # DEV Profile - Local Docker Postgres
   spring:
     config:
       activate:
         on-profile: dev
     datasource:
       url: ${LOCAL_DATASOURCE_URL:jdbc:postgresql://postgres:5432/appdb}
       username: ${LOCAL_DATASOURCE_USERNAME:appuser}
       password: ${LOCAL_DATASOURCE_PASSWORD:apppass}
   ```

**Files Changed:**
- `src/main/resources/application.yml`

---

### Issue #3: Spring Boot Not Using Profile-Specific Configuration

**Problem:**
- Even with `SPRING_PROFILES_ACTIVE=prod` set, Spring Boot was trying to connect to `localhost:5432`
- Profile-specific datasource configuration was not being applied
- Logs showed profile was active but wrong database URL was used

**Root Cause:**
- Old JAR file in container had outdated `application.yml`
- Containers needed rebuild to include latest configuration
- Flyway migrations were not running automatically

**Fix Applied:**
1. Rebuilt containers to include latest `application.yml`
2. Verified environment variables were properly passed
3. Confirmed profile activation in logs

**Files Changed:**
- None (rebuild was required)

---

### Issue #4: Java 25 Build Failure

**Problem:**
- Maven build failing with error: `error: release version 25 not supported`
- Docker build was using Java 21 but trying to compile Java 25 code
- Maven compiler plugin version didn't support Java 25

**Root Cause:**
1. Maven Compiler Plugin version was not explicitly set (using old default)
2. Dockerfile was using Java 21 image instead of Java 25
3. Plugin configuration used deprecated `<source>` and `<target>` instead of `<release>`

**Fix Applied:**
1. Updated all `pom.xml` files to use Maven Compiler Plugin 3.14.1:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <version>3.14.1</version>
       <configuration>
           <release>25</release>
           <annotationProcessorPaths>
               <path>
                   <groupId>org.projectlombok</groupId>
                   <artifactId>lombok</artifactId>
                   <version>1.18.42</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```

2. Updated Dockerfile to use Java 25:
   ```dockerfile
   FROM eclipse-temurin:25-jdk AS build
   # ...
   FROM eclipse-temurin:25-jre
   ```

**Files Changed:**
- `backend-service/pom.xml`
- `api-gateway/pom.xml`
- `pom.xml` (root)
- `backend-service/Dockerfile`

---

### Issue #5: Missing ObjectMapper Bean

**Problem:**
- Application failed to start with error: `No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available`
- `WebhookService` required `ObjectMapper` but Spring Boot wasn't providing it automatically

**Root Cause:**
- Spring Boot auto-configuration should provide `ObjectMapper`, but it wasn't being detected
- Possible conflict or missing configuration

**Fix Applied:**
1. Created `JacksonConfig.java` to explicitly provide `ObjectMapper` bean:
   ```java
   @Configuration
   public class JacksonConfig {
       @Bean
       @Primary
       public ObjectMapper objectMapper() {
           return new ObjectMapper();
       }
   }
   ```

**Files Changed:**
- `backend-service/src/main/java/com/demo/backend/config/JacksonConfig.java` (shared config)

---

### Issue #6: Flyway Migrations Not Running Automatically

**Problem:**
- Flyway migrations were not executing on application startup
- No migration logs in application output
- Tables were not being created in Supabase database
- `flyway_schema_history` table was missing

**Root Cause:**
- Flyway was enabled in `application.yml` but not executing
- Possible issue with Flyway auto-configuration
- No manual way to trigger migrations

**Fix Applied:**
1. Created `FlywayConfig.java` to explicitly configure Flyway bean:
   ```java
   @Configuration
   public class FlywayConfig {
       @Bean
       public Flyway flyway(DataSource dataSource) {
           return Flyway.configure()
               .dataSource(dataSource)
               .locations("classpath:db/migration")
               .baselineOnMigrate(true)
               .baselineVersion("0")
               .schemas("public")
               .load();
       }
   }
   ```

2. Created `MigrationController.java` to manually trigger migrations via API:
   ```java
   @RestController
   @RequestMapping("/api/migrations")
   public class MigrationController {
       private final Flyway flyway;
       
       @PostMapping("/migrate")
       public ResponseEntity<Map<String, Object>> runMigrations() {
           MigrateResult result = flyway.migrate();
           // ... return response
       }
   }
   ```

3. Updated `SecurityConfig.java` to allow migration endpoints:
   ```java
   .requestMatchers("/api/migrations/**").permitAll()
   ```

**Files Changed:**
- `backend-service/src/main/java/com/demo/backend/config/FlywayConfig.java` (shared config)
- `backend-service/src/main/java/com/demo/backend/controller/MigrationController.java` (shared controller)
- `backend-service/src/main/java/com/demo/backend/config/SecurityConfig.java` (shared config)

**Result:**
- Successfully executed 9 migrations via API endpoint
- All tables created in Supabase database

---

## ✅ Final Working Configuration

### Environment Variables (.env)
```env
SPRING_PROFILES_ACTIVE=prod  # or 'dev' for local

# Supabase (PROD)
SUPABASE_DATASOURCE_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres
SUPABASE_DATASOURCE_USERNAME=postgres.kcykicebvvsshyirgldl
SUPABASE_DATASOURCE_PASSWORD=Adminpetsupbase12

# Local Docker (DEV)
LOCAL_DATASOURCE_URL=jdbc:postgresql://postgres:5432/appdb
LOCAL_DATASOURCE_USERNAME=appuser
LOCAL_DATASOURCE_PASSWORD=apppass
```

### How to Switch Databases

**Switch to Supabase (Production):**
```bash
# In .env file
SPRING_PROFILES_ACTIVE=prod

# Restart containers
docker-compose down
docker-compose up -d
```

**Switch to Local Docker (Development):**
```bash
# In .env file
SPRING_PROFILES_ACTIVE=dev

# Restart containers
docker-compose down
docker-compose up -d
```

### Run Migrations

**Via API Endpoint:**
```bash
curl -X POST http://localhost:8081/api/migrations/migrate
```

**Check Migration Status:**
```bash
curl -X POST http://localhost:8081/api/migrations/info
```

---

## 📊 Migration Results

After running migrations, the following tables were created in Supabase:

1. ✅ `users` - User identity data from Clerk
2. ✅ `organizations` - Multi-tenant organizations
3. ✅ `roles` - Authorization roles (ADMIN, USER)
4. ✅ `memberships` - User-organization-role relationships
5. ✅ `user_events` - Audit trail for user webhooks
6. ✅ `organization_events` - Audit trail for org webhooks
7. ✅ `auth_sessions` - Authentication session tracking
8. ✅ `payment_order` - Razorpay payment orders
9. ✅ `payment_transaction` - Razorpay payment transactions
10. ✅ `flyway_schema_history` - Flyway migration tracking

---

## 🔍 Key Learnings

1. **Always rebuild containers** after changing `application.yml` - JAR files are baked into images
2. **Java 25 requires** Maven Compiler Plugin 3.14.1+ and `<release>` tag instead of `<source>/<target>`
3. **Docker images must match** the Java version specified in `pom.xml`
4. **Flyway auto-configuration** may not always work - explicit bean configuration is more reliable
5. **Profile-specific configuration** in Spring Boot requires proper environment variable setup in docker-compose
6. **ObjectMapper** may need explicit configuration in some Spring Boot setups

---

## 🚀 Next Steps / Recommendations

1. **Automate migrations on startup** - Consider making Flyway run automatically on application start
2. **Add migration validation** - Verify migrations before deploying
3. **Document pgAdmin setup** - Add instructions for connecting to Supabase via pgAdmin
4. **Add health checks** - Include database connectivity in health endpoint
5. **Consider migration rollback** - Add support for rolling back migrations if needed

---

## 📝 Files Created/Modified

### New Files:
- `backend-service/src/main/java/com/demo/backend/config/JacksonConfig.java` (shared config)
- `backend-service/src/main/java/com/demo/backend/config/FlywayConfig.java` (shared config)
- `backend-service/src/main/java/com/demo/backend/controller/MigrationController.java` (shared controller)
- `run-migrations.sql` (manual migration script)

### Modified Files:
- `docker-compose.yml` - Added environment variable substitution
- `.env` - Added `SPRING_PROFILES_ACTIVE` and organized variables
- `backend-service/pom.xml` - Updated Maven Compiler Plugin for Java 25
- `api-gateway/pom.xml` - Updated Maven Compiler Plugin for Java 25
- `pom.xml` (root) - Updated Maven Compiler Plugin for Java 25
- `backend-service/Dockerfile` - Updated to Java 25
- `backend-service/src/main/resources/application.yml` - Updated logging levels
- `src/main/resources/application.yml` - Standardized profile configuration
- `backend-service/src/main/java/com/demo/backend/config/SecurityConfig.java` (shared config) - Added migration endpoint permission

---

## ✨ Success Metrics

- ✅ Database profile switching working (dev/prod)
- ✅ Java 25 compilation successful
- ✅ All 9 migrations executed successfully
- ✅ All tables created in Supabase
- ✅ Application starts successfully
- ✅ Migration API endpoint functional

---

**Last Updated:** January 23, 2026
**Branch:** `feature/connect-supabase-again`
