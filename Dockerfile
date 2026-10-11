# Multi-stage Dockerfile for MB Nursing Home HMS
# Stage 1: Build the Android APK using Temurin JDK 21 and Gradle
FROM eclipse-temurin:21-jdk AS android-builder

WORKDIR /workspace

# Copy Gradle wrapper and configuration files
COPY gradle /workspace/gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew /workspace/
COPY app /workspace/app
COPY debug.keystore.base64 /workspace/

# Grant execution permissions to Gradle wrapper
RUN chmod +x gradlew

# Decode debug keystore if present
RUN if [ -f "debug.keystore.base64" ]; then \
        base64 -d debug.keystore.base64 > debug.keystore || true; \
    fi

# Build Debug APK
RUN ./gradlew assembleDebug --no-daemon --stacktrace

# Stage 2: Serve Web Management Portal & Host Downloadable APK via Nginx
FROM nginx:alpine

# Copy web management portal assets
COPY web /usr/share/nginx/html

# Copy compiled APK to download directory
RUN mkdir -p /usr/share/nginx/html/download
COPY --from=android-builder /workspace/app/build/outputs/apk/debug/app-debug.apk /usr/share/nginx/html/download/mb-nursing-home-hms-latest.apk

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
