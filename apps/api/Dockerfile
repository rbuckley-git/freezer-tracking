# syntax=docker/dockerfile:1.6
## Use cache mounts for Gradle wrapper/distribution and dependency caches.
## This keeps gradle-8.7-bin.zip from being re-downloaded on every image build.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
ENV GRADLE_USER_HOME=/root/.gradle
COPY certs/ZscalerRootCertificate-2048-SHA256.pem /usr/local/share/ca-certificates/zscaler.crt
RUN update-ca-certificates && \
    keytool -importcert -noprompt -storepass changeit \
      -keystore "$JAVA_HOME/lib/security/cacerts" \
      -alias zscaler \
      -file /usr/local/share/ca-certificates/zscaler.crt
# Copy wrapper and build metadata first so Gradle distribution download can be layer-cached.
COPY apps/api/gradlew /app/gradlew
COPY apps/api/gradle /app/gradle
COPY apps/api/build.gradle /app/build.gradle
COPY apps/api/settings.gradle /app/settings.gradle
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon help >/dev/null

COPY apps/api/ /app/
RUN --mount=type=cache,target=/root/.gradle \
    for i in 1 2 3; do \
      ./gradlew bootJar --no-daemon && exit 0; \
      echo "Gradle build failed (attempt ${i}/3), retrying in 5s..." >&2; \
      sleep 5; \
    done; \
    exit 1

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --create-home app
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
