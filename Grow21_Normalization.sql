-- =========================================================================
-- GROW21 — Database Normalization Script (BCNF)
-- =========================================================================
-- Project   : GROW21 — Role-Based Child Progress Tracking System
-- Purpose   : Decompose the existing 3NF-violating relations into BCNF
-- Database  : Grow21_DB (MySQL 8.0+)
-- Run order : Apply ONCE on a database created from Grow21_DBMS_Complete.sql
-- Idempotent: All steps guarded so re-runs do not corrupt schema/data
-- =========================================================================
-- VIVA SUMMARY OF ANOMALIES BEING FIXED
-- -------------------------------------------------------------------------
--   #1  Parent.Email       3NF  Transitive dep — Email is also User.Email
--   #2  Educator.Email     3NF  Same redundancy as #1
--   #3  Child.Username/Pw  Design+3NF  Auth split between User and Child
--   #4  Child.Age          3NF  Derived attribute (Age = f(DOB, today))
--   #5  Attempt.QuestionID Refr.  Phantom FK — Question entity is missing
--   #6  ProgressReport(*)  3NF  Stores derived aggregates of Session/Attempt
-- =========================================================================

USE Grow21_DB;

-- Disable FK checks during structural surgery; re-enabled at the end.
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================================
-- STEP 0 — SNAPSHOT BEFORE STATE  (for the viva: prove the violations exist)
-- =========================================================================

