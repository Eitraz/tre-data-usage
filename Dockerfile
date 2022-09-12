FROM maven:3.8.6-eclipse-temurin-17 as builder

WORKDIR /tre

COPY ./src ./src
COPY ./pom.xml ./pom.xml

RUN mvn clean package -Pdocker

FROM openjdk:17-alpine

# Copy app from builder
COPY --from=builder /tre/target/tre-data-usage-0.0.1.jar /opt/tre/tre-data-usage.jar

# Create user/group that our application will be running as
RUN addgroup --gid 1001 tre \
    && adduser --ingroup tre --shell /bin/false --disabled-password --gecos "" --uid 1001 tre \
    && chown -R tre:tre /opt/tre

# Start tre backend
WORKDIR /opt/tre/
USER tre
ENTRYPOINT java -jar tre-data-usage.jar
EXPOSE 8080
