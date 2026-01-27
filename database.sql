-- Создание базы данных
CREATE DATABASE IF NOT EXISTS search_engine 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE search_engine;

-- Таблица сайтов
CREATE TABLE IF NOT EXISTS site (
    id INT AUTO_INCREMENT PRIMARY KEY,
    status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL,
    status_time DATETIME NOT NULL,
    last_error TEXT,
    url VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    INDEX idx_status (status),
    INDEX idx_url (url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица страниц
CREATE TABLE IF NOT EXISTS page (
    id INT AUTO_INCREMENT PRIMARY KEY,
    site_id INT NOT NULL,
    path TEXT NOT NULL,
    code INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE,
    INDEX idx_site_id (site_id),
    INDEX idx_path (path(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица лемм
CREATE TABLE IF NOT EXISTS lemma (
    id INT AUTO_INCREMENT PRIMARY KEY,
    site_id INT NOT NULL,
    lemma VARCHAR(255) NOT NULL,
    frequency INT NOT NULL,
    FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE,
    UNIQUE KEY unique_site_lemma (site_id, lemma),
    INDEX idx_lemma (lemma),
    INDEX idx_frequency (frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Таблица индекса
CREATE TABLE IF NOT EXISTS `index` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    page_id INT NOT NULL,
    lemma_id INT NOT NULL,
    `rank` FLOAT NOT NULL,
    FOREIGN KEY (page_id) REFERENCES page(id) ON DELETE CASCADE,
    FOREIGN KEY (lemma_id) REFERENCES lemma(id) ON DELETE CASCADE,
    UNIQUE KEY unique_page_lemma (page_id, lemma_id),
    INDEX idx_page_id (page_id),
    INDEX idx_lemma_id (lemma_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
