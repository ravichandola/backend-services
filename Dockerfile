# ==============================
# BUILD STAGE (Java 25)
# ==============================
FROM eclipse-temurin:25-jdk AS build

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

# Build with Java 25
RUN ./mvnw clean package -DskipTests


# ==============================
# RUN STAGE (Java 25)
# ==============================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
