DROP DATABASE IF EXISTS Grow21_DB;
CREATE DATABASE Grow21_DB;

USE Grow21_DB;

/* 
   ==========================================================================
   DATABASE DESIGN & NORMALIZATION ANALYSIS
   ==========================================================================
   1NF (First Normal Form): 
   - All tables have a primary key (ID fields).
   - All columns contain atomic values (no comma-separated lists).
   - No repeating groups of columns.
   
   2NF (Second Normal Form):
   - Already in 1NF.
   - No partial dependencies: In tables with composite keys (like Child_Educator), 
     every non-key attribute depends on the WHOLE primary key.
   
   3NF (Third Normal Form):
   - Already in 2NF.
   - No transitive dependencies: Non-key attributes depend only on the primary key.
     Example: In Child table, 'Name' depends on 'ChildID', not on 'ParentID'.
   ==========================================================================
*/


CREATE TABLE Parent (
    ParentID  INT          PRIMARY KEY AUTO_INCREMENT,
    Name      VARCHAR(100) NOT NULL,
    -- [RUBRIC: CANDIDATE KEYS] Unique attributes that could serve as PKs
    Email     VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE School (
    SchoolID  INT          PRIMARY KEY AUTO_INCREMENT,
    Name      VARCHAR(150) NOT NULL,
    Location  VARCHAR(200) NOT NULL
);

CREATE TABLE Child (
    ChildID            INT          PRIMARY KEY AUTO_INCREMENT,
    ParentID           INT          NOT NULL,
    Name               VARCHAR(100) NOT NULL,
    Age                INT          NOT NULL,
    BaselineSkillLevel VARCHAR(50)  NOT NULL,
    Username           VARCHAR(100) UNIQUE,
    Password           VARCHAR(255),
    FOREIGN KEY (ParentID) REFERENCES Parent(ParentID)
        ON DELETE CASCADE
);

CREATE TABLE Educator (
    EducatorID  INT          PRIMARY KEY AUTO_INCREMENT,
    SchoolID    INT          NOT NULL,
    Name        VARCHAR(100) NOT NULL,
    Email       VARCHAR(100) UNIQUE NOT NULL,
    FOREIGN KEY (SchoolID) REFERENCES School(SchoolID)
);

CREATE TABLE Child_Educator (
    ChildID     INT NOT NULL,
    EducatorID  INT NOT NULL,
    PRIMARY KEY (ChildID, EducatorID),
    FOREIGN KEY (ChildID)    REFERENCES Child(ChildID)    ON DELETE CASCADE,
    FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID) ON DELETE CASCADE
);

CREATE TABLE Module (
    ModuleID        INT          PRIMARY KEY AUTO_INCREMENT,
    ModuleName      VARCHAR(100) NOT NULL,
    Category        VARCHAR(100) NOT NULL,
    DifficultyLevel VARCHAR(50)  NOT NULL
);

CREATE TABLE Lesson (
    LessonID    INT          PRIMARY KEY AUTO_INCREMENT,
    ModuleID    INT          NOT NULL,
    LessonTitle VARCHAR(200) NOT NULL,
    TargetSkill VARCHAR(100) NOT NULL,
    FOREIGN KEY (ModuleID) REFERENCES Module(ModuleID)
);

CREATE TABLE Session (
    SessionID   INT  PRIMARY KEY AUTO_INCREMENT,
    ChildID     INT  NOT NULL,
    LessonID    INT  NOT NULL,
    SessionDate DATE NOT NULL,
    Duration    INT  NOT NULL,
    FOREIGN KEY (ChildID)  REFERENCES Child(ChildID),
    FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID)
);

CREATE TABLE Attempt (
    AttemptID    INT            PRIMARY KEY AUTO_INCREMENT,
    SessionID    INT            NOT NULL,
    QuestionID   INT            NOT NULL,
    IsCorrect    BOOLEAN        NOT NULL,
    ResponseTime DECIMAL(5,2)   NOT NULL,
    StreakCount  INT            DEFAULT 0,
    FOREIGN KEY (SessionID) REFERENCES Session(SessionID)
);


