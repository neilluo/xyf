FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="neilluo"

RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Singapore /etc/localtime && \
    echo "Asia/Singapore" > /etc/timezone

WORKDIR /app
COPY distributor-server/target/*.jar app.jar

# Log directory
RUN mkdir -p /app/logs

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
