# Imagen final: solo copia el jar ya compilado por build.sh
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
