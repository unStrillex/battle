# WiFi Battle Platform - Android Cloud Build
# 基于 OpenJDK 17 + Android SDK 34

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/root/.gradle \
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 安装基础工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    wget curl unzip git ca-certificates \
    libstdc++6 libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

# 安装 Android SDK 命令行工具
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip && \
    unzip -q tools.zip && \
    mv cmdline-tools latest && \
    rm tools.zip

ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# 安装 Android 平台与构建工具
RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "ndk;26.1.10909125" \
    "cmake;3.22.1" \
    > /dev/null

WORKDIR /workspace

# 预热 Gradle（缓存依赖）
COPY gradle gradle
COPY gradle.properties settings.gradle build.gradle ./
COPY gradlew ./
RUN chmod +x gradlew && ./gradlew --version || true

COPY . .

# 默认入口：在容器内执行构建
ENTRYPOINT ["./scripts/build.sh"]
CMD ["release"]
