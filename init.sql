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

-- MOCK DATA

INSERT INTO users (username, password_hash, user_type, access_token)
VALUES 
('alice', 'hash1', 'student', NULL),
('bob', 'hash2', 'student', NULL),
('carol', 'hash3', 'teacher', NULL),
('dave', 'hash4', 'teacher', NULL);

INSERT INTO classes (class_name, topic)
VALUES
('Math 101', 'Algebra'),
('Physics 201', 'Mechanics'),
('History 101', 'Ancient Civilizations');

INSERT INTO participants (user_id, class_id)
VALUES
(1, 1),
(2, 1),
(1, 2),
(2, 3),
(3, 1),
(4, 2);

INSERT INTO materials (original_filename, stored_filename, filepath, material_type, class_id, user_id)
VALUES
('Algebra Basics.pdf', '550e8400-e29b-41d4-a716-446655440000', '/uploads/class_1/550e8400-e29b-41d4-a716-446655440000.pdf', 'PDF', 1, 3),
('Mechanics Notes.docx', '660e8400-e29b-41d4-a716-446655440001', '/uploads/class_2/660e8400-e29b-41d4-a716-446655440001.docx', 'DOCX', 2, 4),
('History Overview.pptx', '770e8400-e29b-41d4-a716-446655440002', '/uploads/class_3/770e8400-e29b-41d4-a716-446655440002.pptx', 'PPTX', 3, NULL);

INSERT INTO reviews (review, rating, file_id, user_id)
VALUES
('Great explanation of algebra!', 5, 1, 1),
('Helpful notes for mechanics.', 4, 2, 2),
('Interesting history slides.', 5, 3, 1);


