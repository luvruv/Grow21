-- =========================================================================
-- GROW21 — Cloud Database Initialization Script (BCNF, Aiven MySQL)
-- =========================================================================
-- Domain   : Learning platform for children with Trisomy 21
-- Target   : Aiven MySQL — schema "Grow21"
-- Run via  : node backend/apply_sql.js
-- Idempotent: drops every object in reverse dependency order before recreate
-- =========================================================================
-- Design highlights
--   * Authentication uses an ISA (subtype) hierarchy: User is the supertype;
--     Parent / Educator / Child are 1:1 subtype tables whose PK IS User.UserID.
--     This eliminates the previous nullable polymorphic FKs on User.
--   * No derived attributes are stored. Age is computed from DateOfBirth.
--     Session completion percentage is computed from Attempt rows in views.
--   * Question is a real entity; Attempt.QuestionID is a real FK.
--   * ProgressReport is a derived view (always fresh).
--   * Feedback captures teacher reviews per child / session.
--   * SystemAudit logs sensitive events via a trigger.
--   * Cross-relation invariants that FKs cannot express are enforced by
--     triggers (e.g., an Attempt's Question must belong to its Session's Lesson).
-- =========================================================================

-- ==================== TEARDOWN ====================
SET FOREIGN_KEY_CHECKS = 0;

DROP TRIGGER IF EXISTS trg_child_validate_dob;
DROP TRIGGER IF EXISTS trg_audit_user_insert;
DROP TRIGGER IF EXISTS trg_validate_attempt_lesson;

DROP PROCEDURE IF EXISTS GetChildProfilesByParent;
DROP PROCEDURE IF EXISTS GetChildProgressReport;
DROP PROCEDURE IF EXISTS LoadDashboardStats;
DROP PROCEDURE IF EXISTS RegisterParent;
DROP PROCEDURE IF EXISTS RegisterChild;
DROP PROCEDURE IF EXISTS RecordAttempt;
DROP PROCEDURE IF EXISTS SubmitFeedback;

DROP VIEW IF EXISTS ProgressReport;
DROP VIEW IF EXISTS ProgressView;
DROP VIEW IF EXISTS ParentChildProgressView;
DROP VIEW IF EXISTS TeacherChildView;
DROP VIEW IF EXISTS ChildSessionSummary;
DROP VIEW IF EXISTS WeakStudents;
DROP VIEW IF EXISTS TopPerformers;

DROP TABLE IF EXISTS SystemAudit;
DROP TABLE IF EXISTS Feedback;
DROP TABLE IF EXISTS Attempt;
DROP TABLE IF EXISTS Question;
DROP TABLE IF EXISTS Session;
DROP TABLE IF EXISTS Lesson;
DROP TABLE IF EXISTS Module;
DROP TABLE IF EXISTS Child_Educator;
DROP TABLE IF EXISTS Child;
DROP TABLE IF EXISTS Educator;
DROP TABLE IF EXISTS Parent;
DROP TABLE IF EXISTS `User`;
DROP TABLE IF EXISTS School;  -- defensive: removes leftover from older schema

SET FOREIGN_KEY_CHECKS = 1;

-- ==================== DDL — TABLES ====================

-- Authentication supertype: a single row per actor of any role.
-- Username is the canonical login identifier (email for adults, handle for kids).
CREATE TABLE `User` (
    UserID      INT PRIMARY KEY AUTO_INCREMENT,
    Username    VARCHAR(100) NOT NULL UNIQUE,
    Password    VARCHAR(255) NOT NULL,
    Role        ENUM('Admin','Parent','Teacher','Child') NOT NULL,
    CreatedAt   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LastLoginAt TIMESTAMP NULL
);

