FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -B -Dmaven.test.skip=true package

FROM docker:29-cli AS docker-cli

FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin intercoach

RUN mkdir -p /var/lib/intercoach/execution && chown intercoach:intercoach /var/lib/intercoach/execution

ENV INTERCOACH_EXECUTION_WORKSPACE_ROOT=/var/lib/intercoach/execution

COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker

COPY --from=build /workspace/target/*.jar app.jar

USER intercoach

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
