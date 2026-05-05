-- =========================================================================
-- GROW21 DBMS — Role-Based Child Progress Tracking System (NORMALIZED, BCNF)
-- =========================================================================
-- This file builds the database in its final normalized form. For the
-- before/after migration narrative used in the viva, see
-- Grow21_Normalization.sql.
-- =========================================================================

CREATE DATABASE IF NOT EXISTS Grow21_DB;
USE Grow21_DB;

-- ==================== DDL: CREATE ====================

CREATE TABLE School (
    SchoolID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(150) NOT NULL,
    Location VARCHAR(200) NOT NULL
);

-- PK: ParentID
-- Email is intentionally NOT stored here — it lives in the User table
-- (single source of truth for login identity, see normalization doc).
CREATE TABLE Parent (
    ParentID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(100) NOT NULL,
    Phone VARCHAR(15) NULL
);

-- PK: EducatorID | FK: SchoolID -> School
-- Email lives in the User table.
CREATE TABLE Educator (
    EducatorID INT PRIMARY KEY AUTO_INCREMENT,
    SchoolID INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    FOREIGN KEY (SchoolID) REFERENCES School(SchoolID)
);

-- One Parent -> Many Children
-- DateOfBirth replaces the previously-stored derived attribute Age.
CREATE TABLE Child (
    ChildID INT PRIMARY KEY AUTO_INCREMENT,
    ParentID INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    DateOfBirth DATE NOT NULL,
    BaselineSkillLevel VARCHAR(50) NOT NULL DEFAULT 'Beginner',
    FOREIGN KEY (ParentID) REFERENCES Parent(ParentID)
);

-- Centralized auth table — single login row for every actor (Admin/Parent/Teacher/Child).
-- Username is unique (it is an email for adults, a handle for kids).
CREATE TABLE User (
    UserID INT PRIMARY KEY AUTO_INCREMENT,
    Username VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Role ENUM('Admin', 'Parent', 'Teacher', 'Child') NOT NULL,
    ParentID INT NULL,
    EducatorID INT NULL,
    ChildID INT NULL,
    FOREIGN KEY (ParentID)   REFERENCES Parent(ParentID),
    FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID),
    FOREIGN KEY (ChildID)    REFERENCES Child(ChildID)
);

-- Many-to-Many mapping | Composite PK
CREATE TABLE Child_Educator (
    ChildID INT NOT NULL,
    EducatorID INT NOT NULL,
    PRIMARY KEY (ChildID, EducatorID),
    FOREIGN KEY (ChildID)    REFERENCES Child(ChildID),
    FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID)
);

CREATE TABLE Module (
    ModuleID INT PRIMARY KEY AUTO_INCREMENT,
    ModuleName VARCHAR(100) NOT NULL,
    Category VARCHAR(100) NOT NULL,
    DifficultyLevel VARCHAR(50) NOT NULL
);

CREATE TABLE Lesson (
    LessonID INT PRIMARY KEY AUTO_INCREMENT,
    ModuleID INT NOT NULL,
    LessonTitle VARCHAR(200) NOT NULL,
    TargetSkill VARCHAR(100) NOT NULL,
    FOREIGN KEY (ModuleID) REFERENCES Module(ModuleID)
);

-- Question entity — fixes the previously-phantom FK in Attempt.
CREATE TABLE Question (
    QuestionID INT PRIMARY KEY AUTO_INCREMENT,
    LessonID INT NOT NULL,
    QuestionText VARCHAR(255) NOT NULL,
    CorrectAnswer VARCHAR(100) NULL,
    FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID)
);

CREATE TABLE Session (
    SessionID INT PRIMARY KEY AUTO_INCREMENT,
    ChildID INT NOT NULL,
    LessonID INT NOT NULL,
    SessionDate DATE NOT NULL,
    Duration INT NOT NULL,
    CompletionPercentage INT DEFAULT 0,
    FOREIGN KEY (ChildID)  REFERENCES Child(ChildID),
    FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID)
);