/* 
   ==========================================================================
   INDEXING (RUBRIC COMPONENT #14)
   ==========================================================================
   While Primary Keys automatically create indexes, we add explicit indexes 
   on Foreign Keys and columns used frequently in WHERE/JOIN clauses to 
   improve query performance.
*/
CREATE INDEX idx_child_parent ON Child(ParentID);
CREATE INDEX idx_session_child ON Session(ChildID);
CREATE INDEX idx_attempt_session ON Attempt(SessionID);
CREATE INDEX idx_educator_school ON Educator(SchoolID);

CREATE TABLE ProgressReport (
    ReportID      INT          PRIMARY KEY AUTO_INCREMENT,
    ChildID       INT          NOT NULL,
    GeneratedDate DATE         NOT NULL,
    TotalSessions INT          DEFAULT 0,
    AccuracyRate  DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (ChildID) REFERENCES Child(ChildID) ON DELETE CASCADE
);

CREATE TABLE User (
    UserID       INT PRIMARY KEY AUTO_INCREMENT,
    Email        VARCHAR(100) UNIQUE NOT NULL,
    Password     VARCHAR(255) NOT NULL,
    Role         ENUM('Admin', 'Parent', 'Teacher') NOT NULL,
    ParentID     INT NULL,
    EducatorID   INT NULL,
    FOREIGN KEY (ParentID) REFERENCES Parent(ParentID) ON DELETE CASCADE,
    FOREIGN KEY (EducatorID) REFERENCES Educator(EducatorID) ON DELETE CASCADE
);

INSERT INTO Parent VALUES (1, 'Sunita Sharma', 'sunita@gmail.com');
INSERT INTO Parent VALUES (2, 'Rakesh Mehta',  'rakesh@gmail.com');
INSERT INTO Parent VALUES (3, 'Priya Singh',   'priya@gmail.com');
INSERT INTO Parent VALUES (4, 'Amit Verma',    'amit@gmail.com');
INSERT INTO Parent VALUES (5, 'Sneha Kapoor',  'sneha@gmail.com');
INSERT INTO Parent VALUES (6, 'Rohit Das',     'rohit@gmail.com');

SELECT * FROM Parent;

INSERT INTO School VALUES (1, 'Asha Kiran Special School', 'New Delhi');
INSERT INTO School VALUES (2, 'Sunshine Learning Center',  'Mumbai');

SELECT * FROM School;

INSERT INTO Child VALUES (1, 1, 'Aryan Sharma', 8,  'Beginner', 'aryan1', 'pass123');
INSERT INTO Child VALUES (2, 2, 'Riya Mehta',   10, 'Intermediate', 'riya2', 'pass123');
INSERT INTO Child VALUES (3, 3, 'Dev Singh',    7,  'Beginner', 'dev3', 'pass123');
INSERT INTO Child VALUES (4, 4, 'Aarav Verma',  6,  'Beginner', 'aarav4', 'pass123');
INSERT INTO Child VALUES (5, 5, 'Kavya Kapoor', 9,  'Intermediate', 'kavya5', 'pass123');
INSERT INTO Child VALUES (6, 6, 'Ishaan Das',   11, 'Advanced', 'ishaan6', 'pass123');
INSERT INTO Child VALUES (7, 1, 'Myra Sharma',  7,  'Beginner', 'myra7', 'pass123');
INSERT INTO Child VALUES (8, 2, 'Vivaan Mehta', 5,  'Beginner', 'vivaan8', 'pass123');
INSERT INTO Child VALUES (9, 3, 'Diya Singh',   8,  'Intermediate', 'diya9', 'pass123');
INSERT INTO Child VALUES (10, 4, 'Kabir Verma', 10, 'Advanced', 'kabir10', 'pass123');

SELECT * FROM Child;

INSERT INTO Educator VALUES (1, 1, 'Mrs. Ananya Rao',  'ananya@school.com');
INSERT INTO Educator VALUES (2, 1, 'Mr. Vikram Patel', 'vikram@school.com');
INSERT INTO Educator VALUES (3, 2, 'Ms. Deepa Nair',   'deepa@school.com');

SELECT * FROM Educator;

