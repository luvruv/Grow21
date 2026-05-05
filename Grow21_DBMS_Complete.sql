-- GROW21 DBMS — Role-Based Child Progress Tracking System

CREATE DATABASE IF NOT EXISTS Grow21_DB;
USE Grow21_DB;

-- ==================== DDL: CREATE ====================

CREATE TABLE School (
    SchoolID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(150) NOT NULL,
    Location VARCHAR(200) NOT NULL
);

-- PK: ParentID | Candidate Key: Email
CREATE TABLE Parent (
    ParentID INT PRIMARY KEY AUTO_INCREMENT,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Phone VARCHAR(15) NULL
);

-- PK: EducatorID | FK: SchoolID → School | Candidate Key: Email
CREATE TABLE Educator (
    EducatorID INT PRIMARY KEY AUTO_INCREMENT,
    SchoolID INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL UNIQUE,
    FOREIGN KEY (SchoolID) REFERENCES School(SchoolID)
);

-- Centralized auth table — single login for all roles
-- PK: UserID | FK: ParentID → Parent, EducatorID → Educator | Candidate Key: Email
CREATE TABLE User (
    UserID INT PRIMARY KEY AUTO_INCREMENT,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Role ENUM('Admin', 'Parent', 'Teacher') NOT NULL,
    ParentID INT NULL,
    EducatorID INT NULL,
    FOREIGN KEY (ParentID) REFERENCES Parent(ParentID),
    FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID)
);

-- One Parent → Many Children
-- PK: ChildID | FK: ParentID → Parent | Candidate Key: Username
CREATE TABLE Child (
    ChildID INT PRIMARY KEY AUTO_INCREMENT,
    ParentID INT NOT NULL,
    Name VARCHAR(100) NOT NULL,
    Age INT NOT NULL,
    BaselineSkillLevel VARCHAR(50) NOT NULL DEFAULT 'Beginner',
    Username VARCHAR(100) UNIQUE,
    Password VARCHAR(255),
    FOREIGN KEY (ParentID) REFERENCES Parent(ParentID)
);

-- Many-to-Many mapping | Composite PK: (ChildID, EducatorID)
CREATE TABLE Child_Educator (
    ChildID INT NOT NULL,
    EducatorID INT NOT NULL,
    PRIMARY KEY (ChildID, EducatorID),
    FOREIGN KEY (ChildID) REFERENCES Child(ChildID),
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

-- PK: SessionID | FK: ChildID → Child, LessonID → Lesson
CREATE TABLE Session (
    SessionID INT PRIMARY KEY AUTO_INCREMENT,
    ChildID INT NOT NULL,
    LessonID INT NOT NULL,
    SessionDate DATE NOT NULL,
    Duration INT NOT NULL,
    CompletionPercentage INT DEFAULT 0,
    FOREIGN KEY (ChildID) REFERENCES Child(ChildID),
    FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID)
);

-- PK: AttemptID | FK: SessionID → Session
CREATE TABLE Attempt (
    AttemptID INT PRIMARY KEY AUTO_INCREMENT,
    SessionID INT NOT NULL,
    QuestionID INT NOT NULL,
    IsCorrect TINYINT(1) NOT NULL,
    ResponseTime DECIMAL(5,2) NOT NULL,
    StreakCount INT DEFAULT 0,
    FOREIGN KEY (SessionID) REFERENCES Session(SessionID)
);

-- PK: ReportID | FK: ChildID → Child
CREATE TABLE ProgressReport (
    ReportID INT PRIMARY KEY AUTO_INCREMENT,
    ChildID INT NOT NULL,
    GeneratedDate DATE NOT NULL,
    TotalSessions INT DEFAULT 0,
    AccuracyRate DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (ChildID) REFERENCES Child(ChildID)
);

-- ==================== DML: INSERT ====================

-- ==================== DML: INSERT ====================

INSERT INTO School (Name, Location) VALUES
('Delhi Public School', 'New Delhi'),
('Ryan International', 'Gurugram'),
('Sanskriti School', 'Chandigarh');