CREATE TABLE Attempt (
    AttemptID INT PRIMARY KEY AUTO_INCREMENT,
    SessionID INT NOT NULL,
    QuestionID INT NOT NULL,
    IsCorrect TINYINT(1) NOT NULL,
    ResponseTime DECIMAL(5,2) NOT NULL,
    StreakCount INT DEFAULT 0,
    FOREIGN KEY (SessionID)  REFERENCES Session(SessionID),
    FOREIGN KEY (QuestionID) REFERENCES Question(QuestionID)
);

-- ==================== DML: INSERT ====================

INSERT INTO School (Name, Location) VALUES
('Delhi Public School', 'New Delhi'),
('Ryan International', 'Gurugram'),
('Sanskriti School', 'Chandigarh');

INSERT INTO Parent (Name) VALUES
('Sunita Sharma'),
('Rakesh Mehta'),
('Priya Singh'),
('Amit Verma'),
('Sneha Kapoor'),
('Rohit Das');

INSERT INTO Educator (SchoolID, Name) VALUES
(1, 'Ananya Iyer'),
(2, 'Vikram Rao'),
(1, 'Deepa Nair');

-- Children must be inserted BEFORE child User rows (FK).
-- DateOfBirth chosen so TIMESTAMPDIFF(YEAR, DOB, '2026-05-05') matches the
-- previous Age values for demo continuity.
INSERT INTO Child (ParentID, Name, DateOfBirth, BaselineSkillLevel) VALUES
(1, 'Aryan Sharma',  '2018-01-15', 'Beginner'),
(1, 'Myra Sharma',   '2019-03-10', 'Beginner'),
(2, 'Riya Mehta',    '2016-07-22', 'Intermediate'),
(2, 'Vivaan Mehta',  '2021-02-08', 'Beginner'),
(3, 'Dev Singh',     '2019-04-30', 'Beginner'),
(3, 'Diya Singh',    '2018-09-05', 'Intermediate'),
(4, 'Aarav Verma',   '2020-06-18', 'Beginner'),
(4, 'Kabir Verma',   '2016-11-12', 'Advanced'),
(5, 'Kavya Kapoor',  '2017-08-25', 'Intermediate'),
(6, 'Ishaan Das',    '2015-12-03', 'Advanced');

-- All login identities live in the User table.
INSERT INTO User (Username, Password, Role, ParentID, EducatorID, ChildID) VALUES
('admin@grow21.com',  'admin123', 'Admin',   NULL, NULL, NULL),
('sunita@gmail.com',  'pass123',  'Parent',  1,    NULL, NULL),
('rakesh@gmail.com',  'pass123',  'Parent',  2,    NULL, NULL),
('priya@gmail.com',   'pass123',  'Parent',  3,    NULL, NULL),
('amit@gmail.com',    'pass123',  'Parent',  4,    NULL, NULL),
('sneha@gmail.com',   'pass123',  'Parent',  5,    NULL, NULL),
('rohit@gmail.com',   'pass123',  'Parent',  6,    NULL, NULL),
('ananya@school.com', 'pass123',  'Teacher', NULL, 1,    NULL),
('vikram@school.com', 'pass123',  'Teacher', NULL, 2,    NULL),
('deepa@school.com',  'pass123',  'Teacher', NULL, 3,    NULL),
-- Child accounts:
('aryansharma1',  'pass123', 'Child', NULL, NULL, 1),
('myrasharma7',   'pass123', 'Child', NULL, NULL, 2),
('riyamehta2',    'pass123', 'Child', NULL, NULL, 3),
('vivaanmehta8',  'pass123', 'Child', NULL, NULL, 4),
('devsingh3',     'pass123', 'Child', NULL, NULL, 5),
('diyasingh9',    'pass123', 'Child', NULL, NULL, 6),
('aaravverma4',   'pass123', 'Child', NULL, NULL, 7),
('kabirverma10',  'pass123', 'Child', NULL, NULL, 8),
('kavyakapoor5',  'pass123', 'Child', NULL, NULL, 9),
('ishaandas6',    'pass123', 'Child', NULL, NULL, 10);