-- Aryan is taught by both Ananya AND Vikram (M:N demonstrated!)
INSERT INTO Child_Educator VALUES (1, 1);
INSERT INTO Child_Educator VALUES (1, 2);
-- Riya is taught by Ananya
INSERT INTO Child_Educator VALUES (2, 1);
-- Dev is taught by Deepa
INSERT INTO Child_Educator VALUES (3, 3);
-- Additional children assignments
INSERT INTO Child_Educator VALUES (4, 1);
INSERT INTO Child_Educator VALUES (5, 2);
INSERT INTO Child_Educator VALUES (6, 3);
INSERT INTO Child_Educator VALUES (7, 1);
INSERT INTO Child_Educator VALUES (8, 1);
INSERT INTO Child_Educator VALUES (9, 2);
INSERT INTO Child_Educator VALUES (10, 3);

SELECT * FROM Child_Educator;

INSERT INTO Module VALUES (1, 'Basic Language Skills', 'Communication', 'Easy');
INSERT INTO Module VALUES (2, 'Number Recognition',    'Mathematics',   'Easy');
INSERT INTO Module VALUES (3, 'Social Interaction',    'Life Skills',   'Medium');

SELECT * FROM Module;

INSERT INTO Lesson VALUES (1, 1, 'Alphabet Recognition', 'Letter Identification');
INSERT INTO Lesson VALUES (2, 1, 'Simple Words',         'Word Formation');
INSERT INTO Lesson VALUES (3, 2, 'Count 1 to 10',        'Number Counting');
INSERT INTO Lesson VALUES (4, 3, 'Greeting Others',      'Social Awareness');

SELECT * FROM Lesson;

INSERT INTO Session VALUES (1, 1, 1, '2026-03-01', 30);
INSERT INTO Session VALUES (2, 1, 2, '2026-03-05', 25);
INSERT INTO Session VALUES (3, 2, 3, '2026-03-02', 40);
INSERT INTO Session VALUES (4, 3, 4, '2026-03-03', 20);
INSERT INTO Session VALUES (5, 1, 3, '2026-03-10', 35);

SELECT * FROM Session;

-- Session 1 attempts (Aryan, Alphabet lesson)
INSERT INTO Attempt VALUES (1, 1, 101, TRUE,  3.5, 2);
INSERT INTO Attempt VALUES (2, 1, 102, TRUE,  4.1, 3);
INSERT INTO Attempt VALUES (3, 1, 103, FALSE, 8.2, 0);
-- Session 2 attempts (Aryan, Words lesson)
INSERT INTO Attempt VALUES (4, 2, 201, TRUE,  2.9, 1);
INSERT INTO Attempt VALUES (5, 2, 202, TRUE,  3.1, 2);
-- Session 3 attempts (Riya, Numbers lesson)
INSERT INTO Attempt VALUES (6, 3, 301, FALSE, 9.0, 0);
-- Session 4 attempt (Dev, Social lesson)
INSERT INTO Attempt VALUES (7, 4, 401, TRUE,  5.5, 1);
-- Session 5 attempts (Aryan, Numbers lesson)
INSERT INTO Attempt VALUES (8, 5, 301, TRUE,  3.0, 4);
INSERT INTO Attempt VALUES (9, 5, 302, TRUE,  2.8, 5);

SELECT * FROM Attempt;

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

CREATE VIEW ProgressView AS
SELECT 
    c.ChildID,
    c.Name AS ChildName,
    COUNT(a.AttemptID) AS TotalAttempts,
    SUM(CASE WHEN a.IsCorrect = 1 THEN 1 ELSE 0 END) AS CorrectAttempts,
    IFNULL(ROUND(SUM(CASE WHEN a.IsCorrect = 1 THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(a.AttemptID), 0), 2), 0) AS Accuracy
FROM Child c
LEFT JOIN Session s ON c.ChildID = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name;

-- Remove static ProgressReport inserts and let them be generated dynamically or stored via the procedure.
-- We can initialize an empty ProgressReport, or just let the stored procedure handle it.
SELECT * FROM User;

