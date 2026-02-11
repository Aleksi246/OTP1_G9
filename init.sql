DROP DATABASE IF EXISTS otptestdb;

CREATE DATABASE IF NOT EXISTS otptestdb;
USE otptestdb;

CREATE USER IF NOT EXISTS 'otptestuser'@'localhost' IDENTIFIED BY 'testpass';
GRANT ALL PRIVILEGES ON otptestdb.* TO 'otptestuser'@'localhost';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    user_type ENUM('student', 'teacher') NOT NULL,
    access_token VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS classes (
    class_id INT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(100) UNIQUE NOT NULL,
    topic VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS participants (
    user_id INT,
    class_id INT,
    PRIMARY KEY (user_id, class_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS  materials(
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename CHAR(36) NOT NULL,
    filepath VARCHAR(512),
    material_type VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    class_id INT,
    user_id INT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    review TEXT,
    rating INT CHECK (rating >= 0 AND rating <= 5),
    file_id INT,
    user_id INT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES materials(file_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

