#!/usr/bin/env sh
# Compila el jar ejecutable (maven-shade-plugin) y lo deja en target/app.jar
set -eu

cd "$(dirname "$0")"
mvn -B clean package -DskipTests

cp target/comunicaciones-1.0-SNAPSHOT.jar target/app.jar
echo "Jar listo: $(pwd)/target/app.jar"
