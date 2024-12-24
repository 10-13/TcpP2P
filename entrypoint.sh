#!/bin/bash

# Логирование запуска
echo "Starting application at $(date)"
zerotier-one -d --allowmanaged
sleep 5
# Запуск Spring Boot приложения
exec java -jar /app/app.jar