INSERT INTO Parent (Name, Email) VALUES
('Sunita Sharma', 'sunita@gmail.com'),
('Rakesh Mehta', 'rakesh@gmail.com'),
('Priya Singh', 'priya@gmail.com'),
('Amit Verma', 'amit@gmail.com'),
('Sneha Kapoor', 'sneha@gmail.com'),
('Rohit Das', 'rohit@gmail.com');

INSERT INTO Educator (SchoolID, Name, Email) VALUES
(1, 'Ananya Iyer', 'ananya@school.com'),
(2, 'Vikram Rao', 'vikram@school.com'),
(1, 'Deepa Nair', 'deepa@school.com');

INSERT INTO User (Email, Password, Role, ParentID, EducatorID) VALUES
('admin@grow21.com', 'admin123', 'Admin', NULL, NULL),
('sunita@gmail.com', 'pass123', 'Parent', 1, NULL),
('rakesh@gmail.com', 'pass123', 'Parent', 2, NULL),
('priya@gmail.com', 'pass123', 'Parent', 3, NULL),
('amit@gmail.com', 'pass123', 'Parent', 4, NULL),
('sneha@gmail.com', 'pass123', 'Parent', 5, NULL),
('rohit@gmail.com', 'pass123', 'Parent', 6, NULL),
('ananya@school.com', 'pass123', 'Teacher', NULL, 1),
('vikram@school.com', 'pass123', 'Teacher', NULL, 2),
('deepa@school.com', 'pass123', 'Teacher', NULL, 3);

INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel, Username, Password) VALUES
(1, 'Aryan Sharma', 8, 'Beginner', 'aryansharma1', 'pass123'),
(1, 'Myra Sharma', 7, 'Beginner', 'myrasharma7', 'pass123'),
(2, 'Riya Mehta', 10, 'Intermediate', 'riyamehta2', 'pass123'),
(2, 'Vivaan Mehta', 5, 'Beginner', 'vivaanmehta8', 'pass123'),
(3, 'Dev Singh', 7, 'Beginner', 'devsingh3', 'pass123'),
(3, 'Diya Singh', 8, 'Intermediate', 'diyasingh9', 'pass123'),
(4, 'Aarav Verma', 6, 'Beginner', 'aaravverma4', 'pass123'),
(4, 'Kabir Verma', 10, 'Advanced', 'kabirverma10', 'pass123'),
(5, 'Kavya Kapoor', 9, 'Intermediate', 'kavyakapoor5', 'pass123'),
(6, 'Ishaan Das', 11, 'Advanced', 'ishaandas6', 'pass123');

INSERT INTO Child_Educator (ChildID, EducatorID) VALUES
(1, 1), (2, 1), (3, 2), (5, 1), (7, 3), (8, 2), (9, 3), (10, 2);

INSERT INTO Module (ModuleName, Category, DifficultyLevel) VALUES
('Move and Play', 'Motor Skills', 'Easy'),
('Vocabulary', 'Language', 'Medium'),
('Puzzles', 'Cognitive', 'Hard');

INSERT INTO Lesson (ModuleID, LessonTitle, TargetSkill) VALUES
(1, 'Trace Lines', 'Fine Motor'),
(1, 'Draw Shapes', 'Fine Motor'),
(1, 'Free Draw', 'Creativity'),
(2, 'Image-based MCQ', 'Vocabulary Identification'),
(3, 'Color Sorting', 'Logic'),
(3, 'Memory Flip', 'Memory'),
(3, 'Drag and Match', 'Logic');

INSERT INTO Session (ChildID, LessonID, SessionDate, Duration, CompletionPercentage) VALUES
(1, 1, '2026-04-20', 15, 100), (1, 2, '2026-04-21', 20, 80), (1, 3, '2026-04-22', 12, 100),
(1, 4, '2026-04-23', 18, 60), (1, 5, '2026-04-24', 14, 100),
(2, 1, '2026-04-20', 10, 100), (2, 6, '2026-04-22', 15, 40),
(3, 4, '2026-04-21', 22, 100), (3, 7, '2026-04-23', 25, 90), (3, 1, '2026-04-25', 18, 100),
(5, 1, '2026-04-20', 12, 100), (5, 2, '2026-04-22', 16, 75),
(7, 1, '2026-04-21', 10, 100), (8, 4, '2026-04-22', 20, 100),
(9, 3, '2026-04-23', 18, 100), (9, 5, '2026-04-24', 22, 100),
(10, 5, '2026-04-20', 30, 80), (10, 4, '2026-04-22', 25, 100);