INSERT INTO Child_Educator (ChildID, EducatorID) VALUES
(1, 1), (2, 1), (3, 2), (5, 1), (7, 3), (8, 2), (9, 3), (10, 2);

INSERT INTO Module (ModuleName, Category, DifficultyLevel) VALUES
('Move and Play', 'Motor Skills', 'Easy'),
('Vocabulary',    'Language',     'Medium'),
('Puzzles',       'Cognitive',    'Hard');

INSERT INTO Lesson (ModuleID, LessonTitle, TargetSkill) VALUES
(1, 'Trace Lines',       'Fine Motor'),
(1, 'Draw Shapes',       'Fine Motor'),
(1, 'Free Draw',         'Creativity'),
(2, 'Image-based MCQ',   'Vocabulary Identification'),
(3, 'Color Sorting',     'Logic'),
(3, 'Memory Flip',       'Memory'),
(3, 'Drag and Match',    'Logic');

-- Seed a small bank of Questions per Lesson so Attempt FK is satisfied.
INSERT INTO Question (LessonID, QuestionText, CorrectAnswer) VALUES
(1, 'Trace the straight line', 'Done'),
(1, 'Trace the curved line',   'Done'),
(1, 'Trace the zigzag',        'Done'),
(1, 'Trace the spiral',        'Done'),
(2, 'Draw a circle',           'Circle'),
(2, 'Draw a square',           'Square'),
(2, 'Draw a triangle',         'Triangle'),
(3, 'Free draw your favourite animal', NULL),
(3, 'Free draw a tree',        NULL),
(3, 'Free draw a house',       NULL),
(4, 'Identify: apple',         'Apple'),
(4, 'Identify: ball',          'Ball'),
(4, 'Identify: cat',           'Cat'),
(4, 'Identify: dog',           'Dog'),
(5, 'Sort red items',          'Red'),
(5, 'Sort blue items',         'Blue'),
(5, 'Sort green items',        'Green'),
(6, 'Match the pair: star',    'Star'),
(6, 'Match the pair: moon',    'Moon'),
(6, 'Match the pair: sun',     'Sun'),
(6, 'Match the pair: cloud',   'Cloud'),
(6, 'Match the pair: tree',    'Tree'),
(7, 'Drag fruit to basket',    'Basket'),
(7, 'Drag toy to box',         'Box'),
(7, 'Drag book to shelf',      'Shelf');

INSERT INTO Session (ChildID, LessonID, SessionDate, Duration, CompletionPercentage) VALUES
(1, 1, '2026-04-20', 15, 100), (1, 2, '2026-04-21', 20, 80), (1, 3, '2026-04-22', 12, 100),
(1, 4, '2026-04-23', 18, 60),  (1, 5, '2026-04-24', 14, 100),
(2, 1, '2026-04-20', 10, 100), (2, 6, '2026-04-22', 15, 40),
(3, 4, '2026-04-21', 22, 100), (3, 7, '2026-04-23', 25, 90), (3, 1, '2026-04-25', 18, 100),
(5, 1, '2026-04-20', 12, 100), (5, 2, '2026-04-22', 16, 75),
(7, 1, '2026-04-21', 10, 100), (8, 4, '2026-04-22', 20, 100),
(9, 3, '2026-04-23', 18, 100), (9, 5, '2026-04-24', 22, 100),
(10, 5, '2026-04-20', 30, 80), (10, 4, '2026-04-22', 25, 100);

