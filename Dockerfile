FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -Pproduction -DskipTests

# Expose port
EXPOSE 8080

# Run application
CMD ["java", "-jar", "target/ldap-browser-1.0-SNAPSHOT.jar"]