INSERT INTO Attempt (SessionID, QuestionID, IsCorrect, ResponseTime, StreakCount) VALUES
(1,1,1,2.5,1),(1,2,1,3.0,2),(1,3,0,4.1,0),(1,4,1,2.8,1),
(2,1,1,2.0,1),(2,2,0,5.0,0),(2,3,1,3.2,1),(2,4,1,2.1,2),
(3,1,1,1.8,1),(3,2,1,2.0,2),(3,3,1,1.5,3),
(4,1,0,4.5,0),(4,2,1,3.0,1),(4,3,0,5.2,0),
(5,1,1,2.2,1),(5,2,1,1.9,2),(5,3,1,2.0,3),(5,4,1,1.8,4),
(6,1,1,3.0,1),(6,2,0,4.5,0),(6,3,1,3.2,1),
(7,1,0,5.0,0),(7,2,1,3.8,1),
(8,1,1,2.0,1),(8,2,1,1.5,2),(8,3,1,2.2,3),(8,4,0,4.0,0),
(9,1,1,1.8,1),(9,2,1,2.0,2),(9,3,1,1.2,3),(9,4,1,1.5,4),(9,5,1,1.0,5),
(10,1,1,2.5,1),(10,2,1,2.0,2),(10,3,0,3.5,0),(10,4,1,2.8,1),
(11,1,1,2.0,1),(11,2,0,4.0,0),(11,3,1,3.0,1),
(12,1,0,5.5,0),(12,2,0,6.0,0),(12,3,1,4.0,1),
(13,1,0,5.0,0),(13,2,0,6.0,0),
(14,1,1,2.0,1),(14,2,1,1.5,2),(14,3,1,2.0,3),
(15,1,1,2.5,1),(15,2,1,2.0,2),(15,3,0,4.0,0),
(16,1,1,1.5,1),(16,2,1,1.2,2),(16,3,1,1.0,3),(16,4,1,1.3,4),
(17,1,1,2.0,1),(17,2,1,1.8,2),(17,3,1,1.5,3),(17,4,1,2.0,4),(17,5,1,1.2,5),
(18,1,1,2.2,1),(18,2,0,4.0,0),(18,3,1,2.5,1),(18,4,1,2.0,2);

INSERT INTO ProgressReport (ChildID, GeneratedDate, TotalSessions, AccuracyRate) VALUES
(1, '2026-04-25', 5, 72.22),
(3, '2026-04-25', 3, 84.62),
(10, '2026-04-25', 2, 100.00);

-- ==================== DML: UPDATE ====================

UPDATE Child SET BaselineSkillLevel = 'Intermediate' WHERE ChildID = 1;

UPDATE Parent SET Name = 'Sunita Sharma-Verma' WHERE ParentID = 1;

-- ==================== DML: DELETE ====================

DELETE FROM ProgressReport WHERE GeneratedDate < '2025-01-01';

DELETE FROM Attempt WHERE AttemptID = 999;

-- ==================== DQL: Role-Specific SELECTs ====================

-- Parent login → fetch their children (Netflix profile selector)
SELECT c.ChildID, c.Name AS ChildName, c.Age, c.BaselineSkillLevel
FROM Child c
WHERE c.ParentID = 1
ORDER BY c.Name ASC;

-- Teacher login → fetch assigned children via mapping table
SELECT c.ChildID, c.Name AS ChildName, c.Age, p.Name AS ParentName
FROM Child c
JOIN Child_Educator ce ON c.ChildID = ce.ChildID
JOIN Parent p ON c.ParentID = p.ParentID
WHERE ce.EducatorID = 1
ORDER BY c.Name;

-- Admin → all users with display names
SELECT UserID, Email, Role,
    CASE
        WHEN Role = 'Parent' THEN (SELECT Name FROM Parent WHERE ParentID = User.ParentID)
        WHEN Role = 'Teacher' THEN (SELECT Name FROM Educator WHERE EducatorID = User.EducatorID)
        ELSE 'System Admin'
    END AS DisplayName
