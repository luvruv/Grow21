# Grow21 DBMS Presentation Walkthrough

This guide is designed to help you ace your DBMS project presentation. It gives you a step-by-step script to prove that your Frontend, Backend, and Android App are perfectly integrated with your advanced MySQL database.

---

## 💻 Preparation Checklist (Before the Presentation)

Have these windows open and ready to switch between:
1. **VS Code**: Showing your project files (`Grow21_Cloud_Init.sql`, `apiController.js`, `DatabaseHelper.java`, `Dashboard.jsx`).
2. **Terminal 1**: Running the backend (`cd backend` -> `npm start` or `node server.js`).
3. **Terminal 2**: Running the frontend (`cd frontend` -> `npm run dev`).
4. **Android Studio**: With your Android Emulator running the app.
5. **MySQL Workbench**: Connected to your database (Aiven Cloud or Local).

---

## 🎬 Phase 1: Showcasing the Database Architecture (The "Code")

Start by showing your professor the code. Open these files and point out the exact line numbers:

### 1. Show `Grow21_Cloud_Init.sql` (The Master Schema)
Open this file in VS Code.
* **What to say:** *"We designed our database in BCNF to eliminate redundancy. Here is our master schema."*
* **Highlight Line 338 (`CREATE PROCEDURE RegisterParent`):** Explain that you use this Stored Procedure to ensure atomic transactions (inserting into the `User` and `Parent` tables at the exact same time).
* **Highlight Line 249 (`CREATE TRIGGER trg_validate_attempt_lesson`):** Explain that standard Foreign Keys weren't enough. You wrote a trigger to ensure a child cannot attempt a question that belongs to a different lesson, ensuring strict data integrity.
* **Highlight Line 407 (`CREATE OR REPLACE VIEW ProgressView`):** Explain how you used SQL Views to pre-calculate the average accuracy for children, abstracting complex joins away from the backend.

### 2. Show `backend/controllers/apiController.js` (The Node.js Backend)
* **What to say:** *"Our Node.js backend connects directly to this database and executes these advanced SQL features."*
* **Highlight Line 89:** 
  ```javascript
  db.query('CALL RegisterParent(?, ?, ?, ?, @uid); SELECT @uid AS uid;', ...)
  ```
  *Explain:* *"Instead of writing basic INSERT statements in Javascript, we call our SQL Stored Procedure to offload the heavy transactional logic to the database."*

### 3. Show `user-app/.../DatabaseHelper.java` (The Android App)
* **What to say:** *"Our mobile app is offline-first for performance, but syncs to the cloud using the Retrofit API."*
* **Highlight Line 180:** 
  ```java
  apiService.syncSession(request).enqueue(new Callback<SyncResponse>() { ... })
  ```
  *Explain:* *"When a child finishes a game locally on SQLite, Retrofit pushes the payload to our Node.js API, which then inserts it into the cloud MySQL database."*

### 4. Show `frontend/src/pages/Dashboard.jsx` (The React Frontend)
* **What to say:** *"Our web dashboard securely consumes this validated data."*
* **Highlight Lines 20-21:**
  ```javascript
  fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/progress/report/${activeChild.ChildID}`)
  ```
  *Explain:* *"The frontend pulls real-time progress reports generated dynamically by our SQL Views."*

---

## 🚀 Phase 2: Live Demo - Adding a User (Frontend ➡️ Database)

Now, prove that the web interface physically alters the database.

### Step 1: Show the Code (Before Action)
*   **Show Frontend Code:** Open `frontend/src/pages/Children.jsx` and point to **Line 44**. This is where the React app sends the `POST` request to `/api/children`.
*   **Show Backend Code:** Open `backend/controllers/apiController.js` and point to **Line 289** (`exports.addChild`). Scroll down to **Line 332** to show where it safely calls the `RegisterParent` and `RegisterChild` stored procedures if a new user is created.

### Step 2: Baseline Check in MySQL Workbench
Run these queries in Workbench:
```sql
SELECT * FROM User ORDER BY UserID DESC LIMIT 5;
SELECT * FROM Parent ORDER BY ParentID DESC LIMIT 5;
```

### Step 3: Action in the Web Browser
1. Go to your React Frontend (`http://localhost:5173/children` or login page).
2. Click "Add Child".
3. Enter dummy data (e.g., Parent Name: `Professor Demo`, Email: `prof@demo.com`).
4. Click **Save**.