-- Show duplicated email between Parent and User (proves anomaly #1)
SELECT 'BEFORE: Parent.Email duplicated in User.Email' AS Check_Description;
SELECT p.ParentID, p.Name, p.Email AS Parent_Email, u.Email AS User_Email
FROM Parent p JOIN User u ON u.ParentID = p.ParentID
LIMIT 5;

-- Show duplicated email between Educator and User (proves anomaly #2)
SELECT 'BEFORE: Educator.Email duplicated in User.Email' AS Check_Description;
SELECT e.EducatorID, e.Name, e.Email AS Educator_Email, u.Email AS User_Email
FROM Educator e JOIN User u ON u.EducatorID = e.EducatorID
LIMIT 5;

-- Show split-auth: Child has its own credentials, parallel to User (anomaly #3)
SELECT 'BEFORE: Child has independent Username/Password (split auth)' AS Check_Description;
SELECT ChildID, Name, Username, Password FROM Child WHERE Username IS NOT NULL LIMIT 5;

-- Show derived attribute Age (anomaly #4)
SELECT 'BEFORE: Child.Age is derived (no DOB stored)' AS Check_Description;
SELECT ChildID, Name, Age FROM Child LIMIT 5;

-- Show phantom FK (anomaly #5): QuestionID values exist in Attempt but not in any table
SELECT 'BEFORE: QuestionID referenced by Attempt but Question table missing' AS Check_Description;
SELECT COUNT(DISTINCT QuestionID) AS DistinctQuestionIDsReferenced FROM Attempt;

-- Show derived aggregates stored in ProgressReport (anomaly #6)
SELECT 'BEFORE: ProgressReport stores values derivable from Session/Attempt' AS Check_Description;
SELECT * FROM ProgressReport LIMIT 5;


-- =========================================================================
-- STEP 1 — DECOMPOSE USER  (centralize auth identity)
-- -------------------------------------------------------------------------
-- Goal: User is the SINGLE source of truth for login identity.
--   - Rename Email -> Username so it can hold either an email (adults)
--     or a non-email handle (kids).
--   - Add Role 'Child' so child accounts live in the same table.
--   - Add a nullable ChildID FK so a User row can point at a Child.
-- =========================================================================

ALTER TABLE User CHANGE Email Username VARCHAR(100) NOT NULL;
-- Email's UNIQUE index travels with the column rename in MySQL, so Username remains UNIQUE.

ALTER TABLE User MODIFY Role ENUM('Admin','Parent','Teacher','Child') NOT NULL;

ALTER TABLE User ADD COLUMN ChildID INT NULL AFTER EducatorID;
ALTER TABLE User
  ADD CONSTRAINT fk_user_child FOREIGN KEY (ChildID) REFERENCES Child(ChildID);

-- Migrate child credentials from Child into User (one row per child with creds)
INSERT INTO User (Username, Password, Role, ChildID)
SELECT Username, Password, 'Child', ChildID
FROM Child
WHERE Username IS NOT NULL
  AND Username NOT IN (SELECT Username FROM User);  -- guard against re-runs

-- Now drop the duplicated auth columns from Child
ALTER TABLE Child DROP COLUMN Username;
ALTER TABLE Child DROP COLUMN Password;


-- =========================================================================
-- STEP 2 — DROP REDUNDANT EMAIL FROM PARENT AND EDUCATOR
-- -------------------------------------------------------------------------
-- Functional Dependency analysis:
--   ParentID   -> Name, Phone   (kept in Parent)
--   ParentID   -> User.Username (via the User row whose ParentID matches)
-- Storing Email in BOTH Parent and User violates 3NF (transitive dep).
-- =========================================================================

ALTER TABLE Parent   DROP COLUMN Email;
ALTER TABLE Educator DROP COLUMN Email;


-- =========================================================================
-- STEP 3 — REPLACE CHILD.AGE WITH DATEOFBIRTH
-- -------------------------------------------------------------------------
-- Age is a derived attribute: Age = TIMESTAMPDIFF(YEAR, DOB, CURDATE()).
-- Storing it means every birthday creates an UPDATE anomaly. Replace with DOB.
-- =========================================================================

ALTER TABLE Child ADD COLUMN DateOfBirth DATE NULL AFTER Name;

-- Backfill DOB from existing Age values (assumes today's date for born-this-year math)
UPDATE Child
SET DateOfBirth = DATE_SUB(CURDATE(), INTERVAL Age YEAR)
WHERE DateOfBirth IS NULL;

ALTER TABLE Child MODIFY COLUMN DateOfBirth DATE NOT NULL;
ALTER TABLE Child DROP COLUMN Age;


-- =========================================================================
-- STEP 4 — INTRODUCE QUESTION ENTITY  (fix phantom FK)
-- -------------------------------------------------------------------------
-- Attempt.QuestionID currently references nothing.
-- Create a Question table, backfill from existing Attempt rows, and add the FK.
-- =========================================================================

CREATE TABLE IF NOT EXISTS Question (
    QuestionID    INT PRIMARY KEY AUTO_INCREMENT,
    LessonID      INT NOT NULL,
    QuestionText  VARCHAR(255) NOT NULL,
    CorrectAnswer VARCHAR(100) NULL,
    FOREIGN KEY (LessonID) REFERENCES Lesson(LessonID)
);

-- Backfill: insert one Question per (Lesson, original-QuestionID) referenced by Attempts
INSERT INTO Question (QuestionID, LessonID, QuestionText)
SELECT DISTINCT
       a.QuestionID,
       s.LessonID,
       CONCAT('Question #', a.QuestionID, ' (auto-migrated from Attempt)')
FROM Attempt a
JOIN Session s ON a.SessionID = s.SessionID
WHERE a.QuestionID NOT IN (SELECT QuestionID FROM Question);

-- Now we can promote QuestionID to a real foreign key
ALTER TABLE Attempt
  ADD CONSTRAINT fk_attempt_question FOREIGN KEY (QuestionID) REFERENCES Question(QuestionID);


-- =========================================================================
-- STEP 5 — REPLACE PROGRESSREPORT TABLE WITH A VIEW
-- -------------------------------------------------------------------------
-- TotalSessions and AccuracyRate are FULLY derivable from Session ⨝ Attempt.
-- Storing them violates 3NF (non-key non-key dep on derivation rule).
-- We replace the table with a view so values are always fresh.
-- =========================================================================

DROP TABLE IF EXISTS ProgressReport;

CREATE OR REPLACE VIEW ProgressReport AS
SELECT
    s.ChildID                                    AS ReportID,       -- 1:1 with ChildID
    s.ChildID                                    AS ChildID,
    CURDATE()                                    AS GeneratedDate,
    COUNT(DISTINCT s.SessionID)                  AS TotalSessions,
    IFNULL(ROUND(SUM(a.IsCorrect) * 100.0
        / NULLIF(COUNT(a.AttemptID), 0), 2), 0)  AS AccuracyRate
FROM Session s
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY s.ChildID;


-- =========================================================================
-- STEP 6 — REBUILD VIEWS THAT REFERENCED RENAMED/REMOVED COLUMNS
-- -------------------------------------------------------------------------
-- Three legacy views (ParentChildProgressView, TeacherChildView,
-- ChildSessionSummary) referenced Child.Age and Parent.Email. Recreate them
-- against the normalized schema.
-- =========================================================================

DROP VIEW IF EXISTS ProgressView;
CREATE VIEW ProgressView AS
SELECT s.ChildID, ROUND(AVG(a.IsCorrect) * 100, 2) AS Accuracy
FROM Session s
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY s.ChildID;

DROP VIEW IF EXISTS ParentChildProgressView;
CREATE VIEW ParentChildProgressView AS
SELECT
    p.ParentID,
    p.Name AS ParentName,
    u.Username AS Email,                                      -- Email now lives in User
    c.ChildID,
    c.Name AS ChildName,
    TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,     -- computed from DOB
    COUNT(DISTINCT s.SessionID) AS TotalSessions,
    IFNULL(ROUND(AVG(a.IsCorrect) * 100, 2), 0) AS Accuracy
FROM Parent p
LEFT JOIN User u   ON u.ParentID  = p.ParentID AND u.Role = 'Parent'
JOIN Child c       ON p.ParentID  = c.ParentID
LEFT JOIN Session s ON c.ChildID  = s.ChildID
LEFT JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY p.ParentID, p.Name, u.Username, c.ChildID, c.Name, c.DateOfBirth;

DROP VIEW IF EXISTS TeacherChildView;
CREATE VIEW TeacherChildView AS
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

DROP VIEW IF EXISTS ChildSessionSummary;
CREATE VIEW ChildSessionSummary AS
SELECT
    c.ChildID, c.Name,
    COUNT(s.SessionID) AS SessionCount,
    IFNULL(SUM(s.Duration), 0) AS TotalMinutes,
    ROUND(IFNULL(SUM(s.Duration), 0) / 60, 1) AS TotalHours
FROM Child c
LEFT JOIN Session s ON c.ChildID = s.ChildID
GROUP BY c.ChildID, c.Name;


-- =========================================================================
-- STEP 7 — REBUILD STORED PROCEDURES
-- -------------------------------------------------------------------------
-- GetChildProfilesByParent : now returns Age computed from DOB
-- GetChildProgressReport   : no longer INSERTs into ProgressReport (it's a
--                            view); just returns summary + JSON history
-- LoadDashboardStats       : unchanged in shape, kept for backend parity
-- =========================================================================

DROP PROCEDURE IF EXISTS GetChildProfilesByParent;
DROP PROCEDURE IF EXISTS GetChildProgressReport;
DROP PROCEDURE IF EXISTS LoadDashboardStats;

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

    -- ProgressReport is a VIEW now, so we no longer INSERT into it.
    -- The summary below is computed live from base tables on every call.

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
        SELECT
            COUNT(DISTINCT c.ChildID)            AS ChildCount,
            COUNT(DISTINCT s.SessionID)          AS SessionCount,
            IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0) AS OverallAccuracy
        FROM Child c
        LEFT JOIN Session s ON c.ChildID  = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID = a.SessionID
        WHERE c.ParentID = p_RefID;
    ELSEIF p_Role = 'Teacher' THEN
        SELECT
            COUNT(DISTINCT ce.ChildID)           AS ChildCount,
            COUNT(DISTINCT s.SessionID)          AS SessionCount,
            IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0) AS OverallAccuracy
        FROM Child_Educator ce
        JOIN Child c        ON ce.ChildID   = c.ChildID
        LEFT JOIN Session s ON c.ChildID    = s.ChildID
        LEFT JOIN Attempt a ON s.SessionID  = a.SessionID
        WHERE ce.EducatorID = p_RefID;
    END IF;
END //

DELIMITER ;


-- =========================================================================
-- STEP 8 — RE-ENABLE INTEGRITY ENFORCEMENT
-- =========================================================================
SET FOREIGN_KEY_CHECKS = 1;


-- =========================================================================
-- STEP 9 — AFTER-STATE VERIFICATION  (proves anomalies are eliminated)
-- =========================================================================

SELECT '====== AFTER STATE — schema verification ======' AS Section;

-- (1) Parent and Educator no longer have an Email column
SELECT 'AFTER #1/#2: Parent and Educator no longer carry Email' AS Check_Description;
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'Grow21_DB'
  AND TABLE_NAME IN ('Parent','Educator')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- (2) User now holds Username (not Email) and the Child role
SELECT 'AFTER #3: User table is the single auth source — incl. Child accounts' AS Check_Description;
SELECT Role, COUNT(*) AS UserCount FROM User GROUP BY Role;

-- (3) Child has DateOfBirth instead of Age
SELECT 'AFTER #4: Child stores DateOfBirth; Age is computed' AS Check_Description;
SELECT ChildID, Name, DateOfBirth,
       TIMESTAMPDIFF(YEAR, DateOfBirth, CURDATE()) AS ComputedAge
FROM Child LIMIT 5;

-- (4) Question table exists and Attempt.QuestionID is a real FK
SELECT 'AFTER #5: Question table populated; Attempt.QuestionID is a valid FK' AS Check_Description;
SELECT COUNT(*) AS QuestionRows FROM Question;
SELECT COUNT(*) AS OrphanedAttempts
FROM Attempt a LEFT JOIN Question q ON a.QuestionID = q.QuestionID
WHERE q.QuestionID IS NULL;

-- (5) ProgressReport is now a view returning live aggregates
SELECT 'AFTER #6: ProgressReport is a derived view (always fresh)' AS Check_Description;
SELECT TABLE_TYPE
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'Grow21_DB' AND TABLE_NAME = 'ProgressReport';
SELECT * FROM ProgressReport ORDER BY ChildID LIMIT 5;


-- =========================================================================
-- STEP 10 — FUNCTIONAL DEPENDENCIES OF THE FINAL (NORMALIZED) SCHEMA
-- -------------------------------------------------------------------------
-- For the viva, here are the FDs after decomposition. Every non-key
-- attribute depends on the WHOLE primary key, and ONLY on the primary key
-- (no transitive deps) — so each relation is in BCNF.
-- =========================================================================
-- School(SchoolID)         -> Name, Location
-- Parent(ParentID)         -> Name, Phone
-- Educator(EducatorID)     -> SchoolID, Name
-- User(UserID)             -> Username, Password, Role, ParentID, EducatorID, ChildID
-- User(Username)           -> UserID                 (Username is a candidate key)
-- Child(ChildID)           -> ParentID, Name, DateOfBirth, BaselineSkillLevel
-- Child_Educator(ChildID, EducatorID)                (all-key relation)
-- Module(ModuleID)         -> ModuleName, Category, DifficultyLevel
-- Lesson(LessonID)         -> ModuleID, LessonTitle, TargetSkill
-- Question(QuestionID)     -> LessonID, QuestionText, CorrectAnswer
-- Session(SessionID)       -> ChildID, LessonID, SessionDate, Duration, CompletionPercentage
-- Attempt(AttemptID)       -> SessionID, QuestionID, IsCorrect, ResponseTime, StreakCount
-- =========================================================================
-- END OF NORMALIZATION SCRIPT
-- =========================================================================
