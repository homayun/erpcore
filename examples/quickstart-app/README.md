# Quickstart App

This is a minimal Spring Boot application that consumes the erp-security-module.
It demonstrates how easy it is to add multi-tenant security to a new project.

## Run locally

1. From the root of the repository, install the security module to your local Maven cache:
   mvn clean install -DskipTests
2. Navigate to this directory:
   cd examples/quickstart-app
3. Run the app:
   mvn spring-boot:run

## Test the API

The app automatically bootstraps an admin user on startup.
Get a token:

curl -X POST http://localhost:8080/login -H 'Content-Type: application/json' -d '{"tenantId":"11111111-1111-1111-1111-111111111111","username":"admin","password":"AdminPassword1!"}'

Use the token to access the protected endpoint:

curl http://localhost:8080/api/me -H 'Authorization: Bearer <YOUR_TOKEN>'