-- Parent profile: 1:1 with User. ParentID = the user's UserID.
CREATE TABLE Parent (
    ParentID INT PRIMARY KEY,
    Name     VARCHAR(100) NOT NULL,
    Phone    VARCHAR(15)  NULL,
    CONSTRAINT fk_parent_user
        FOREIGN KEY (ParentID) REFERENCES `User`(UserID)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Educator profile: 1:1 with User. EducatorID = the user's UserID.
CREATE TABLE Educator (
    EducatorID INT PRIMARY KEY,
    Name       VARCHAR(100) NOT NULL,
    CONSTRAINT fk_educator_user
        FOREIGN KEY (EducatorID) REFERENCES `User`(UserID)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Child profile: 1:1 with User. ChildID = the user's UserID.
CREATE TABLE Child (
    ChildID            INT PRIMARY KEY,
    ParentID           INT NOT NULL,
    Name               VARCHAR(100) NOT NULL,
    DateOfBirth        DATE NOT NULL,
    BaselineSkillLevel ENUM('Beginner','Intermediate','Advanced') NOT NULL DEFAULT 'Beginner',
    CONSTRAINT fk_child_user
        FOREIGN KEY (ChildID) REFERENCES `User`(UserID)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_child_parent
        FOREIGN KEY (ParentID) REFERENCES Parent(ParentID)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- M:N: which children each educator monitors.
CREATE TABLE Child_Educator (
    ChildID    INT NOT NULL,
    EducatorID INT NOT NULL,
    AssignedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ChildID, EducatorID),
    CONSTRAINT fk_ce_child
        FOREIGN KEY (ChildID) REFERENCES Child(ChildID) ON DELETE CASCADE,
    CONSTRAINT fk_ce_educator
        FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID) ON DELETE CASCADE
);

CREATE TABLE Module (
    ModuleID        INT PRIMARY KEY AUTO_INCREMENT,
    ModuleName      VARCHAR(100) NOT NULL,
    Category        VARCHAR(100) NOT NULL,
    DifficultyLevel ENUM('Easy','Medium','Hard') NOT NULL
);

CREATE TABLE Lesson (
    LessonID    INT PRIMARY KEY AUTO_INCREMENT,
    ModuleID    INT NOT NULL,
    LessonTitle VARCHAR(200) NOT NULL,
    TargetSkill VARCHAR(100) NOT NULL,
    CONSTRAINT fk_lesson_module
        FOREIGN KEY (ModuleID) REFERENCES Module(ModuleID) ON DELETE CASCADE
);

-- Question entity makes Attempt.QuestionID a real FK (was a phantom column before).
CREATE TABLE Question (
    QuestionID    INT PRIMARY KEY AUTO_INCREMENT,
    LessonID      INT NOT NULL,
    QuestionText  VARCHAR(255) NOT NULL,
    CorrectAnswer VARCHAR(100) NULL,
    CONSTRAINT fk_question_lesson
        FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID) ON DELETE CASCADE
);

-- Mobile app writes Session rows. CompletionPercentage is NOT stored — it is
-- a derived metric computed from Attempts (see view ChildSessionSummary).
CREATE TABLE Session (
    SessionID   INT PRIMARY KEY AUTO_INCREMENT,
    ChildID     INT NOT NULL,
    LessonID    INT NOT NULL,
    SessionDate DATE NOT NULL DEFAULT (CURDATE()),
    Duration    INT  NOT NULL,                       -- minutes
    IsCompleted BOOLEAN NOT NULL DEFAULT FALSE,
    StartedAt   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_child
        FOREIGN KEY (ChildID) REFERENCES Child(ChildID) ON DELETE CASCADE,
    CONSTRAINT fk_session_lesson
        FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID) ON DELETE RESTRICT,
    CONSTRAINT chk_session_duration CHECK (Duration >= 0)
);

