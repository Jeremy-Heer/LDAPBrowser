# LDAP Browser - Production Deployment Guide

## Requirements to Run the Production JAR

### Minimum System Requirements

#### Java Runtime Environment
- **Java Version**: Java 17 or higher (Java 21+ recommended)
- **JRE/JDK**: Any OpenJDK or Oracle JDK distribution
- **Architecture**: 64-bit (recommended)

#### System Resources
- **RAM**: Minimum 512MB, Recommended 1GB+
- **Disk Space**: ~100MB for the JAR file and temporary files
- **Network**: TCP port 8080 (default, configurable)

#### Operating System
- **Linux**: Any modern distribution (RHEL, Ubuntu, CentOS, etc.)
- **Windows**: Windows 10/11, Windows Server 2016+
- **macOS**: macOS 10.14+
- **Docker**: Any container runtime supporting Java

### JAR File Details
- **File**: `ldap-browser-1.0-SNAPSHOT.jar`
- **Size**: ~62MB (fully self-contained)
- **Type**: Spring Boot "fat JAR" with embedded Tomcat server

## Deployment Instructions

### 1. Basic Deployment

#### Copy the JAR file to target system:
```bash
# Copy the production JAR
scp target/ldap-browser-1.0-SNAPSHOT.jar user@targetserver:/opt/ldap-browser/
```

#### Run the application:
```bash
# Basic run (automatically uses production profile)
java -jar ldap-browser-1.0-SNAPSHOT.jar

# Run with custom port
java -jar ldap-browser-1.0-SNAPSHOT.jar --server.port=9090

# Run with custom memory settings
java -Xmx1g -jar ldap-browser-1.0-SNAPSHOT.jar

# Override profile if needed (not typically required)
java -jar ldap-browser-1.0-SNAPSHOT.jar --spring.profiles.active=development
```

### 2. Service Installation (Linux Systemd)

Create a systemd service file:
```bash
sudo nano /etc/systemd/system/ldap-browser.service
```

```ini
[Unit]
Description=LDAP Browser Application
After=network.target

[Service]
Type=simple
User=ldap-browser
WorkingDirectory=/opt/ldap-browser
ExecStart=/usr/bin/java -jar /opt/ldap-browser/ldap-browser-1.0-SNAPSHOT.jar --spring.profiles.active=production
Restart=always
RestartSec=10

# Environment
Environment=JAVA_OPTS="-Xmx1g"

[Install]
WantedBy=multi-user.target
```

Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable ldap-browser
sudo systemctl start ldap-browser
sudo systemctl status ldap-browser
```

### 3. Docker Deployment

Create a Dockerfile:
```dockerfile
FROM openjdk:17-jre-slim

# Create app user
RUN useradd -r -s /bin/false ldap-browser

# Create app directory
WORKDIR /app

# Copy JAR file
COPY target/ldap-browser-1.0-SNAPSHOT.jar app.jar

# Change ownership
RUN chown ldap-browser:ldap-browser app.jar

# Switch to app user
USER ldap-browser

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=production"]
```

Build and run:
```bash
docker build -t ldap-browser .
docker run -d -p 8080:8080 --name ldap-browser ldap-browser
```

## Configuration

### Application Properties
The JAR includes these configuration files:
- `application.properties` (base configuration)
- `application-production.properties` (production overrides)

### Environment Variables
Override configuration using environment variables:
```bash
# Port configuration
export SERVER_PORT=9090

# Logging level
export LOGGING_LEVEL_COM_EXAMPLE_LDAPBROWSER=INFO

# Run application
java -jar ldap-browser-1.0-SNAPSHOT.jar --spring.profiles.active=production
```

### JVM Options
Recommended JVM settings for production:
```bash
java -Xmx1g \
     -XX:+UseG1GC \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/ldap-browser/ \
     -jar ldap-browser-1.0-SNAPSHOT.jar \
     --spring.profiles.active=production
```

## Network Configuration

### Default Ports
- **HTTP**: 8080 (configurable via `--server.port=XXXX`)
- **Management**: Not exposed by default

### Firewall Requirements
```bash
# Allow HTTP traffic (adjust port as needed)
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload

# Or using iptables
sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
```

### Reverse Proxy (Optional)
Example Nginx configuration:
```nginx
server {
    listen 80;
    server_name ldap-browser.example.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring

### Health Check
```bash
# Check if application is running
curl http://localhost:8080/

# Check application logs
journalctl -u ldap-browser -f
```

### Log Files
- **Console Output**: Captured by systemd journal or docker logs
- **Application Logs**: Configurable via `logging.file.name` property

## Troubleshooting

### Common Issues
1. **Port Already in Use**: Change port with `--server.port=XXXX`
2. **Java Version**: Ensure Java 17+ is installed
3. **Memory Issues**: Increase heap size with `-Xmx2g`
4. **Frontend Resources**: Ensure `--spring.profiles.active=production` is used

### Verification Commands
```bash
# Check Java version
java -version

# Check if port is available
netstat -tlnp | grep :8080

# Test application accessibility
curl -I http://localhost:8080
```

## Dependencies Included
The JAR file is completely self-contained and includes:
- ✅ Embedded Tomcat web server
- ✅ Vaadin framework and frontend resources
- ✅ UnboundID LDAP SDK
- ✅ Spring Boot framework
- ✅ All runtime dependencies

**No additional dependencies or installations required beyond Java 17+**
