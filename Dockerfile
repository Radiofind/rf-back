FROM maven:3.10.0-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package -DskipTests \
  && cp "$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" app.jar


FROM eclipse-temurin:25

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]