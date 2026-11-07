FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl bootstrap -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /opt/sixpay
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 --create-home sixpay
COPY --from=build /workspace/bootstrap/target/bootstrap-*.jar /opt/sixpay/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/sixpay/app.jar"]