-- One Attempt per question shown during a Session.
-- StreakCount is dropped — it is a window-function calculation on (Session, AttemptedAt).
CREATE TABLE Attempt (
    AttemptID    INT PRIMARY KEY AUTO_INCREMENT,
    SessionID    INT NOT NULL,
    QuestionID   INT NOT NULL,
    IsCorrect    BOOLEAN NOT NULL,
    ResponseTime DECIMAL(5,2) NOT NULL,
    AttemptedAt  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempt_session
        FOREIGN KEY (SessionID) REFERENCES Session(SessionID) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_question
        FOREIGN KEY (QuestionID) REFERENCES Question(QuestionID) ON DELETE RESTRICT,
    CONSTRAINT chk_attempt_response_time CHECK (ResponseTime > 0)
);

-- Teachers leave qualitative feedback / numeric ratings on a child or a specific session.
CREATE TABLE Feedback (
    FeedbackID INT PRIMARY KEY AUTO_INCREMENT,
    EducatorID INT NOT NULL,
    ChildID    INT NOT NULL,
    SessionID  INT NULL,
    Rating     TINYINT NOT NULL,
    Comment    TEXT NULL,
    CreatedAt  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_educator
        FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_child
        FOREIGN KEY (ChildID) REFERENCES Child(ChildID) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_session
        FOREIGN KEY (SessionID) REFERENCES Session(SessionID) ON DELETE SET NULL,
    CONSTRAINT chk_feedback_rating CHECK (Rating BETWEEN 1 AND 5)
);

-- Append-only event log written by triggers.
CREATE TABLE SystemAudit (
    AuditID    INT PRIMARY KEY AUTO_INCREMENT,
    EventType  VARCHAR(50) NOT NULL,
    EventData  VARCHAR(500) NULL,
    OccurredAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==================== INDEXES ====================
-- Indexes on every FK column and on attributes that drive frequent filters.
CREATE INDEX idx_user_role          ON `User`(Role);
CREATE INDEX idx_user_created       ON `User`(CreatedAt);
CREATE INDEX idx_child_parent       ON Child(ParentID);
CREATE INDEX idx_child_dob          ON Child(DateOfBirth);
CREATE INDEX idx_session_child      ON Session(ChildID);
CREATE INDEX idx_session_lesson     ON Session(LessonID);
CREATE INDEX idx_session_date       ON Session(SessionDate);
CREATE INDEX idx_attempt_session    ON Attempt(SessionID);
CREATE INDEX idx_attempt_question   ON Attempt(QuestionID);
CREATE INDEX idx_question_lesson    ON Question(LessonID);
CREATE INDEX idx_feedback_child     ON Feedback(ChildID);
CREATE INDEX idx_feedback_educator  ON Feedback(EducatorID);
CREATE INDEX idx_audit_event_time   ON SystemAudit(EventType, OccurredAt);

-- ==================== TRIGGERS ====================

DELIMITER //

-- 1) DateOfBirth must be in the past (CHECK can't use CURDATE() — non-deterministic).
CREATE TRIGGER trg_child_validate_dob
BEFORE INSERT ON Child
FOR EACH ROW
BEGIN
    IF NEW.DateOfBirth >= CURDATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Child.DateOfBirth must be in the past';
    END IF;
END //

-- 2) Audit every new account creation.
CREATE TRIGGER trg_audit_user_insert
AFTER INSERT ON `User`
FOR EACH ROW
BEGIN
    INSERT INTO SystemAudit (EventType, EventData)
    VALUES ('USER_CREATED',
            CONCAT('UserID=', NEW.UserID, ' Role=', NEW.Role, ' Username=', NEW.Username));
END //

-- 3) Cross-relation invariant: an Attempt's Question must belong to the Session's Lesson.
--    FKs alone cannot express this — both sides reference Lesson independently.
CREATE TRIGGER trg_validate_attempt_lesson
BEFORE INSERT ON Attempt
FOR EACH ROW
BEGIN
    DECLARE v_session_lesson  INT;
    DECLARE v_question_lesson INT;
    SELECT LessonID INTO v_session_lesson  FROM Session  WHERE SessionID  = NEW.SessionID;
    SELECT LessonID INTO v_question_lesson FROM Question WHERE QuestionID = NEW.QuestionID;
    IF v_session_lesson <> v_question_lesson THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Attempt.Question must belong to the Session''s Lesson';
    END IF;
