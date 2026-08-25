# ---------------------------------------------------------------------------
# Stage 1 — build. The JDK and the Maven cache stay in this stage only, so
# they never reach the final image.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM alone first: as long as dependencies don't change, Docker reuses
# the cached layer below and skips re-downloading the world on every build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 — runtime. JRE only, non-root user, ~200MB instead of ~800MB.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl is only here so HEALTHCHECK below has something to call.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system spring \
 && useradd --system --gid spring spring

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

ARG BUILD_VERSION=dev
ENV APP_BUILD_VERSION=${BUILD_VERSION}
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
