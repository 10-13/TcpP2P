# Сборочный образ
FROM maven:3.9.9-eclipse-temurin-23-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM zerotier/zerotier:latest AS zerotier


# Базовый образ
FROM openjdk:23-jdk-slim
COPY --from=zerotier /usr/sbin/zerotier-cli /usr/bin/zerotier-cli
COPY --from=zerotier /usr/sbin/zerotier-one /usr/bin/zerotier-one
RUN chmod +x /usr/bin/zerotier-cli
RUN chmod +x /usr/bin/zerotier-one
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

RUN apt-get install -y libtcnative-1

# Установка ZeroTier

# Очистка кэша apt
RUN apt-get clean && rm -rf /var/lib/apt/lists/*
# В Dockerfile или docker-entrypoint.sh

# Установка Python (если нужен)
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip
RUN apt-get update
RUN apt-get install -y libapr1 libapr1-dev libapache2-mod-jk
RUN apt-get install -y libtcnative-1

# Копирование приложения
COPY --from=build /app/target/P2P-0.0.1-SNAPSHOT.jar /app/app.jar
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
CMD ["--privileged"]
ENTRYPOINT ["/entrypoint.sh"]