# Imagen final: solo copia el jar ya compilado por build.sh
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY target/app.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
