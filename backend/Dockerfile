FROM maven:3.9.16-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN groupadd --system --gid 10001 reviewlens \
    && useradd --system --uid 10001 --gid 10001 --home-dir /app --no-create-home reviewlens

COPY --from=build --chown=10001:10001 /workspace/target/*.jar /app/app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