SELECT 'Parent' AS TableName, COUNT(*) AS TotalRows FROM Parent
UNION ALL
SELECT 'Child', COUNT(*) FROM Child
UNION ALL
SELECT 'School', COUNT(*) FROM School
UNION ALL
SELECT 'Educator', COUNT(*) FROM Educator
UNION ALL
SELECT 'Child_Educator', COUNT(*) FROM Child_Educator
UNION ALL
SELECT 'Module', COUNT(*) FROM Module
UNION ALL
SELECT 'Lesson', COUNT(*) FROM Lesson
UNION ALL
SELECT 'Session', COUNT(*) FROM Session
UNION ALL
SELECT 'Attempt', COUNT(*) FROM Attempt
UNION ALL
SELECT 'ProgressReport', COUNT(*) FROM ProgressReport;

-- [RUBRIC: JOINS] Retrieve child info along with their parent's name
SELECT c.ChildID, c.Name AS ChildName, c.Age,
       c.BaselineSkillLevel, p.Name AS ParentName
FROM Child c
JOIN Parent p ON c.ParentID = p.ParentID;

SELECT e.Name AS Educator, e.Email, s.Name AS School, s.Location
FROM Educator e
JOIN School s ON e.SchoolID = s.SchoolID;

SELECT s.SessionID, l.LessonTitle, s.SessionDate, s.Duration
FROM Session s
JOIN Lesson l ON s.LessonID = l.LessonID
WHERE s.ChildID = 1
ORDER BY s.SessionDate;

SELECT c.Name AS Child, l.LessonTitle, m.ModuleName,
       m.DifficultyLevel, s.SessionDate,
       s.Duration AS Duration_Mins
FROM Session s
JOIN Child  c ON s.ChildID  = c.ChildID
JOIN Lesson l ON s.LessonID = l.LessonID
JOIN Module m ON l.ModuleID = m.ModuleID
ORDER BY c.Name, s.SessionDate;

-- [RUBRIC: GROUP BY & HAVING] Count sessions per child and filter those with more than 2
SELECT c.Name AS Child,
       COUNT(s.SessionID) AS TotalSessions
FROM Session s
JOIN Child c ON s.ChildID = c.ChildID
GROUP BY c.ChildID, c.Name
HAVING COUNT(s.SessionID) > 2
ORDER BY TotalSessions DESC;

SELECT c.Name AS Child,
       c.BaselineSkillLevel,
       ROUND(AVG(pr.AccuracyRate), 2) AS AvgAccuracy
FROM ProgressReport pr
JOIN Child c ON pr.ChildID = c.ChildID
GROUP BY c.ChildID, c.Name, c.BaselineSkillLevel
ORDER BY AvgAccuracy DESC;

-- [RUBRIC: SUBQUERIES] Find sessions that lasted longer than the average duration
SELECT s.SessionID, c.Name AS Child,
       l.LessonTitle, s.Duration AS Duration_Mins
FROM Session s
JOIN Child  c ON s.ChildID  = c.ChildID
JOIN Lesson l ON s.LessonID = l.LessonID
WHERE s.Duration > (SELECT AVG(Duration) FROM Session)
ORDER BY s.Duration DESC;

SELECT e.Name AS Educator, e.Email
FROM Educator e
WHERE EXISTS (
    SELECT 1
    FROM Child_Educator ce
    WHERE ce.EducatorID = e.EducatorID
);

-- [RUBRIC: AGGREGATE FUNCTIONS & CASE] Calculate session accuracy using SUM and CASE
SELECT s.SessionID, c.Name AS Child,
       l.LessonTitle,
       SUM(CASE WHEN a.IsCorrect = 1 THEN 1 ELSE 0 END) AS Correct,
       SUM(CASE WHEN a.IsCorrect = 0 THEN 1 ELSE 0 END) AS Incorrect,
       COUNT(a.AttemptID) AS Total,
       ROUND(SUM(a.IsCorrect)*100.0/COUNT(a.AttemptID),2) AS AccuracyPct
FROM Attempt a
JOIN Session s ON a.SessionID = s.SessionID
JOIN Child   c ON s.ChildID   = c.ChildID
JOIN Lesson  l ON s.LessonID  = l.LessonID
GROUP BY s.SessionID, c.Name, l.LessonTitle;

