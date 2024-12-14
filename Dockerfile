# Базовый образ
FROM openjdk:23-jdk-slim

# Установка системных зависимостей
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    net-tools \
    iputils-ping \
    dnsutils \
    iproute2 \
    netcat-traditional \
    procps \
    htop \
    nano \
    vim \
    less \
    build-essential \
    git

# Установка ZeroTier
RUN curl -s https://install.zerotier.com | bash

# Очистка кэша apt
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Установка Python (если нужен)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip

# Копирование приложения
COPY target/P2P-0.0.1-SNAPSHOT.jar /app/app.jar

# Создание рабочей директории
WORKDIR /app

# Копирование скриптов и конфигураций
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Переменные окружения
ENV ZEROTIER_TOKEN=VPj5xJYBLZAzCUuXOs8nG3aYKAlSByev

# Порты (примерный список)
EXPOSE 8080
EXPOSE 22
EXPOSE 443
EXPOSE 8090

# Точка входа
ENTRYPOINT ["/entrypoint.sh"]