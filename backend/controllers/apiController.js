const db = require('../db');

exports.login = (req, res) => {
  const { email, password } = req.body;
  console.log(`Login attempt - Email: '${email}', Password: '${password}'`);
  const query = 'SELECT UserID, Email, Role, ParentID, EducatorID FROM User WHERE Email = ? AND Password = ?';
  db.query(query, [email, password], (err, results) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ error: err.message });
    }
    console.log(`Login SQL Results Length: ${results.length}`);
    if (results.length === 0) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    const user = results[0];
    const refId = user.Role === 'Parent' ? user.ParentID : user.EducatorID;

    // A real app would generate a JWT token here, but we'll use a pseudo-token that is just the UserID encoded for simplicity 
    // to match the prompt's request for "validate the saved login token/session"
    const token = Buffer.from(JSON.stringify({ id: user.UserID, time: Date.now() })).toString('base64');

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.UserID,
        name: user.Email.split('@')[0],
        email: user.Email,
        role: user.Role,
        refId: refId
      }
    });
  });
};

exports.validateToken = (req, res) => {
  const { token } = req.body;
  if (!token) return res.status(401).json({ message: 'No token provided' });
  
  try {
    const decoded = JSON.parse(Buffer.from(token, 'base64').toString('ascii'));
    const query = 'SELECT UserID, Email, Role, ParentID, EducatorID FROM User WHERE UserID = ?';
    db.query(query, [decoded.id], (err, results) => {
      if (err) return res.status(500).json({ error: err.message });
      if (results.length === 0) return res.status(401).json({ message: 'User no longer exists' });
      
      const user = results[0];
      const refId = user.Role === 'Parent' ? user.ParentID : user.EducatorID;
      
      res.json({
        valid: true,
        user: {
          id: user.UserID,
          name: user.Email.split('@')[0],
          email: user.Email,
          role: user.Role,
          refId: refId
        }
      });
    });
  } catch (e) {
    res.status(401).json({ message: 'Invalid token format' });
  }
};

exports.forgotPassword = (req, res) => {
  const { email } = req.body;
  const query = 'SELECT UserID FROM User WHERE Email = ?';
  db.query(query, [email], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    if (results.length === 0) return res.status(404).json({ message: 'Email not found' });
    
    // In a real app, send an email. For this demo, we'll just simulate a successful reset email sent.
    res.json({ message: 'A password reset link has been sent to your email.' });
  });
};

exports.resetPassword = (req, res) => {
  const { email, newPassword } = req.body;
  const query = 'UPDATE User SET Password = ? WHERE Email = ?';
  db.query(query, [newPassword, email], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    if (results.affectedRows === 0) return res.status(404).json({ message: 'User not found' });
    res.json({ message: 'Password has been reset successfully. You can now login.' });
  });
};

exports.signup = (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).json({ error: 'Missing fields' });
  
  // Check if user already exists
  db.query('SELECT UserID FROM User WHERE Email = ?', [email], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    if (results.length > 0) return res.status(409).json({ error: 'User already exists' });
    
    const parentName = email.split('@')[0];
    db.query('INSERT INTO Parent (Name, Email) VALUES (?, ?)', [parentName, email], (err2, parentRes) => {
      if (err2) return res.status(500).json({ error: err2.message });
      
      const parentId = parentRes.insertId;
      db.query('INSERT INTO User (Email, Password, Role, ParentID) VALUES (?, ?, ?, ?)', [email, password, 'Parent', parentId], (err3) => {
        if (err3) return res.status(500).json({ error: err3.message });
        res.status(201).json({ message: 'Signup successful', parentId: parentId });
      });
    });
  });
};

exports.childLogin = (req, res) => {
  const { username, password } = req.body;
  const query = 'SELECT ChildID, ParentID, Name, Age, BaselineSkillLevel FROM Child WHERE Username = ? AND Password = ?';
  db.query(query, [username, password], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    if (results.length === 0) return res.status(401).json({ message: 'Invalid credentials' });
    res.json({ message: 'Login successful', child: results[0] });
  });
};