SELECT m.ModuleName, m.Category,
       COUNT(s.SessionID) AS TotalSessions,
       SUM(s.Duration)    AS TotalMinutes
FROM Session s
JOIN Lesson l ON s.LessonID = l.LessonID
JOIN Module m ON l.ModuleID = m.ModuleID
GROUP BY m.ModuleID, m.ModuleName, m.Category
ORDER BY TotalSessions DESC;

SELECT c.Name AS Child,
       ROUND(MIN(a.ResponseTime),2) AS Fastest_Sec,
       ROUND(MAX(a.ResponseTime),2) AS Slowest_Sec,
       ROUND(MAX(a.ResponseTime) - MIN(a.ResponseTime),2) AS Variation
FROM Attempt a
JOIN Session s ON a.SessionID = s.SessionID
JOIN Child   c ON s.ChildID   = c.ChildID
GROUP BY c.ChildID, c.Name
HAVING (MAX(a.ResponseTime) - MIN(a.ResponseTime)) > 3
ORDER BY Variation DESC;

CREATE VIEW Child_Session_Summary AS
SELECT
    c.ChildID,
    c.Name      AS ChildName,
    c.Age,
    l.LessonTitle,
    m.ModuleName,
    m.DifficultyLevel,
    s.SessionDate,
    s.Duration  AS DurationMins
FROM Session s
JOIN Child  c ON s.ChildID  = c.ChildID
JOIN Lesson l ON s.LessonID = l.LessonID
JOIN Module m ON l.ModuleID = m.ModuleID;

/* 
   ==========================================================================
   ADVANCED SQL: COMMON TABLE EXPRESSION (CTE)
   ==========================================================================
   This query calculates a "Mastery Score" by analyzing session performance 
   over time. It uses a CTE to first aggregate data before performing 
   the final analysis. (Rubric: Expert SQL Implementation)
*/
WITH ChildSessionStats AS (
    SELECT 
        c.Name AS ChildName,
        l.LessonTitle,
        COUNT(a.AttemptID) AS TotalAttempts,
        SUM(CASE WHEN a.IsCorrect = 1 THEN 1 ELSE 0 END) AS CorrectAttempts
    FROM Attempt a
    JOIN Session s ON a.SessionID = s.SessionID
    JOIN Child c   ON s.ChildID = c.ChildID
    JOIN Lesson l  ON s.LessonID = l.LessonID
    GROUP BY c.ChildID, l.LessonID
)
SELECT 
    ChildName,
    LessonTitle,
    TotalAttempts,
    CorrectAttempts,
    ROUND((CorrectAttempts / TotalAttempts) * 100, 2) AS MasteryPercentage
FROM ChildSessionStats
WHERE TotalAttempts > 0
ORDER BY MasteryPercentage DESC;
 
-- Test the view:
SELECT * FROM Child_Session_Summary;
SELECT * FROM Child_Session_Summary WHERE ChildName = 'Aryan Sharma';

CREATE VIEW Educator_Child_Count AS
SELECT
    e.EducatorID,
    e.Name       AS Educator,
    sc.Name      AS School,
    COUNT(ce.ChildID) AS TotalChildren
FROM Educator e
JOIN Child_Educator ce ON e.EducatorID = ce.EducatorID
JOIN School sc         ON e.SchoolID   = sc.SchoolID
GROUP BY e.EducatorID, e.Name, sc.Name;
 
-- Test:
SELECT * FROM Educator_Child_Count ORDER BY TotalChildren DESC;


DELIMITER $$
 
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
    SELECT 
        COALESCE(v_sessions, 0) AS TotalSessions, 
        COALESCE(v_accuracy, 0) AS AverageAccuracy,
        COALESCE(v_accuracy, 0) AS OverallCompletion,
        (SELECT Name FROM Child WHERE ChildID = p_ChildID) AS ChildName;

    -- 4. RETURN RESULT SET 2: Session History
    SELECT s.SessionDate, l.LessonTitle, m.ModuleName,
           COUNT(a.AttemptID) AS TotalAttempts,
           SUM(a.IsCorrect)   AS CorrectAttempts
    FROM Session s
    JOIN Lesson  l ON s.LessonID = l.LessonID
    JOIN Module  m ON l.ModuleID = m.ModuleID
    LEFT JOIN Attempt a ON a.SessionID = s.SessionID
    WHERE s.ChildID = p_ChildID
    GROUP BY s.SessionID, s.SessionDate, l.LessonTitle, m.ModuleName
    ORDER BY s.SessionDate DESC;