END //

-- ==================== STORED PROCEDURES ====================

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
        SELECT COUNT(DISTINCT c.ChildID)            AS ChildCount,
               COUNT(DISTINCT s.SessionID)          AS SessionCount,
               IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0) AS OverallAccuracy
        FROM Child c
        LEFT JOIN Session s ON c.ChildID = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID = a.SessionID
        WHERE c.ParentID = p_RefID;
    ELSEIF p_Role = 'Teacher' THEN
        SELECT COUNT(DISTINCT ce.ChildID)           AS ChildCount,
               COUNT(DISTINCT s.SessionID)          AS SessionCount,
               IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0) AS OverallAccuracy
        FROM Child_Educator ce
        JOIN Child c        ON ce.ChildID   = c.ChildID
        LEFT JOIN Session s ON c.ChildID    = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID  = a.SessionID
        WHERE ce.EducatorID = p_RefID;
    END IF;
END //

-- Atomic registration: User + Parent profile in one transaction.
CREATE PROCEDURE RegisterParent(
    IN  p_Username VARCHAR(100),
    IN  p_Password VARCHAR(255),
    IN  p_Name     VARCHAR(100),
    IN  p_Phone    VARCHAR(15),
    OUT p_UserID   INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
        INSERT INTO `User` (Username, Password, Role)
            VALUES (p_Username, p_Password, 'Parent');
        SET p_UserID = LAST_INSERT_ID();
        INSERT INTO Parent (ParentID, Name, Phone)
            VALUES (p_UserID, p_Name, p_Phone);
    COMMIT;
END //

-- Atomic registration: User + Child profile linked to an existing Parent.
CREATE PROCEDURE RegisterChild(
    IN  p_ParentID INT,
    IN  p_Username VARCHAR(100),
    IN  p_Password VARCHAR(255),
    IN  p_Name     VARCHAR(100),
    IN  p_DOB      DATE,
    IN  p_Skill    VARCHAR(20),
    OUT p_ChildID  INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN ROLLBACK; RESIGNAL; END;
    START TRANSACTION;
        INSERT INTO `User` (Username, Password, Role)
            VALUES (p_Username, p_Password, 'Child');
        SET p_ChildID = LAST_INSERT_ID();
        INSERT INTO Child (ChildID, ParentID, Name, DateOfBirth, BaselineSkillLevel)
            VALUES (p_ChildID, p_ParentID, p_Name, p_DOB, p_Skill);
    COMMIT;
END //

CREATE PROCEDURE RecordAttempt(
    IN p_SessionID    INT,
    IN p_QuestionID   INT,
    IN p_IsCorrect    BOOLEAN,
    IN p_ResponseTime DECIMAL(5,2)
)
BEGIN
    INSERT INTO Attempt (SessionID, QuestionID, IsCorrect, ResponseTime)
    VALUES (p_SessionID, p_QuestionID, p_IsCorrect, p_ResponseTime);
END //

CREATE PROCEDURE SubmitFeedback(
    IN p_EducatorID INT,
    IN p_ChildID    INT,
    IN p_SessionID  INT,
    IN p_Rating     TINYINT,
    IN p_Comment    TEXT
)
BEGIN
    INSERT INTO Feedback (EducatorID, ChildID, SessionID, Rating, Comment)
    VALUES (p_EducatorID, p_ChildID, p_SessionID, p_Rating, p_Comment);
END //

DELIMITER ;

-- ==================== VIEWS ====================

-- Per-child average accuracy across all attempts.
CREATE OR REPLACE VIEW ProgressView AS
SELECT s.ChildID,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS Accuracy
FROM Session s
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY s.ChildID;

-- ProgressReport is a derived view — values are always fresh.
CREATE OR REPLACE VIEW ProgressReport AS
SELECT
    s.ChildID                                    AS ReportID,
    s.ChildID                                    AS ChildID,
    CURDATE()                                    AS GeneratedDate,
    COUNT(DISTINCT s.SessionID)                  AS TotalSessions,
    IFNULL(ROUND(SUM(a.IsCorrect) * 100.0
        / NULLIF(COUNT(a.AttemptID), 0), 2), 0)  AS AccuracyRate
FROM Session s
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY s.ChildID;

-- Parent dashboard: every parent → each child → session count + accuracy.
CREATE OR REPLACE VIEW ParentChildProgressView AS
SELECT
    p.ParentID,
    p.Name AS ParentName,
    u.Username AS Email,
    c.ChildID,
    c.Name AS ChildName,
    TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
    COUNT(DISTINCT s.SessionID) AS TotalSessions,
    IFNULL(ROUND(AVG(a.IsCorrect) * 100, 2), 0) AS Accuracy
FROM Parent p
JOIN `User` u       ON u.UserID = p.ParentID
JOIN Child c        ON p.ParentID = c.ParentID
LEFT JOIN Session s ON c.ChildID  = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY p.ParentID, p.Name, u.Username, c.ChildID, c.Name, c.DateOfBirth;

-- Teacher dashboard: every educator → each assigned child → session count + accuracy.
CREATE OR REPLACE VIEW TeacherChildView AS
SELECT
    e.EducatorID,
    e.Name AS TeacherName,
    c.ChildID,
    c.Name AS ChildName,
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

-- Session-level summary: completion percentage is computed here, not stored.
CREATE OR REPLACE VIEW ChildSessionSummary AS
SELECT
    c.ChildID,
    c.Name AS ChildName,
    s.SessionID,
    s.SessionDate,
    s.Duration,
    CASE
        WHEN s.IsCompleted = TRUE THEN 100
        WHEN COUNT(a.AttemptID) > 0
            THEN ROUND(SUM(a.IsCorrect) * 100.0 / COUNT(a.AttemptID))
        ELSE 0
    END AS CompletionPercentage
FROM Child c
JOIN Session s      ON c.ChildID  = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name, s.SessionID, s.SessionDate, s.Duration, s.IsCompleted;

-- Demographic-style alert views.
CREATE OR REPLACE VIEW WeakStudents AS
SELECT c.ChildID, c.Name AS ChildName, p.Name AS ParentName,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
FROM Child c
JOIN Parent p   ON c.ParentID  = p.ParentID
JOIN Session s  ON c.ChildID   = s.ChildID
JOIN Attempt a  ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name, p.Name
HAVING AccuracyPercent < 50;

CREATE OR REPLACE VIEW TopPerformers AS
SELECT c.ChildID, c.Name AS ChildName, p.Name AS ParentName,
       ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
FROM Child c
JOIN Parent p   ON c.ParentID  = p.ParentID
JOIN Session s  ON c.ChildID   = s.ChildID
JOIN Attempt a  ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name, p.Name
HAVING AccuracyPercent >= 80;

-- ==================== SEED DATA ====================

-- Users first (auth supertype). Explicit UserIDs so subtype rows can reference them.
INSERT INTO `User` (UserID, Username, Password, Role) VALUES
(1,  'admin@grow21.com',  'admin123', 'Admin'),
(2,  'sunita@gmail.com',  'pass123',  'Parent'),
(3,  'rakesh@gmail.com',  'pass123',  'Parent'),
(4,  'priya@gmail.com',   'pass123',  'Parent'),
(5,  'amit@gmail.com',    'pass123',  'Parent'),
(6,  'sneha@gmail.com',   'pass123',  'Parent'),
(7,  'rohit@gmail.com',   'pass123',  'Parent'),
(8,  'ananya@school.com', 'pass123',  'Teacher'),
(9,  'vikram@school.com', 'pass123',  'Teacher'),
(10, 'deepa@school.com',  'pass123',  'Teacher'),
(11, 'aryansharma1', 'pass123', 'Child'),
(12, 'myrasharma7',  'pass123', 'Child'),
(13, 'riyamehta2',   'pass123', 'Child'),
(14, 'vivaanmehta8', 'pass123', 'Child'),
(15, 'devsingh3',    'pass123', 'Child'),
(16, 'diyasingh9',   'pass123', 'Child'),
(17, 'aaravverma4',  'pass123', 'Child'),
(18, 'kabirverma10', 'pass123', 'Child'),
(19, 'kavyakapoor5', 'pass123', 'Child'),
(20, 'ishaandas6',   'pass123', 'Child');

INSERT INTO Parent (ParentID, Name, Phone) VALUES
(2, 'Sunita Sharma', '9810000001'),
(3, 'Rakesh Mehta',  '9810000002'),
(4, 'Priya Singh',   '9810000003'),
(5, 'Amit Verma',    '9810000004'),
(6, 'Sneha Kapoor',  '9810000005'),
(7, 'Rohit Das',     '9810000006');

INSERT INTO Educator (EducatorID, Name) VALUES
(8,  'Ananya Iyer'),
(9,  'Vikram Rao'),
(10, 'Deepa Nair');

INSERT INTO Child (ChildID, ParentID, Name, DateOfBirth, BaselineSkillLevel) VALUES
(11, 2, 'Aryan Sharma', '2018-01-15', 'Beginner'),
(12, 2, 'Myra Sharma',  '2019-03-10', 'Beginner'),
(13, 3, 'Riya Mehta',   '2016-07-22', 'Intermediate'),
(14, 3, 'Vivaan Mehta', '2021-02-08', 'Beginner'),
(15, 4, 'Dev Singh',    '2019-04-30', 'Beginner'),
(16, 4, 'Diya Singh',   '2018-09-05', 'Intermediate'),
(17, 5, 'Aarav Verma',  '2020-06-18', 'Beginner'),
(18, 5, 'Kabir Verma',  '2016-11-12', 'Advanced'),
(19, 6, 'Kavya Kapoor', '2017-08-25', 'Intermediate'),
(20, 7, 'Ishaan Das',   '2015-12-03', 'Advanced');

INSERT INTO Child_Educator (ChildID, EducatorID) VALUES
(11, 8), (12, 8), (13, 9), (15, 8), (17, 10), (18, 9), (19, 10), (20, 9);

INSERT INTO Module (ModuleName, Category, DifficultyLevel) VALUES
('Move and Play', 'Motor Skills', 'Easy'),
('Vocabulary',    'Language',     'Medium'),
('Puzzles',       'Cognitive',    'Hard');

INSERT INTO Lesson (ModuleID, LessonTitle, TargetSkill) VALUES
(1, 'Trace Lines',     'Fine Motor'),
(1, 'Draw Shapes',     'Fine Motor'),
(1, 'Free Draw',       'Creativity'),
(2, 'Image-based MCQ', 'Vocabulary Identification'),
(3, 'Color Sorting',   'Logic'),
(3, 'Memory Flip',     'Memory'),
(3, 'Drag and Match',  'Logic');

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

INSERT INTO Session (ChildID, LessonID, SessionDate, Duration, IsCompleted) VALUES
(11, 1, '2026-04-20', 15, TRUE),  (11, 2, '2026-04-21', 20, FALSE), (11, 3, '2026-04-22', 12, TRUE),
(11, 4, '2026-04-23', 18, FALSE), (11, 5, '2026-04-24', 14, TRUE),
(12, 1, '2026-04-20', 10, TRUE),  (12, 6, '2026-04-22', 15, FALSE),
(13, 4, '2026-04-21', 22, TRUE),  (13, 7, '2026-04-23', 25, FALSE), (13, 1, '2026-04-25', 18, TRUE),
(15, 1, '2026-04-20', 12, TRUE),  (15, 2, '2026-04-22', 16, FALSE),
(17, 1, '2026-04-21', 10, TRUE),  (18, 4, '2026-04-22', 20, TRUE),
(19, 3, '2026-04-23', 18, TRUE),  (19, 5, '2026-04-24', 22, TRUE),
(20, 5, '2026-04-20', 30, FALSE), (20, 4, '2026-04-22', 25, TRUE);

-- Attempts. The trg_validate_attempt_lesson trigger enforces that each Attempt's
-- Question belongs to the same Lesson as its Session.
INSERT INTO Attempt (SessionID, QuestionID, IsCorrect, ResponseTime) VALUES
(1,1,1,2.5),(1,2,1,3.0),(1,3,0,4.1),(1,4,1,2.8),
(2,5,1,2.0),(2,6,0,5.0),(2,7,1,3.2),(2,5,1,2.1),
(3,8,1,1.8),(3,9,1,2.0),(3,10,1,1.5),
(4,11,0,4.5),(4,12,1,3.0),(4,13,0,5.2),
(5,15,1,2.2),(5,16,1,1.9),(5,17,1,2.0),(5,15,1,1.8),
(6,1,1,3.0),(6,2,0,4.5),(6,3,1,3.2),
(7,18,0,5.0),(7,19,1,3.8),
(8,11,1,2.0),(8,12,1,1.5),(8,13,1,2.2),(8,14,0,4.0),
(9,23,1,1.8),(9,24,1,2.0),(9,25,1,1.2),(9,23,1,1.5),(9,24,1,1.0),
(10,1,1,2.5),(10,2,1,2.0),(10,3,0,3.5),(10,4,1,2.8),
(11,1,1,2.0),(11,2,0,4.0),(11,3,1,3.0),
(12,5,0,5.5),(12,6,0,6.0),(12,7,1,4.0),
(13,1,0,5.0),(13,2,0,6.0),
(14,11,1,2.0),(14,12,1,1.5),(14,13,1,2.0),
(15,8,1,2.5),(15,9,1,2.0),(15,10,0,4.0),
(16,15,1,1.5),(16,16,1,1.2),(16,17,1,1.0),(16,15,1,1.3),
(17,15,1,2.0),(17,16,1,1.8),(17,17,1,1.5),(17,15,1,2.0),(17,16,1,1.2),
(18,11,1,2.2),(18,12,0,4.0),(18,13,1,2.5),(18,14,1,2.0);

-- Sample teacher feedback so the new entity has data.
INSERT INTO Feedback (EducatorID, ChildID, SessionID, Rating, Comment) VALUES
(8, 11, 1, 5, 'Excellent fine-motor progress. Encourage longer sessions.'),
(8, 12, 6, 4, 'Strong start. Will revisit Memory Flip next week.'),
(9, 13, 8, 5, 'Vocabulary identification is consistent and quick.'),
(9, 18, 14, 5, 'Top performer in MCQs. Move to advanced bank.'),
(10, 17, 13, 3, 'Engaged but distracted; suggest shorter sessions.');

-- Demo transaction: COMMIT a new parent + child via the registration procedures.
START TRANSACTION;
CALL RegisterParent('neha@gmail.com', 'neha123', 'Neha Gupta', '9810000099', @new_parent_id);
CALL RegisterChild(@new_parent_id, CONCAT('rohangupta', @new_parent_id), 'pass123', 'Rohan Gupta', '2020-01-15', 'Beginner', @new_child_id);
COMMIT;

-- Demo transaction: ROLLBACK an experimental insert.
START TRANSACTION;
INSERT INTO `User` (Username, Password, Role) VALUES ('rollbacktest', 'x', 'Parent');
ROLLBACK;
