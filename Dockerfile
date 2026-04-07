FROM eclipse-temurin:21-jdk

# Install dependencies for GUI + Maven
RUN apt-get update && \
    apt-get install -y maven libgtk-3-0 libgbm1 libx11-6 libcanberra-gtk3-module libgl1 libxrender1 libxi6 libxtst6 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src
COPY init.sql ./init.sql

# Build the JAR with a JavaFX platform classifier that matches the image architecture
ARG TARGETARCH
RUN if [ "$TARGETARCH" = "arm64" ]; then \
      JAVAFX_PLATFORM="linux-aarch64"; \
    else \
      JAVAFX_PLATFORM="linux"; \
    fi && \
    echo "Building with javafx.platform=$JAVAFX_PLATFORM" && \
    mvn clean package -DskipTests -Djavafx.platform="$JAVAFX_PLATFORM"

# Run the JAR
# Adjust the name if your JAR isn't exactly 'otp-1.0-SNAPSHOT.jar'
CMD ["java", "-jar", "target/otp-1.0-SNAPSHOT.jar"]
