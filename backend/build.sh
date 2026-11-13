#!/usr/bin/env sh
# Compila el jar ejecutable (spring-boot-maven-plugin) y lo deja en target/app.jar
set -eu

cd "$(dirname "$0")"
mvn -B clean package -DskipTests

jar=$(find target -maxdepth 1 -name 'backend-*.jar' ! -name '*.original' | head -n 1)
cp "$jar" target/app.jar
echo "Jar listo: $(pwd)/target/app.jar"
