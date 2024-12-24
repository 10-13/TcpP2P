#!/bin/bash

# Логирование запуска
echo "Starting application at $(date)"
zerotier-one -d
sleep 5
# Запуск Spring Boot приложения
exec java -jar /app/app.jar
