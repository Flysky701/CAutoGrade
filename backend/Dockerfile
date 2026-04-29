FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -DskipTests -Dproject.build.sourceEncoding=UTF-8 dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests -Dproject.build.sourceEncoding=UTF-8 package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV LANG=C.UTF-8
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
