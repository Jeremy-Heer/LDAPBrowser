#!/bin/bash

# LDAP Browser Development Script with Live Reload
echo "Starting LDAP Browser with Live Reload enabled..."

# Set Java options for development
export JAVA_OPTS="-Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true"

# Run with Spring Boot DevTools
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="$JAVA_OPTS" \
  -Dspring.profiles.active=development
