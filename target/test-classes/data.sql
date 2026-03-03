-- clean tables (order matters)
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE reviews;
TRUNCATE TABLE materials;
TRUNCATE TABLE participants;
TRUNCATE TABLE classes;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- MOCK DATA
INSERT INTO users (username, password_hash, email)
VALUES 
('alice', 'hash1', 'alice@example.com'),
('bob', 'hash2', 'bob@example.com'),
('carol', 'hash3', 'carol@example.com'),
('dave', 'hash4', 'dave@example.com');

INSERT INTO classes (class_name, creator_id, topic)
VALUES
('Math 101', 3, 'Algebra'),
('Physics 201', 4, 'Mechanics'),
('History 101', 3, 'Ancient Civilizations');

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