# 📡 RF-Back (RadioFind Backend)

Backend application for the **RadioFind** system, built using Spring Boot. The project features a monolithic architecture with a REST API, ensuring secure data management and integration with PostgreSQL.

## 🚀 Tech stack

- Java 25
- Spring Boot 4.0.5
- Maven
- Spring Web — creating REST API
- Spring Security — authentication and authorization
- Spring Data JPA — work with database
- PostgreSQL — main database
- Lombok — reducing boilerplate code
- Spring Validation — input data validation

## 📦 Main characteristics of the project

| Параметр      | Значение        |
| ------------- | --------------- |
| Group         | com.radiofind   |
| Artifact      | Monolit         |
| Packaging     | jar             |
| configuration | application.yml |
| Architecture  | Monolit         |

## 📁 Project structure

rf-back/.  
├── src/.  
│ ├── main/.  
│ │ ├── java/com/radiofind/.  
│ │ │ ├── config/ # Configurations (Security, Beans).  
│ │ │ ├── controller/ # REST controllers.  
│ │ │ ├── service/ # Business logic.  
│ │ │ ├── repository/ # JPA repos.  
│ │ │ ├── model/ # Entities.  
│ │ │ └── dto/ # Data Transfer Objects.  
│ │ └── resources/.  
│ │ ├── application.yml.  
│ │ └── db/.  
│ │ └── migration/ # (optional) Migrations.  
├── pom.xml.  
└── README.md.

## ⚙️ Configuration

The main settings are in the file:
src/main/resources/application.yml

The example of configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/radiofind
    username: postgres
    password: password

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  security:
    user:
      name: admin
      password: admin
```

## ▶️ Project run

1. Cloning a repo

```Bash
git clone https://github.com/your-username/rf-back.git
cd rf-back
```

2. Building the project

```Bash
mvn clean install
```

3. Running the application

```Bash
mvn spring-boot:run
```

or

```Bash
java -jar target/Monolit.jar
```

## 🔐 Security

The project uses Spring Security:

- Basic authentication (default)
- Extendable to JWT/OAuth2
- Protecting REST endpoints

## 📡 API

Base URL: http://localhost:8080

Example endpoint: GET /api/v1/...

## 🧪 Validation

Spring Validation is used:

- @NotNull
- @Size
- @Email
- other annotations

## 🛢️ Work with the database

- Used PostgreSQL
- ORM: Hibernate (JPA)
- Repositories via JpaRepository

## 📌 Peculiarities

- Clean architecture (Controller → Service → Repository)
- Using DTOs to isolate a Model
- Minifying boilerplate code with Lombok