FROM User;

-- ==================== JOINS ====================

-- INNER JOIN: children with their parents
SELECT c.ChildID, c.Name AS ChildName, c.Age,
       p.ParentID, p.Name AS ParentName, p.Email AS ParentEmail
FROM Child c
INNER JOIN Parent p ON c.ParentID = p.ParentID
ORDER BY p.Name, c.Name;

-- LEFT JOIN: all parents including those without children
SELECT p.ParentID, p.Name AS ParentName, p.Email,
       COUNT(c.ChildID) AS ChildCount
FROM Parent p
LEFT JOIN Child c ON p.ParentID = c.ParentID
GROUP BY p.ParentID, p.Name, p.Email;

-- Multi-table JOIN: full progress chain Child → Session → Lesson → Attempt
SELECT c.Name AS ChildName, s.SessionDate, l.LessonTitle,
       COUNT(a.AttemptID) AS TotalAttempts,
       SUM(a.IsCorrect) AS CorrectAnswers
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
JOIN Lesson l ON s.LessonID = l.LessonID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.Name, s.SessionDate, l.LessonTitle
ORDER BY s.SessionDate DESC;

-- ==================== GROUP BY / HAVING ====================

-- Average accuracy per child (only children with >3 sessions)
SELECT c.Name AS ChildName,
       COUNT(DISTINCT s.SessionID) AS TotalSessions,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name
HAVING COUNT(DISTINCT s.SessionID) > 3
ORDER BY AccuracyPercent DESC;

-- Parent-level summary: total sessions across all their children
SELECT p.Name AS ParentName,
       COUNT(DISTINCT c.ChildID) AS ChildCount,
       COUNT(DISTINCT s.SessionID) AS TotalFamilySessions
FROM Parent p
JOIN Child c ON p.ParentID = c.ParentID
JOIN Session s ON c.ChildID = s.ChildID
GROUP BY p.ParentID, p.Name
HAVING COUNT(DISTINCT s.SessionID) >= 2;

-- Module difficulty breakdown
SELECT m.DifficultyLevel,
       COUNT(DISTINCT s.SessionID) AS SessionCount,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AvgAccuracy
FROM Module m
JOIN Lesson l ON m.ModuleID = l.ModuleID
JOIN Session s ON l.LessonID = s.LessonID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY m.DifficultyLevel
HAVING COUNT(DISTINCT s.SessionID) > 0;

-- ==================== SUBQUERIES ====================

-- Weak student detection (accuracy below 50%)
SELECT c.ChildID, c.Name, sub.AccuracyPercent
FROM Child c
JOIN (
    SELECT s.ChildID, ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
    FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
    GROUP BY s.ChildID
) sub ON c.ChildID = sub.ChildID
WHERE sub.AccuracyPercent < 50
ORDER BY sub.AccuracyPercent ASC;

-- Top performers (above 80% accuracy)
SELECT c.ChildID, c.Name, sub.AccuracyPercent
FROM Child c
JOIN (
    SELECT s.ChildID, ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
    FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
    GROUP BY s.ChildID
) sub ON c.ChildID = sub.ChildID
WHERE sub.AccuracyPercent > 80
ORDER BY sub.AccuracyPercent DESC;

-- Children with more sessions than average
SELECT c.Name, COUNT(s.SessionID) AS SessionCount
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
GROUP BY c.ChildID, c.Name
HAVING COUNT(s.SessionID) > (
    SELECT AVG(cnt) FROM (
        SELECT COUNT(SessionID) AS cnt FROM Session GROUP BY ChildID
    ) AS avg_tbl
);

-- Children who have NOT completed any session
SELECT c.ChildID, c.Name, p.Name AS ParentName
FROM Child c
JOIN Parent p ON c.ParentID = p.ParentID
WHERE c.ChildID NOT IN (SELECT DISTINCT ChildID FROM Session);

-- ==================== FUNCTIONS: Aggregates + Scalars ====================