// 1. Dashboard APIs
exports.getDashboardStats = (req, res) => {
  const { role, refId } = req.query;

  let childFilter = '';
  let sessionFilter = '';
  let progressFilter = '';
  const params = [];

  if (role === 'Parent' && refId) {
    childFilter = `WHERE ParentID = ?`;
    sessionFilter = `WHERE ChildID IN (SELECT ChildID FROM Child WHERE ParentID = ?)`;
    progressFilter = `WHERE ChildID IN (SELECT ChildID FROM Child WHERE ParentID = ?)`;
    params.push(Number(refId), Number(refId), Number(refId));
  } else if (role === 'Teacher' && refId) {
    childFilter = `JOIN Child_Educator ce ON Child.ChildID = ce.ChildID WHERE ce.EducatorID = ?`;
    sessionFilter = `WHERE ChildID IN (SELECT ChildID FROM Child_Educator WHERE EducatorID = ?)`;
    progressFilter = `WHERE ChildID IN (SELECT ChildID FROM Child_Educator WHERE EducatorID = ?)`;
    params.push(Number(refId), Number(refId), Number(refId));
  }

  const statsQuery = `
    SELECT 
      (SELECT COUNT(*) FROM Child ${childFilter}) AS total_children,
      (SELECT COUNT(*) FROM Session ${sessionFilter}) AS total_sessions,
      (SELECT AVG(AccuracyRate) FROM ProgressReport ${progressFilter}) AS avg_accuracy
  `;

  db.query(statsQuery, params, (err, statsResults) => {
    if (err) return res.status(500).json({ error: err.message });
    
    if (role === 'Teacher' && refId) {
      // Fetch top performers and weak students for Teacher
      const weakQuery = `
        SELECT c.ChildID, c.Name AS ChildName, ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
        FROM Child c
        JOIN Child_Educator ce ON c.ChildID = ce.ChildID
        JOIN Session s ON c.ChildID = s.ChildID
        JOIN Attempt a ON s.SessionID = a.SessionID
        WHERE ce.EducatorID = ?
        GROUP BY c.ChildID, c.Name
        HAVING AccuracyPercent < 50
        ORDER BY AccuracyPercent ASC LIMIT 5
      `;
      const topQuery = `
        SELECT c.ChildID, c.Name AS ChildName, ROUND(AVG(a.IsCorrect) * 100, 2) AS AccuracyPercent
        FROM Child c
        JOIN Child_Educator ce ON c.ChildID = ce.ChildID
        JOIN Session s ON c.ChildID = s.ChildID
        JOIN Attempt a ON s.SessionID = a.SessionID
        WHERE ce.EducatorID = ?
        GROUP BY c.ChildID, c.Name
        HAVING AccuracyPercent >= 80
        ORDER BY AccuracyPercent DESC LIMIT 5
      `;
      const activityQuery = `
        SELECT m.Category, COUNT(s.SessionID) AS count, ROUND(AVG(a.IsCorrect) * 100, 2) AS accuracy
        FROM Module m
        JOIN Lesson l ON m.ModuleID = l.ModuleID
        JOIN Session s ON l.LessonID = s.LessonID
        LEFT JOIN Attempt a ON s.SessionID = a.SessionID
        JOIN Child_Educator ce ON s.ChildID = ce.ChildID
        WHERE ce.EducatorID = ?
        GROUP BY m.Category
      `;
      
      db.query(weakQuery, [refId], (err2, weakResults) => {
        db.query(topQuery, [refId], (err3, topResults) => {
          db.query(activityQuery, [refId], (err4, activityResults) => {
            res.json({
              ...statsResults[0],
              weakStudents: weakResults || [],
              topPerformers: topResults || [],
              activityStats: activityResults || []
            });
          });
        });
      });
    } else {
      res.json(statsResults[0]);
    }
  });
};

