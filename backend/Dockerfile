FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app
# 先拷贝 pom.xml 利用 Docker layer cache 缓存依赖
COPY pom.xml ./
# dependency:go-offline 首次较慢（下载所有依赖），用 --batch-mode 避免交互卡住
# -DskipTests 和 encoding 已配在 pom.xml properties 中，无需重复传
RUN mvn dependency:go-offline --batch-mode -DskipTests -q
COPY src ./src
RUN mvn package --batch-mode -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV LANG=C.UTF-8
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
