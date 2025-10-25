# Gunakan base image Java
FROM openjdk:17-jdk-slim

# Install dependencies
RUN apt-get update && apt-get install -y wget unzip dos2unix

# Set environment variable untuk Android SDK
ENV ANDROID_HOME /usr/local/android-sdk
ENV PATH ${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Install Android SDK Command Line Tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd ${ANDROID_HOME}/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip && \
    mv cmdline-tools latest && \
    rm cmdline-tools.zip

# Install SDK packages
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Fix permission & line ending (Windows-safe)
RUN dos2unix /app/gradlew && chmod +x /app/gradlew

# ✅ Jangan build di tahap image agar image cepat dibangun
#    Build dijalankan di Jenkins atau manual dengan docker run
CMD ["./gradlew", "assembleDebug"]