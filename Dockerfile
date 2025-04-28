# ------------------------------ BUILD ------------------------------
FROM maven:3.9.6-eclipse-temurin-21 as java

# Install the Protobuf compiler.
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive \
    apt-get install --no-install-recommends --assume-yes \
      protobuf-compiler

# Set the working directory.
WORKDIR /app

# Download dependencies (with support for caching).
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code into the container.
COPY src ./src

# Run the Protobuf compiler.
RUN protoc --java_out=src/main/generated src/Messages.proto

# Build the application.
RUN mvn package -DskipTests

# ------------------------------ NODEJS ------------------------------
FROM node:20 as node

# Install the Protobuf compiler.
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive \
    apt-get install --no-install-recommends --assume-yes \
      protobuf-compiler

# Set the working directory.
WORKDIR /node

# Copy the source code into the container.
COPY src/node .

# Install the helper executables.
RUN npm install -g @protobuf-ts/plugin @vercel/ncc typescript

# Copy the Protobuf file into the container.
COPY src/Messages.proto .

# Run the Protobuf compiler.
RUN protoc --ts_out=src Messages.proto

# Compile the TypeScript code.
RUN npm run build

# ------------------------------ RUN ------------------------------
FROM ubuntu:22.04 as run

# Update the package list.
RUN apt-get update

# Install Java.
RUN apt-get install -y openjdk-21-jdk

# Install Node.js.
RUN apt install -y curl \
  && curl -sL https://deb.nodesource.com/setup_22.x | bash - \
  && apt install -y nodejs \
  && curl -L https://www.npmjs.com/install.sh | sh

# Copy the Java application from the build stage.
COPY --from=java /app/target/Laudiolin-Backend-*.jar /app/Laudiolin-Backend.jar

# Copy the Node.js application from the build stage.
COPY --from=node /node/dist/index.js /app/index.js

# Expose the web server port.
EXPOSE 3000

# Run the Java application.
CMD ["java", "-jar", "Laudiolin-Backend.jar"]
