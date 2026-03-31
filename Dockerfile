# We use the 'platform' flag to ensure it works on your Mac and their Windows
FROM --platform=linux/amd64 eclipse-temurin:21-jdk

# Install dependencies for GUI + Maven
RUN apt-get update && \
    apt-get install -y maven libgtk-3-0 libgbm1 libx11-6 libcanberra-gtk3-module && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the JAR
# We use -Djavafx.platform=linux to force Maven to grab the correct UI libs
RUN mvn clean package -DskipTests -Djavafx.platform=linux

# Run the JAR
# Adjust the name if your JAR isn't exactly 'otp-1.0-SNAPSHOT.jar'
CMD ["java", "-jar", "target/otp-1.0-SNAPSHOT.jar"]