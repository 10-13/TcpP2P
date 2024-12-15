FROM maven:3.9.9-eclipse-temurin-23-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

FROM zerotier/zerotier:latest AS zerotier


# Базовый образ
FROM openjdk:23-jdk-slim
COPY --from=zerotier /usr/sbin/zerotier-cli /usr/bin/zerotier-cli
COPY --from=zerotier /usr/sbin/zerotier-one /usr/bin/zerotier-one
RUN chmod +x /usr/bin/zerotier-cli
RUN chmod +x /usr/bin/zerotier-one

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

RUN apt-get clean && rm -rf /var/lib/apt/lists/*

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip
RUN apt-get update
RUN apt-get install -y libapr1 libapr1-dev libapache2-mod-jk
RUN apt-get install -y libtcnative-1

COPY --from=build /app/target/P2P-0.0.1-SNAPSHOT.jar /app/app.jar

WORKDIR /app

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ARG ZEROTIER_TOKEN
ENV ZEROTIER_TOKEN=${ZEROTIER_TOKEN}

ARG DATABASE_URL
ENV DATABASE_URL=${DATABASE_URL}

ARG DATABASE_USERNAME
ENV DATABASE_USERNAME=${DATABASE_USERNAME}

ARG DATABASE_PASSWORD
ENV DATABASE_PASSWORD=${DATABASE_PASSWORD}



EXPOSE 8080
EXPOSE 22
EXPOSE 443
EXPOSE 8090

CMD ["--privileged"]
ENTRYPOINT ["/entrypoint.sh"]