-- Attempts now reference real Question rows (1..25 above).
INSERT INTO Attempt (SessionID, QuestionID, IsCorrect, ResponseTime, StreakCount) VALUES
(1,1,1,2.5,1),(1,2,1,3.0,2),(1,3,0,4.1,0),(1,4,1,2.8,1),
(2,5,1,2.0,1),(2,6,0,5.0,0),(2,7,1,3.2,1),(2,5,1,2.1,2),
(3,8,1,1.8,1),(3,9,1,2.0,2),(3,10,1,1.5,3),
(4,11,0,4.5,0),(4,12,1,3.0,1),(4,13,0,5.2,0),
(5,15,1,2.2,1),(5,16,1,1.9,2),(5,17,1,2.0,3),(5,15,1,1.8,4),
(6,1,1,3.0,1),(6,2,0,4.5,0),(6,3,1,3.2,1),
(7,18,0,5.0,0),(7,19,1,3.8,1),
(8,11,1,2.0,1),(8,12,1,1.5,2),(8,13,1,2.2,3),(8,14,0,4.0,0),
(9,1,1,1.8,1),(9,2,1,2.0,2),(9,3,1,1.2,3),(9,4,1,1.5,4),(9,1,1,1.0,5),
(10,15,1,2.5,1),(10,16,1,2.0,2),(10,17,0,3.5,0),(10,15,1,2.8,1),
(11,1,1,2.0,1),(11,2,0,4.0,0),(11,3,1,3.0,1),
(12,5,0,5.5,0),(12,6,0,6.0,0),(12,7,1,4.0,1),
(13,1,0,5.0,0),(13,2,0,6.0,0),
(14,11,1,2.0,1),(14,12,1,1.5,2),(14,13,1,2.0,3),
(15,8,1,2.5,1),(15,9,1,2.0,2),(15,10,0,4.0,0),
(16,15,1,1.5,1),(16,16,1,1.2,2),(16,17,1,1.0,3),(16,15,1,1.3,4),
(17,15,1,2.0,1),(17,16,1,1.8,2),(17,17,1,1.5,3),(17,15,1,2.0,4),(17,16,1,1.2,5),
(18,11,1,2.2,1),(18,12,0,4.0,0),(18,13,1,2.5,1),(18,14,1,2.0,2);

-- ==================== DML: UPDATE / DELETE samples ====================

UPDATE Child  SET BaselineSkillLevel = 'Intermediate' WHERE ChildID = 1;
UPDATE Parent SET Name = 'Sunita Sharma-Verma' WHERE ParentID = 1;

DELETE FROM Attempt WHERE AttemptID = 999;

-- ==================== DQL: Role-Specific SELECTs ====================

-- Parent login -> fetch their children (Netflix profile selector)
SELECT c.ChildID, c.Name AS ChildName,
       TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
       c.BaselineSkillLevel
FROM Child c
WHERE c.ParentID = 1
ORDER BY c.Name ASC;

-- Teacher login -> fetch assigned children via mapping table
SELECT c.ChildID, c.Name AS ChildName,
       TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
       p.Name AS ParentName
FROM Child c
JOIN Child_Educator ce ON c.ChildID = ce.ChildID
JOIN Parent p          ON c.ParentID = p.ParentID
WHERE ce.EducatorID = 1
ORDER BY c.Name;

-- Admin -> all users with display names
SELECT UserID, Username AS Email, Role,
    CASE
        WHEN Role = 'Parent'  THEN (SELECT Name FROM Parent   WHERE ParentID   = User.ParentID)
        WHEN Role = 'Teacher' THEN (SELECT Name FROM Educator WHERE EducatorID = User.EducatorID)
        WHEN Role = 'Child'   THEN (SELECT Name FROM Child    WHERE ChildID    = User.ChildID)
        ELSE 'System Admin'
    END AS DisplayName
FROM User;

-- ==================== JOINS ====================

-- INNER JOIN: children with their parents (Email pulled from User now)
SELECT c.ChildID, c.Name AS ChildName,
       TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
       p.ParentID, p.Name AS ParentName, u.Username AS ParentEmail
FROM Child c
INNER JOIN Parent p ON c.ParentID = p.ParentID
LEFT JOIN  User u   ON u.ParentID = p.ParentID AND u.Role = 'Parent'
ORDER BY p.Name, c.Name;

-- LEFT JOIN: all parents including those without children
SELECT p.ParentID, p.Name AS ParentName, u.Username AS Email,
       COUNT(c.ChildID) AS ChildCount
FROM Parent p
LEFT JOIN Child c ON p.ParentID = c.ParentID
LEFT JOIN User u  ON u.ParentID = p.ParentID AND u.Role = 'Parent'
GROUP BY p.ParentID, p.Name, u.Username;