### Step 4: Proof in MySQL Workbench
Run the exact same queries again:
```sql
SELECT * FROM User ORDER BY UserID DESC LIMIT 5;
SELECT * FROM Parent ORDER BY ParentID DESC LIMIT 5;
```
* **What to say:** *"As you can see, 'prof@demo.com' instantly appears in the database. The `RegisterParent` stored procedure (defined at Line 338 in Grow21_Cloud_Init.sql) executed flawlessly, passing data directly from Line 44 of our React Frontend."*

---

## 📱 Phase 3: Live Demo - Playing a Game (Android App ➡️ Database)

Next, prove the mobile app syncs live session data.

### Step 1: Show the Code (Before Action)
*   **Show Android Code:** Open `user-app/app/src/main/java/com/example/grow21/DatabaseHelper.java` and point to **Line 180**. Show how Retrofit enqueues the `syncSession` POST payload with the child's score.
*   **Show Backend Code:** Open `backend/controllers/apiController.js` and point to **Line 450** (`exports.appSession`). Highlight how it validates the score and creates the session securely.

### Step 2: Baseline Check in MySQL Workbench
```sql
SELECT * FROM Session ORDER BY SessionID DESC LIMIT 5;
```

### Step 3: Action in the Android Emulator
1. Open the Grow21 app and play a quick game.
2. Finish the game so the "Celebration" screen triggers (this fires the Retrofit sync at Line 180).

### Step 4: Proof in MySQL Workbench
```sql
SELECT s.SessionID, s.ChildID, l.LessonTitle, s.IsCompleted, s.SessionDate
FROM Session s JOIN Lesson l ON s.LessonID = l.LessonID
ORDER BY s.SessionID DESC LIMIT 5;
```
* **What to say:** *"The game the child just finished on the Android emulator has been transmitted through Retrofit and inserted into our Cloud Database. Notice the `IsCompleted` flag (handled at Line 450 in apiController.js) is accurately set."*

---

## 🧠 Phase 4: Advanced Queries & Locking (Impress the Professor)

To get top marks, show off some advanced SQL commands directly in MySQL Workbench.

### 1. Concurrency Control (Row-Level Locking)
Demonstrate that your database can handle multiple users updating the same record safely.
* **What to say:** *"To prevent race conditions if multiple teachers try to update a child's baseline skill (defined at Line 95 in Grow21_Cloud_Init.sql) at the exact same millisecond, we use `FOR UPDATE` to establish a row-level write lock."*
* **Run this in Workbench:**
```sql
START TRANSACTION;

-- Lock the row for the specific child
SELECT BaselineSkillLevel 
FROM Child 
WHERE ChildID = 1 
FOR UPDATE;

-- Update the record safely
UPDATE Child 
SET BaselineSkillLevel = 'Intermediate' 
WHERE ChildID = 1;

COMMIT;
```

### 2. Advanced Aggregation with Window Functions
Show an analytical query that ranks children by their performance.
* **What to say:** *"We use advanced Window Functions like `RANK()` to dynamically calculate leaderboards without needing extra tables, building upon the complex aggregation logic we defined for our `ProgressView` at Line 407 in Grow21_Cloud_Init.sql."*
* **Run this in Workbench:**
```sql
SELECT 
    c.Name AS ChildName,
    COUNT(s.SessionID) AS TotalSessions,
    ROUND(AVG(a.IsCorrect) * 100, 2) AS AverageAccuracy,
    RANK() OVER (ORDER BY AVG(a.IsCorrect) DESC) AS PerformanceRank
FROM Child c
JOIN Session s ON c.ChildID = s.ChildID
JOIN Attempt a ON s.SessionID = a.SessionID
GROUP BY c.ChildID, c.Name
ORDER BY PerformanceRank;
```

### 3. Built-in Transactions (COMMIT and ROLLBACK)
Show that you have pre-written test transactions inside your initialization script to demonstrate ACID properties.
* **What to say:** *"We have built-in demo scripts to prove our database handles COMMIT and ROLLBACK transactions safely. You can see them defined starting at Line 638 in Grow21_Cloud_Init.sql."*
* **Run this in Workbench (from Line 638):**
```sql
-- Demo transaction: COMMIT a new parent + child via the registration procedures.
START TRANSACTION;
CALL RegisterParent('neha@gmail.com', 'neha123', 'Neha Gupta', '9810000099', @new_parent_id);
CALL RegisterChild(@new_parent_id, CONCAT('rohangupta', @new_parent_id), 'pass123', 'Rohan Gupta', '2020-01-15', 'Beginner', @new_child_id);
COMMIT;
```
* **Run this in Workbench (from Line 644):**
```sql
-- Demo transaction: ROLLBACK an experimental insert.
START TRANSACTION;
INSERT INTO `User` (Username, Password, Role) VALUES ('rollbacktest', 'x', 'Parent');
ROLLBACK;
```
