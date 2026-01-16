# ==============================
# BUILD STAGE
# ==============================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper & config
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build
RUN ./mvnw clean package -DskipTests

# ==============================
# RUN STAGE
# ==============================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
