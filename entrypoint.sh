#!/bin/bash

# Логирование запуска
echo "Starting application at $(date)"

# Запуск Spring Boot приложения
exec java -jar /app.jar
