# Etapa 1: compila o jar (os testes rodam no seu PC ou no GitHub Actions, não aqui)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -Dmaven.test.skip=true

# Etapa 2: só o necessário para rodar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system caderneta && useradd --system --gid caderneta caderneta
COPY --from=build /app/target/caderneta-zap.jar app.jar
USER caderneta
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
