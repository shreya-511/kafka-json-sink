# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

# Install unzip
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

# Build project
RUN mvn clean install -DskipTests

# Unzip the distribution
RUN unzip target/kafka-json-sink-*-distribution.zip -d target/kafka-json-sink

# Runtime stage4
FROM eclipse-temurin:21-jre

WORKDIR /kafka-json-sink

# Copy build artifacts
COPY --from=build /app/target/kafka-json-sink-1.0.jar /kafka-json-sink/
COPY --from=build /app/target/kafka-json-sink/kafka-json-sink-*/lib/* /kafka-json-sink/lib/
COPY --from=build /app/target/kafka-json-sink /kafka-json-sink/kafka-json-sink

RUN mv kafka-json-sink-*.jar kafka-json-sink.jar

ENTRYPOINT ["java", "-Xms1g", "-Xmx2g", "-jar", "kafka-json-sink.jar"]