-- Multi-table JOIN: full progress chain Child -> Session -> Lesson -> Attempt
SELECT c.Name AS ChildName, s.SessionDate, l.LessonTitle,
       COUNT(a.AttemptID) AS TotalAttempts,
       SUM(a.IsCorrect)   AS CorrectAnswers
FROM Child c
JOIN Session s ON c.ChildID  = s.ChildID
JOIN Lesson  l ON s.LessonID = l.LessonID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.Name, s.SessionDate, l.LessonTitle
ORDER BY s.SessionDate DESC;

-- ==================== GROUP BY / HAVING ====================

SELECT c.Name AS ChildName,
       COUNT(DISTINCT s.SessionID) AS TotalSessions,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
FROM Child c
JOIN Session s ON c.ChildID  = s.ChildID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name
HAVING COUNT(DISTINCT s.SessionID) > 3
ORDER BY AccuracyPercent DESC;

SELECT p.Name AS ParentName,
       COUNT(DISTINCT c.ChildID)   AS ChildCount,
       COUNT(DISTINCT s.SessionID) AS TotalFamilySessions
FROM Parent p
JOIN Child   c ON p.ParentID = c.ParentID
JOIN Session s ON c.ChildID  = s.ChildID
GROUP BY p.ParentID, p.Name
HAVING COUNT(DISTINCT s.SessionID) >= 2;

SELECT m.DifficultyLevel,
       COUNT(DISTINCT s.SessionID) AS SessionCount,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AvgAccuracy
FROM Module m
JOIN Lesson  l ON m.ModuleID  = l.ModuleID
JOIN Session s ON l.LessonID  = s.LessonID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY m.DifficultyLevel
HAVING COUNT(DISTINCT s.SessionID) > 0;

-- ==================== SUBQUERIES ====================

SELECT c.ChildID, c.Name, sub.AccuracyPercent
FROM Child c
JOIN (
    SELECT s.ChildID, ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
    FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
    GROUP BY s.ChildID
) sub ON c.ChildID = sub.ChildID
WHERE sub.AccuracyPercent < 50
ORDER BY sub.AccuracyPercent ASC;

SELECT c.ChildID, c.Name, sub.AccuracyPercent
FROM Child c
JOIN (
    SELECT s.ChildID, ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
    FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
    GROUP BY s.ChildID
) sub ON c.ChildID = sub.ChildID
WHERE sub.AccuracyPercent > 80
ORDER BY sub.AccuracyPercent DESC;

SELECT c.Name, COUNT(s.SessionID) AS SessionCount
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
GROUP BY c.ChildID, c.Name
HAVING COUNT(s.SessionID) > (
    SELECT AVG(cnt) FROM (
        SELECT COUNT(SessionID) AS cnt FROM Session GROUP BY ChildID
    ) AS avg_tbl
);

SELECT c.ChildID, c.Name, p.Name AS ParentName
FROM Child c
JOIN Parent p ON c.ParentID = p.ParentID
WHERE c.ChildID NOT IN (SELECT DISTINCT ChildID FROM Session);

-- ==================== FUNCTIONS ====================

SELECT
    COUNT(DISTINCT c.ChildID) AS TotalChildren,
    ROUND(AVG(TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE())), 1) AS AverageAge,
    MIN(TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE())) AS YoungestAge,
    MAX(TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE())) AS OldestAge,
    SUM(CASE WHEN c.BaselineSkillLevel = 'Beginner' THEN 1 ELSE 0 END) AS Beginners
FROM Child c;

SELECT
    ChildID,
    UPPER(Name) AS NameUpper,
    CONCAT('Student: ', Name, ' (Age ',
           TIMESTAMPDIFF(YEAR, DateOfBirth, CURDATE()), ')') AS FormattedLabel
FROM Child;

SELECT c.Name,
       MAX(s.SessionDate) AS LastActive,
       DATEDIFF(CURDATE(), MAX(s.SessionDate)) AS DaysSinceLastSession
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
GROUP BY c.ChildID, c.Name
ORDER BY DaysSinceLastSession ASC;