// 2. Parent APIs
exports.getAllParents = (req, res) => {
  db.query('SELECT * FROM Parent', (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addParent = (req, res) => {
  const { Name, Email, Phone } = req.body;
  const query = 'INSERT INTO Parent (Name, Email, Phone) VALUES (?, ?, ?)';
  db.query(query, [Name, Email, Phone], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    const parentId = results.insertId;
    
    const userQuery = 'INSERT INTO User (Email, Password, Role, ParentID) VALUES (?, ?, ?, ?)';
    db.query(userQuery, [Email, 'pass123', 'Parent', parentId], (err2) => {
      if (err2) return res.status(500).json({ error: err2.message });
      res.status(201).json({ message: 'Parent added successfully', id: parentId });
    });
  });
};

// 3. Child APIs
exports.getAllChildren = (req, res) => {
  db.query('SELECT * FROM Child', (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.getChildWithParent = (req, res) => {
  const { role, refId } = req.query;
  // JOIN Query with role filtering and AvgAccuracy calculation using ProgressView
  let query = `
    SELECT c.ChildID, c.Name AS ChildName, c.Age, c.BaselineSkillLevel,
           p.ParentID, p.Name AS ParentName, p.Email,
           IFNULL(pv.Accuracy, 0) AS AvgAccuracy
    FROM Child c
    LEFT JOIN Parent p ON c.ParentID = p.ParentID
    LEFT JOIN ProgressView pv ON c.ChildID = pv.ChildID
  `;

  const params = [];

  if (role === 'Parent' && refId) {
    query += ` WHERE c.ParentID = ? `;
    params.push(Number(refId));
  } else if (role === 'Teacher' && refId) {
    query += ` JOIN Child_Educator ce ON c.ChildID = ce.ChildID WHERE ce.EducatorID = ? `;
    params.push(Number(refId));
  }

  query += ` ORDER BY c.Name ASC`;

  db.query(query, params, (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addChild = (req, res) => {
  const { ParentID, ParentEmail, ParentPassword, Name, Age, BaselineSkillLevel, ChildUsername, ChildPassword } = req.body;

  // Auto-generate child credentials if not provided
  const finalChildUsername = ChildUsername || (Name.replace(/\s+/g, '').toLowerCase() + Math.floor(Math.random() * 10000));
  const finalChildPassword = ChildPassword || 'child123';

  if (ParentEmail) {
    // Lookup parent ID from the provided email
    db.query('SELECT p.ParentID, u.Password FROM Parent p LEFT JOIN User u ON p.ParentID = u.ParentID WHERE p.Email = ?', [ParentEmail], (err, results) => {
      if (err) return res.status(500).json({ error: err.message });

      if (results.length === 0) {
        // Parent doesn't exist -> Create a new Parent automatically
        const parentName = ParentEmail.split('@')[0]; // Use prefix of email as placeholder name
        db.query('INSERT INTO Parent (Name, Email) VALUES (?, ?)', [parentName, ParentEmail], (err, parentRes) => {
          if (err) return res.status(500).json({ error: err.message });

          const newParentID = parentRes.insertId;
          const userPassword = ParentPassword || 'pass123';

          // Also create a default User account so they can log in
          db.query('INSERT INTO User (Email, Password, Role, ParentID) VALUES (?, ?, ?, ?)', [ParentEmail, userPassword, 'Parent', newParentID], (err) => {
            if (err) return res.status(500).json({ error: err.message });
            
            // Finally, add the child
            const query = 'INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel, Username, Password) VALUES (?, ?, ?, ?, ?, ?)';
            db.query(query, [newParentID, Name, Age, BaselineSkillLevel, finalChildUsername, finalChildPassword], (err, insertResults) => {
              if (err) return res.status(500).json({ error: err.message });
              res.status(201).json({ message: 'Child and new Parent added successfully', id: insertResults.insertId, parentId: newParentID });
            });
          });
        });
      } else {
        // Parent exists -> Verify password before adding child
        const foundParent = results[0];
        
        // If ParentPassword was provided, and the User table has a password, they must match
        if (ParentPassword && foundParent.Password && foundParent.Password !== ParentPassword) {
            return res.status(401).json({ error: 'Incorrect password for existing parent account' });
        }
        
        const foundParentID = foundParent.ParentID;
        const query = 'INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel, Username, Password) VALUES (?, ?, ?, ?, ?, ?)';
        db.query(query, [foundParentID, Name, Age, BaselineSkillLevel, finalChildUsername, finalChildPassword], (err, insertResults) => {
          if (err) return res.status(500).json({ error: err.message });
          res.status(201).json({ message: 'Child added successfully', id: insertResults.insertId, parentId: foundParentID });
        });
      }
    });
  } else {
    // Fallback if ParentID is directly provided (e.g. logged in as Parent)
    const query = 'INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel, Username, Password) VALUES (?, ?, ?, ?, ?, ?)';
    db.query(query, [ParentID, Name, Age, BaselineSkillLevel, finalChildUsername, finalChildPassword], (err, results) => {
      if (err) return res.status(500).json({ error: err.message });
      res.status(201).json({ message: 'Child added successfully', id: results.insertId });
    });
  }
};

// 4. Session APIs
exports.getSessionsByChild = (req, res) => {
  const { childId } = req.params;
  const query = `
    SELECT s.SessionID, s.SessionDate, s.Duration, s.CompletionPercentage, l.LessonTitle, m.ModuleName, c.Name AS ChildName
    FROM Session s
    LEFT JOIN Lesson l ON s.LessonID = l.LessonID
    LEFT JOIN Module m ON l.ModuleID = m.ModuleID
    LEFT JOIN Child c ON s.ChildID = c.ChildID
    WHERE s.ChildID = ?
    ORDER BY s.SessionDate DESC
  `;
  db.query(query, [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addSession = (req, res) => {
  const { ChildID, LessonID, SessionDate, Duration } = req.body;
  const query = 'INSERT INTO Session (ChildID, LessonID, SessionDate, Duration) VALUES (?, ?, ?, ?)';
  db.query(query, [ChildID, LessonID, SessionDate, Duration], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.status(201).json({ message: 'Session added successfully', id: results.insertId });
  });
};

// 5. Progress APIs
exports.getProgressByChild = (req, res) => {
  const { childId } = req.params;
  const query = 'SELECT * FROM ProgressReport WHERE ChildID = ? ORDER BY GeneratedDate DESC';
  db.query(query, [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.callProgressStoredProcedure = (req, res) => {
  const { childId } = req.params;
  // Make sure the Stored Procedure EXACTLY matches your database script 
  const query = 'CALL GetChildProgressReport(?)';
  db.query(query, [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    // Detailed SP results usually come in an array format inside results[0]
    res.json(results[0]);
  });
};

// 6. App Sync APIs
exports.appSession = (req, res) => {
  const { childId, gameType, score, total, duration, isCompleted } = req.body;
  
  // Map Android gameType to CRM LessonID
  let lessonId = 1; // Default
  
  // MOVE AND PLAY
  if (gameType === 'Trace Lines') lessonId = 1;
  else if (gameType === 'Draw Shapes') lessonId = 2;
  else if (gameType === 'Free Draw') lessonId = 3;
  // VOCABULARY
  else if (gameType === 'Image-based MCQ') lessonId = 4;
  // PUZZLES
  else if (gameType === 'Color Sorting') lessonId = 5;
  else if (gameType === 'Memory Flip') lessonId = 6;
  else if (gameType === 'Drag and Match') lessonId = 7;

  // Real-time tracking
  const actualDuration = duration || 10; // Fallback if not provided by app
  const completionPercentage = isCompleted === true ? 100 : (isCompleted === false && total > 0 ? Math.round((score / total) * 100) : 0);

  // Insert Session
  const sessionQuery = 'INSERT INTO Session (ChildID, LessonID, SessionDate, Duration, CompletionPercentage) VALUES (?, ?, CURDATE(), ?, ?)';
  
  db.query(sessionQuery, [childId, lessonId, actualDuration, completionPercentage], (err, sessionRes) => {
    if (err) return res.status(500).json({ error: err.message });
    
    const sessionId = sessionRes.insertId;
    const correctCount = score || 0;
    const incorrectCount = (total || 0) - correctCount;
    
    // We will generate fake Attempt rows so the CRM calculates Accuracy correctly
    let attempts = [];
    for (let i = 0; i < correctCount; i++) attempts.push([sessionId, 101 + i, 1, 3.0, 0]);
    for (let i = 0; i < incorrectCount; i++) attempts.push([sessionId, 201 + i, 0, 5.0, 0]);
    
    if (attempts.length === 0) {
      return res.status(201).json({ message: 'Session added without attempts', sessionId });
    }

    const attemptQuery = 'INSERT INTO Attempt (SessionID, QuestionID, IsCorrect, ResponseTime, StreakCount) VALUES ?';
    db.query(attemptQuery, [attempts], (err) => {
      if (err) return res.status(500).json({ error: err.message });
      res.status(201).json({ message: 'Session and Attempts synced successfully' });
    });
  });
};

