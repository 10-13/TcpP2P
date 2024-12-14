# Базовый образ
FROM openjdk:17-jdk-slim

# Установка системных зависимостей
RUN apt-get update && apt-get install -y \
    # Сетевые утилиты
    wget \
    curl \
    net-tools \
    iputils-ping \
    dnsutils \
    iproute2 \
    netcat \
    
    # Утилиты для работы с системой
    procps \
    htop \
    nano \
    vim \
    less \
    
    # Компиляторы и инструменты разработки
    build-essential \
    git \
    
    # ZeroTier
    && curl -s https://install.zerotier.com | sudo bash
    
    # Очистка кэша apt
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Установка Python (если нужен)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip

# Базовые Python библиотеки
RUN pip3 install \
    requests \
    paramiko \
    psutil \
    flask

# Копирование приложения
COPY target/*.jar app.jar

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
