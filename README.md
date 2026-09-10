# Catalog Backend

This application provides an API for the management of catalog resources. The first supported resource type is
information models, described according to the [ModellDCAT-AP-NO](https://data.norge.no/specification/modelldcat-ap-no) specification.

For a broader understanding of the system’s context, refer to the [architecture documentation](https://github.com/Informasjonsforvaltning/architecture-documentation) wiki.
For more specific context on this application, see the **Registration** subsystem section.

## Getting Started

These instructions will give you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Ensure you have the following installed:

- Java 21
- Maven
- Docker

### Running locally

```sh
mvn spring-boot:run
```

### API Documentation (OpenAPI)

Once the application is running locally, the API documentation can be accessed
at http://localhost:8080/swagger-ui/index.html

### Formatting code

```sh
mvn ktlint:format
```