-- Aggregate functions
SELECT
    COUNT(DISTINCT c.ChildID) AS TotalChildren,
    ROUND(AVG(c.Age), 1) AS AverageAge,
    MIN(c.Age) AS YoungestAge,
    MAX(c.Age) AS OldestAge,
    SUM(CASE WHEN c.BaselineSkillLevel = 'Beginner' THEN 1 ELSE 0 END) AS Beginners
FROM Child c;

-- Scalar functions: UPPER, CONCAT, SUBSTRING
SELECT
    ChildID,
    UPPER(Name) AS NameUpper,
    CONCAT('Student: ', Name, ' (Age ', Age, ')') AS FormattedLabel,
    CONCAT(UPPER(LEFT(Name, 1)), LOWER(SUBSTRING(Name, 2))) AS ProperCase
FROM Child;

-- Date functions: recent activity tracking
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
    p.ParentID, p.Name AS ParentName, p.Email,
    c.ChildID, c.Name AS ChildName, c.Age,
    COUNT(DISTINCT s.SessionID) AS TotalSessions,
    IFNULL(ROUND(AVG(a.IsCorrect) * 100, 2), 0) AS Accuracy
FROM Parent p
JOIN Child c ON p.ParentID = c.ParentID
LEFT JOIN Session s ON c.ChildID = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY p.ParentID, p.Name, p.Email, c.ChildID, c.Name, c.Age;

-- Usage: SELECT * FROM ParentChildProgressView WHERE ParentID = 1;

CREATE OR REPLACE VIEW TeacherChildView AS
SELECT
    e.EducatorID, e.Name AS TeacherName,
    c.ChildID, c.Name AS ChildName, c.Age,
    p.Name AS ParentName,
    COUNT(DISTINCT s.SessionID) AS TotalSessions,
    IFNULL(ROUND(AVG(a.IsCorrect) * 100, 2), 0) AS Accuracy
FROM Educator e
JOIN Child_Educator ce ON e.EducatorID = ce.EducatorID
JOIN Child c ON ce.ChildID = c.ChildID
JOIN Parent p ON c.ParentID = p.ParentID
LEFT JOIN Session s ON c.ChildID = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY e.EducatorID, e.Name, c.ChildID, c.Name, c.Age, p.Name;

-- Usage: SELECT * FROM TeacherChildView WHERE EducatorID = 1;

CREATE OR REPLACE VIEW ChildSessionSummary AS
SELECT
    c.ChildID, c.Name,
    COUNT(s.SessionID) AS SessionCount,
    IFNULL(SUM(s.Duration), 0) AS TotalMinutes,
    ROUND(IFNULL(SUM(s.Duration), 0) / 60, 1) AS TotalHours
FROM Child c
LEFT JOIN Session s ON c.ChildID = s.ChildID
GROUP BY c.ChildID, c.Name;

-- ==================== STORED PROCEDURES ====================

DELIMITER //