-- ==================== VIEWS ====================

CREATE OR REPLACE VIEW ProgressView AS
SELECT s.ChildID, ROUND(AVG(a.IsCorrect) * 100, 2) AS Accuracy
FROM Session s
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY s.ChildID;

CREATE OR REPLACE VIEW ParentChildProgressView AS
SELECT
    p.ParentID, p.Name AS ParentName, u.Username AS Email,
    c.ChildID, c.Name AS ChildName,
    TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
    COUNT(DISTINCT s.SessionID) AS TotalSessions,
    IFNULL(ROUND(AVG(a.IsCorrect) * 100, 2), 0) AS Accuracy
FROM Parent p
LEFT JOIN User u    ON u.ParentID = p.ParentID AND u.Role = 'Parent'
JOIN Child c        ON p.ParentID = c.ParentID
LEFT JOIN Session s ON c.ChildID  = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY p.ParentID, p.Name, u.Username, c.ChildID, c.Name, c.DateOfBirth;

CREATE OR REPLACE VIEW TeacherChildView AS
SELECT
    e.EducatorID, e.Name AS TeacherName,
    c.ChildID, c.Name AS ChildName,
    TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
    p.Name AS ParentName,
    COUNT(DISTINCT s.SessionID) AS TotalSessions,
    IFNULL(ROUND(AVG(a.IsCorrect) * 100, 2), 0) AS Accuracy
FROM Educator e
JOIN Child_Educator ce ON e.EducatorID = ce.EducatorID
JOIN Child c           ON ce.ChildID   = c.ChildID
JOIN Parent p          ON c.ParentID   = p.ParentID
LEFT JOIN Session s    ON c.ChildID    = s.ChildID
LEFT JOIN Attempt a    ON s.SessionID  = a.SessionID
GROUP BY e.EducatorID, e.Name, c.ChildID, c.Name, c.DateOfBirth, p.Name;

CREATE OR REPLACE VIEW ChildSessionSummary AS
SELECT
    c.ChildID, c.Name,
    COUNT(s.SessionID) AS SessionCount,
    IFNULL(SUM(s.Duration), 0) AS TotalMinutes,
    ROUND(IFNULL(SUM(s.Duration), 0) / 60, 1) AS TotalHours
FROM Child c
LEFT JOIN Session s ON c.ChildID = s.ChildID
GROUP BY c.ChildID, c.Name;

-- ProgressReport is now a derived view (no stored aggregates).
CREATE OR REPLACE VIEW ProgressReport AS
SELECT
    s.ChildID                                     AS ReportID,
    s.ChildID                                     AS ChildID,
    CURDATE()                                     AS GeneratedDate,
    COUNT(DISTINCT s.SessionID)                   AS TotalSessions,
    IFNULL(ROUND(SUM(a.IsCorrect) * 100.0
        / NULLIF(COUNT(a.AttemptID), 0), 2), 0)   AS AccuracyRate
FROM Session s
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY s.ChildID;

-- ==================== STORED PROCEDURES ====================

DELIMITER //

CREATE PROCEDURE GetChildProfilesByParent(IN p_ParentID INT)
BEGIN
    SELECT
        c.ChildID,
        c.Name AS ChildName,
        TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
        c.BaselineSkillLevel,
        IFNULL(pv.Accuracy, 0) AS Accuracy
    FROM Child c
    LEFT JOIN ProgressView pv ON c.ChildID = pv.ChildID
    WHERE c.ParentID = p_ParentID
    ORDER BY c.Name;
END //

