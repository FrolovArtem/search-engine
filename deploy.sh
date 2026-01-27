#!/bin/bash

# Скрипт для деплоя на Timeweb Cloud

echo "=== Установка зависимостей ==="
sudo apt update
sudo apt install -y openjdk-11-jdk maven mysql-server git

echo "=== Настройка MySQL ==="
sudo mysql -e "CREATE DATABASE IF NOT EXISTS search_engine CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'searchengine'@'localhost' IDENTIFIED BY 'SearchEngine2024!';"
sudo mysql -e "GRANT ALL PRIVILEGES ON search_engine.* TO 'searchengine'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "=== Клонирование проекта ==="
cd /opt
if [ -d "search-engine" ]; then
    cd search-engine
    git pull
else
    git clone https://github.com/FrolovArtem/search-engine.git
    cd search-engine
fi

echo "=== Сборка проекта ==="
mvn clean package -DskipTests

echo "=== Создание systemd сервиса ==="
sudo tee /etc/systemd/system/search-engine.service > /dev/null <<EOF
[Unit]
Description=Search Engine Application
After=mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/search-engine
ExecStart=/usr/bin/java -jar /opt/search-engine/target/SearchEngine-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

echo "=== Запуск сервиса ==="
sudo systemctl daemon-reload
sudo systemctl enable search-engine
sudo systemctl restart search-engine

echo "=== Проверка статуса ==="
sudo systemctl status search-engine

echo ""
echo "✅ Деплой завершен!"
echo "Приложение доступно на порту 8080"
echo "Проверьте: curl http://localhost:8080"