CREATE PROCEDURE GetChildProfilesByParent(IN p_ParentID INT)
BEGIN
    SELECT c.ChildID, c.Name AS ChildName, c.Age, c.BaselineSkillLevel,
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
 
    -- 1. Calculate Summary Stats
    SELECT 
        COUNT(DISTINCT s.SessionID), 
        IFNULL(ROUND(SUM(a.IsCorrect) * 100.0 / NULLIF(COUNT(a.AttemptID), 0), 2), 0)
    INTO v_sessions, v_accuracy
    FROM Session s
    LEFT JOIN Attempt a ON a.SessionID = s.SessionID
    WHERE s.ChildID = p_ChildID;
 
    -- 2. Insert a new progress report record for history dynamically
    IF v_sessions > 0 THEN
        INSERT INTO ProgressReport (ChildID, GeneratedDate, TotalSessions, AccuracyRate)
        VALUES (p_ChildID, CURDATE(), v_sessions, COALESCE(v_accuracy, 0));
    END IF;
 
    -- 3. RETURN RESULT SET 1: Summary Stats (For Dashboard/Profile)
    -- Include SessionHistoryJSON inside Result Set 1
    SELECT 
        COALESCE(v_sessions, 0) AS TotalSessions, 
        COALESCE(v_accuracy, 0) AS AverageAccuracy,
        COALESCE(v_accuracy, 0) AS OverallCompletion,
        (SELECT Name FROM Child WHERE ChildID = p_ChildID) AS ChildName,
        (
            SELECT JSON_ARRAYAGG(
                JSON_OBJECT(
                    'SessionDate', DATE_FORMAT(s.SessionDate, '%Y-%m-%d'),
                    'LessonTitle', l.LessonTitle,
                    'ModuleName', m.ModuleName,
                    'TotalAttempts', (SELECT COUNT(*) FROM Attempt WHERE SessionID = s.SessionID),
                    'CorrectAttempts', (SELECT COALESCE(SUM(IsCorrect), 0) FROM Attempt WHERE SessionID = s.SessionID)
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
        JOIN Child c ON ce.ChildID = c.ChildID
        LEFT JOIN Session s ON c.ChildID = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID = a.SessionID
        WHERE ce.EducatorID = p_RefID;
    END IF;
END //

DELIMITER ;

-- ==================== TRANSACTIONS ====================

-- COMMIT: atomically create parent + user + child
START TRANSACTION;

INSERT INTO Parent (Name, Email) VALUES ('Neha Gupta', 'neha@gmail.com');
SET @new_parent_id = LAST_INSERT_ID();

INSERT INTO User (Email, Password, Role, ParentID)
VALUES ('neha@gmail.com', 'neha123', 'Parent', @new_parent_id);

INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel, Username)
VALUES (@new_parent_id, 'Rohan Gupta', 6, 'Beginner', CONCAT('rohangupta', @new_parent_id));

COMMIT;

-- ROLLBACK: discard test data
START TRANSACTION;
INSERT INTO Parent (Name, Email) VALUES ('Test User', 'test@fail.com');
SELECT * FROM Parent WHERE Email = 'test@fail.com';  -- exists during transaction
ROLLBACK;
SELECT * FROM Parent WHERE Email = 'test@fail.com';  -- empty — proves rollback

-- ==================== INDEXING ====================

CREATE INDEX idx_child_parent ON Child(ParentID);
CREATE INDEX idx_session_child ON Session(ChildID);
CREATE INDEX idx_session_date ON Session(SessionDate);
CREATE INDEX idx_attempt_session ON Attempt(SessionID);
CREATE INDEX idx_educator_school ON Educator(SchoolID);

-- ==================== ANALYTICAL QUERIES ====================

-- Weak student detection (below 50% accuracy)
SELECT c.ChildID, c.Name AS ChildName, p.Name AS ParentName,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent,
       COUNT(DISTINCT s.SessionID) AS Sessions
FROM Child c
JOIN Parent p ON c.ParentID = p.ParentID
JOIN Session s ON c.ChildID = s.ChildID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name, p.Name
HAVING AccuracyPercent < 50
ORDER BY AccuracyPercent ASC;

-- Top performer identification (above 80%)
SELECT c.ChildID, c.Name, c.Age,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent,
       MAX(a.StreakCount) AS BestStreak
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name, c.Age
HAVING AccuracyPercent > 80
ORDER BY AccuracyPercent DESC;

-- Recent activity tracking (last 7 days)
SELECT c.Name AS ChildName, s.SessionDate,
       l.LessonTitle, s.Duration AS Minutes,
       COUNT(a.AttemptID) AS Attempts,
       ROUND(AVG(a.IsCorrect)*100, 2) AS Accuracy
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
JOIN Lesson l ON s.LessonID = l.LessonID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
WHERE s.SessionDate >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY c.Name, s.SessionDate, l.LessonTitle, s.Duration
ORDER BY s.SessionDate DESC;

-- Netflix profile selector query (used by mobile app after parent login)
SELECT c.ChildID, c.Name AS ChildName, c.Age, c.BaselineSkillLevel,
       p.ParentID, p.Name AS ParentName, p.Email,
       IFNULL(pv.Accuracy, 0) AS AvgAccuracy
FROM Child c
LEFT JOIN Parent p ON c.ParentID = p.ParentID
LEFT JOIN ProgressView pv ON c.ChildID = pv.ChildID
WHERE c.ParentID = 1
ORDER BY c.Name ASC;