CREATE PROCEDURE GetChildProgressReport(IN p_ChildID INT)
BEGIN
    DECLARE v_accuracy DECIMAL(5,2);
    DECLARE v_sessions INT;

    SELECT
        COUNT(DISTINCT s.SessionID),
        IFNULL(ROUND(SUM(a.IsCorrect) * 100.0
            / NULLIF(COUNT(a.AttemptID), 0), 2), 0)
    INTO v_sessions, v_accuracy
    FROM Session s
    LEFT JOIN Attempt a ON a.SessionID = s.SessionID
    WHERE s.ChildID = p_ChildID;

    -- ProgressReport is a VIEW; values are computed on demand, not stored.

    SELECT
        COALESCE(v_sessions, 0) AS TotalSessions,
        COALESCE(v_accuracy, 0) AS AverageAccuracy,
        COALESCE(v_accuracy, 0) AS OverallCompletion,
        (SELECT Name FROM Child WHERE ChildID = p_ChildID) AS ChildName,
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'SessionDate',     DATE_FORMAT(s.SessionDate, '%Y-%m-%d'),
                    'LessonTitle',     l.LessonTitle,
                    'ModuleName',      m.ModuleName,
                    'TotalAttempts',   (SELECT COUNT(*)              FROM Attempt WHERE SessionID = s.SessionID),
                    'CorrectAttempts', (SELECT COALESCE(SUM(IsCorrect),0) FROM Attempt WHERE SessionID = s.SessionID)
                )
            )
            FROM Session s
            JOIN Lesson l ON s.LessonID = l.LessonID
            JOIN Module m ON l.ModuleID = m.ModuleID
            WHERE s.ChildID = p_ChildID
        ) AS SessionHistoryJSON;
END //

CREATE PROCEDURE LoadDashboardStats(IN p_Role VARCHAR(20), IN p_RefID INT)
BEGIN
    IF p_Role = 'Parent' THEN
        SELECT COUNT(DISTINCT c.ChildID) AS ChildCount,
               COUNT(DISTINCT s.SessionID) AS SessionCount,
               IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0) AS OverallAccuracy
        FROM Child c
        LEFT JOIN Session s ON c.ChildID = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID = a.SessionID
        WHERE c.ParentID = p_RefID;
    ELSEIF p_Role = 'Teacher' THEN
        SELECT COUNT(DISTINCT ce.ChildID) AS ChildCount,
               COUNT(DISTINCT s.SessionID) AS SessionCount,
               IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0) AS OverallAccuracy
        FROM Child_Educator ce
        JOIN Child c        ON ce.ChildID   = c.ChildID
        LEFT JOIN Session s ON c.ChildID    = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID  = a.SessionID
        WHERE ce.EducatorID = p_RefID;
    END IF;
END //

DELIMITER ;

-- ==================== TRANSACTIONS ====================

-- COMMIT: atomically create parent + user + child (uses normalized schema)
START TRANSACTION;

INSERT INTO Parent (Name) VALUES ('Neha Gupta');
SET @new_parent_id = LAST_INSERT_ID();

INSERT INTO User (Username, Password, Role, ParentID)
VALUES ('neha@gmail.com', 'neha123', 'Parent', @new_parent_id);

INSERT INTO Child (ParentID, Name, DateOfBirth, BaselineSkillLevel)
VALUES (@new_parent_id, 'Rohan Gupta', '2020-01-15', 'Beginner');
SET @new_child_id = LAST_INSERT_ID();

INSERT INTO User (Username, Password, Role, ChildID)
VALUES (CONCAT('rohangupta', @new_parent_id), 'pass123', 'Child', @new_child_id);

COMMIT;

-- ROLLBACK demonstration
START TRANSACTION;
INSERT INTO Parent (Name) VALUES ('Test User');
SELECT * FROM Parent WHERE Name = 'Test User';
ROLLBACK;
SELECT * FROM Parent WHERE Name = 'Test User';

-- ==================== INDEXING ====================

CREATE INDEX idx_child_parent     ON Child(ParentID);
CREATE INDEX idx_session_child    ON Session(ChildID);
CREATE INDEX idx_session_date     ON Session(SessionDate);
CREATE INDEX idx_attempt_session  ON Attempt(SessionID);
CREATE INDEX idx_attempt_question ON Attempt(QuestionID);
CREATE INDEX idx_educator_school  ON Educator(SchoolID);
CREATE INDEX idx_user_role        ON User(Role);
