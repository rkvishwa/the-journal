FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY . .
RUN chmod +x mvnw && ./mvnw clean package

FROM eclipse-temurin:17-jre-jammy

RUN useradd --system --uid 10001 --create-home --home-dir /app blogapp

WORKDIR /app
COPY --from=build /workspace/target/personal-blog-engine-0.0.1-SNAPSHOT.jar /app/app.jar

RUN mkdir -p /app/data/uploads && chown -R blogapp:blogapp /app

USER blogapp

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