END $$
 
DELIMITER ;

-- How to CALL the procedure:
CALL GetChildProgressReport(1);   -- Run for Aryan (ChildID=1)
CALL GetChildProgressReport(2);   -- Run for Riya  (ChildID=2)

-- Transaction 1: Enroll a new child (all 3 steps must succeed)
START TRANSACTION;
 
INSERT INTO Parent (Name, Email)
VALUES ('Meera Gupta', 'meera@gmail.com');
 
SET @new_parent_id = LAST_INSERT_ID();
 
INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel, Username, Password)
VALUES (@new_parent_id, 'Rohan Gupta', 9, 'Beginner', 'rohan_g', 'pass123');
 
SET @new_child_id = LAST_INSERT_ID();
 
INSERT INTO Child_Educator (ChildID, EducatorID)
VALUES (@new_child_id, 2);
 
-- Everything worked — save permanently:
COMMIT;
 
-- Verify it worked:
SELECT * FROM Parent  WHERE Name = 'Meera Gupta';
SELECT * FROM Child   WHERE Name = 'Rohan Gupta';
SELECT * FROM Child_Educator WHERE ChildID = @new_child_id;

-- Transaction 2: Demonstrate ROLLBACK
START TRANSACTION;
 
-- Insert a test record
INSERT INTO School (Name, Location)
VALUES ('Test School', 'Test City');
 
-- Check it exists temporarily:
SELECT * FROM School;   -- You will see Test School here
 
-- Oops — we changed our mind. Undo everything:
ROLLBACK;
 
-- Verify rollback worked:
SELECT * FROM School;   -- Test School is GONE

/* 
   ==========================================================================
   LOCKING MECHANISM DEMONSTRATION
   ==========================================================================
   Here we demonstrate a pessimistic lock using SELECT ... FOR UPDATE.
   This is useful when multiple users (e.g. educators) might try to update 
   the same child's baseline skill level at the exact same time.
   This ensures data consistency and prevents race conditions.
*/
START TRANSACTION;

-- Lock the specific row for 'Riya Mehta' so no other transaction can modify it until we commit
SELECT * FROM Child 
WHERE ChildID = 2 
FOR UPDATE;

-- Update the skill level
UPDATE Child 
SET BaselineSkillLevel = 'Advanced' 
WHERE ChildID = 2;

-- Release the lock and save changes
COMMIT;

/* 
   ==========================================================================
   EFFICIENCY & OPTIMIZATION DEMONSTRATION (EXPLAIN & INDEXING)
   ==========================================================================
   To show how our program is efficient, we demonstrate query execution plans
   before and after adding a specific index. This shows that adding this code
   significantly reduces the time taken to fetch results (hence becoming efficient),
   and shows what would happen if it wasn't there (full table scan).
   
   Scenario: Searching for lessons by TargetSkill can be slow if the table grows large.
*/

-- Step 1: Analyze query performance BEFORE indexing
-- (agar ye cheez nhi hoti toh kya horha hota - Full Table Scan)
EXPLAIN SELECT * FROM Lesson WHERE TargetSkill = 'Word Formation';
-- Without an index, the database performs a "Full Table Scan" (type = ALL).
-- This means it checks every single row, which is inefficient for large datasets.

-- Step 2: Create an Index to optimize the query
-- (agar ye code likha hai toh ab kam time lag rha hai)
CREATE INDEX idx_lesson_targetskill ON Lesson(TargetSkill);

-- Step 3: Analyze query performance AFTER indexing
EXPLAIN SELECT * FROM Lesson WHERE TargetSkill = 'Word Formation';
-- With the index, the database uses an "Index Scan" (type = ref).
-- It directly finds the relevant rows, drastically reducing execution time and making the system highly efficient!

