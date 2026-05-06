const db = require('../db');

// Helper: with the ISA design, every subtype's PK equals the User's UserID,
// so refId is just UserID for every role.
function refIdFor(user) {
  return user.Role === 'Admin' ? null : user.UserID;
}

exports.login = (req, res) => {
  const { email, password } = req.body;
  console.log(`Login attempt - Email: '${email}', Password: '${password}'`);
  const query = 'SELECT UserID, Username AS Email, Role FROM `User` WHERE Username = ? AND Password = ?';
  db.query(query, [email, password], (err, results) => {
    if (err) { console.error(err); return res.status(500).json({ error: err.message }); }
    if (results.length === 0) return res.status(401).json({ message: 'Invalid credentials' });

    const user = results[0];
    const token = Buffer.from(JSON.stringify({ id: user.UserID, time: Date.now() })).toString('base64');

    // Best-effort LastLoginAt update — fire and forget.
    db.query('UPDATE `User` SET LastLoginAt = NOW() WHERE UserID = ?', [user.UserID], () => {});

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.UserID,
        name: user.Email.split('@')[0],
        email: user.Email,
        role: user.Role,
        refId: refIdFor(user)
      }
    });
  });
};

exports.validateToken = (req, res) => {
  const { token } = req.body;
  if (!token) return res.status(401).json({ message: 'No token provided' });

  try {
    const decoded = JSON.parse(Buffer.from(token, 'base64').toString('ascii'));
    const query = 'SELECT UserID, Username AS Email, Role FROM `User` WHERE UserID = ?';
    db.query(query, [decoded.id], (err, results) => {
      if (err) return res.status(500).json({ error: err.message });
      if (results.length === 0) return res.status(401).json({ message: 'User no longer exists' });

      const user = results[0];
      res.json({
        valid: true,
        user: {
          id: user.UserID,
          name: user.Email.split('@')[0],
          email: user.Email,
          role: user.Role,
          refId: refIdFor(user)
        }
      });
    });
  } catch (e) {
    res.status(401).json({ message: 'Invalid token format' });
  }
};

exports.forgotPassword = (req, res) => {
  const { email } = req.body;
  db.query('SELECT UserID FROM `User` WHERE Username = ?', [email], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    if (results.length === 0) return res.status(404).json({ message: 'Email not found' });
    res.json({ message: 'A password reset link has been sent to your email.' });
  });
};

exports.getChildAccuracy = (req, res) => {
  const childId = req.params.id;
  const query = `SELECT IFNULL(ROUND(SUM(a.IsCorrect) * 100.0 / NULLIF(COUNT(a.AttemptID), 0), 0) AS Accuracy
                 FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
                 WHERE s.ChildID = ?`;
  db.query(query, [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    const accuracy = results[0] && results[0].Accuracy !== null ? results[0].Accuracy : 0;
    res.json({ childId, accuracy });
  });
};

exports.signup = (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).json({ error: 'Missing fields' });
  const parentName = email.split('@')[0];

  // RegisterParent stored procedure wraps the User + Parent inserts in a transaction.
  db.query('CALL RegisterParent(?, ?, ?, ?, @uid); SELECT @uid AS uid;',
    [email, password, parentName, null],
    (err, results) => {
      if (err) {
        if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ error: 'User already exists' });
        return res.status(500).json({ error: err.message });
      }
      const out = results[results.length - 1];
      const parentId = out && out[0] ? out[0].uid : null;
      res.status(201).json({ message: 'Signup successful', parentId });
    }
  );
};

exports.childLogin = (req, res) => {
  const { username, password } = req.body;
  const query = `
    SELECT c.ChildID, c.ParentID, c.Name,
           TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
           c.BaselineSkillLevel
    FROM \`User\` u
    JOIN Child c ON c.ChildID = u.UserID
    WHERE u.Username = ? AND u.Password = ? AND u.Role = 'Child'
  `;
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
  const params = [];

  if (role === 'Parent' && refId) {
    childFilter   = `WHERE ParentID = ?`;
    sessionFilter = `WHERE ChildID IN (SELECT ChildID FROM Child WHERE ParentID = ?)`;
    params.push(Number(refId), Number(refId));
  } else if (role === 'Teacher' && refId) {
    childFilter   = `JOIN Child_Educator ce ON Child.ChildID = ce.ChildID WHERE ce.EducatorID = ?`;
    sessionFilter = `WHERE ChildID IN (SELECT ChildID FROM Child_Educator WHERE EducatorID = ?)`;
    params.push(Number(refId), Number(refId));
  }

  // Average accuracy is computed live from base tables.
  const accSql = role === 'Parent' && refId
    ? `(SELECT IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0)
         FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
         WHERE s.ChildID IN (SELECT ChildID FROM Child WHERE ParentID = ?))`
    : role === 'Teacher' && refId
    ? `(SELECT IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0)
         FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID
         WHERE s.ChildID IN (SELECT ChildID FROM Child_Educator WHERE EducatorID = ?))`
    : `(SELECT IFNULL(ROUND(AVG(a.IsCorrect)*100,2),0)
         FROM Session s JOIN Attempt a ON s.SessionID = a.SessionID)`;

  if (refId) params.push(Number(refId));

  const statsQuery = `
    SELECT
      (SELECT COUNT(*) FROM Child ${childFilter})       AS total_children,
      (SELECT COUNT(*) FROM Session ${sessionFilter})   AS total_sessions,
      ${accSql}                                         AS avg_accuracy
  `;

  db.query(statsQuery, params, (err, statsResults) => {
    if (err) return res.status(500).json({ error: err.message });

    if (role === 'Teacher' && refId) {
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
              weakStudents:  weakResults     || [],
              topPerformers: topResults      || [],
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
  const query = `
    SELECT p.ParentID, p.Name, p.Phone, u.Username AS Email
    FROM Parent p
    JOIN \`User\` u ON u.UserID = p.ParentID
  `;
  db.query(query, (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addParent = (req, res) => {
  const { Name, Email, Phone } = req.body;
  db.query('CALL RegisterParent(?, ?, ?, ?, @uid); SELECT @uid AS uid;',
    [Email, 'pass123', Name, Phone],
    (err, results) => {
      if (err) {
        if (err.code === 'ER_DUP_ENTRY') return res.status(409).json({ error: 'Email already exists' });
        return res.status(500).json({ error: err.message });
      }
      const out = results[results.length - 1];
      const parentId = out && out[0] ? out[0].uid : null;
      res.status(201).json({ message: 'Parent added successfully', id: parentId });
    }
  );
};

// 3. Child APIs
exports.getAllChildren = (req, res) => {
  const query = `
    SELECT ChildID, ParentID, Name, DateOfBirth,
           TIMESTAMPDIFF(YEAR, DateOfBirth, CURDATE()) AS Age,
           BaselineSkillLevel
    FROM Child
  `;
  db.query(query, (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.getChildWithParent = (req, res) => {
  const { role, refId } = req.query;
  let query = `
    SELECT c.ChildID, c.Name AS ChildName,
           TIMESTAMPDIFF(YEAR, c.DateOfBirth, CURDATE()) AS Age,
           c.BaselineSkillLevel,
           p.ParentID, p.Name AS ParentName, u.Username AS Email,
           IFNULL(pv.Accuracy, 0) AS AvgAccuracy
    FROM Child c
    LEFT JOIN Parent p     ON c.ParentID = p.ParentID
    LEFT JOIN \`User\` u   ON u.UserID    = p.ParentID
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
  const { ParentID, ParentEmail, ParentPassword, Name, Age, DateOfBirth, BaselineSkillLevel, ChildUsername, ChildPassword } = req.body;

  const finalDob = DateOfBirth
    ? DateOfBirth
    : Age
    ? new Date(new Date().setFullYear(new Date().getFullYear() - Number(Age))).toISOString().slice(0, 10)
    : null;

  if (!finalDob) return res.status(400).json({ error: 'DateOfBirth or Age is required' });

  const finalChildUsername = ChildUsername || (Name.replace(/\s+/g, '').toLowerCase() + Math.floor(Math.random() * 10000));
  const finalChildPassword = ChildPassword || 'child123';
  const finalSkill = BaselineSkillLevel || 'Beginner';

  const callRegisterChild = (parentId, done) => {
    db.query(
      'CALL RegisterChild(?, ?, ?, ?, ?, ?, @cid); SELECT @cid AS cid;',
      [parentId, finalChildUsername, finalChildPassword, Name, finalDob, finalSkill],
      (err, results) => {
        if (err) return done(err);
        const out = results[results.length - 1];
        const childId = out && out[0] ? out[0].cid : null;
        done(null, childId);
      }
    );
  };

  if (ParentEmail) {
    // Resolve existing Parent through their User row (Username = email).
    db.query(
      `SELECT u.UserID AS ParentID, u.Password
       FROM \`User\` u
       WHERE u.Username = ? AND u.Role = 'Parent'`,
      [ParentEmail],
      (err, results) => {
        if (err) return res.status(500).json({ error: err.message });

        if (results.length === 0) {
          // Parent doesn't exist — register them first.
          const parentName = ParentEmail.split('@')[0];
          const userPassword = ParentPassword || 'pass123';
          db.query(
            'CALL RegisterParent(?, ?, ?, ?, @uid); SELECT @uid AS uid;',
            [ParentEmail, userPassword, parentName, null],
            (err2, parentResults) => {
              if (err2) return res.status(500).json({ error: err2.message });
              const out = parentResults[parentResults.length - 1];
              const newParentID = out && out[0] ? out[0].uid : null;
              callRegisterChild(newParentID, (err3, childId) => {
                if (err3) return res.status(500).json({ error: err3.message });
                res.status(201).json({ message: 'Child and new Parent added successfully', id: childId, parentId: newParentID });
              });
            }
          );
        } else {
          const foundParent = results[0];
          if (ParentPassword && foundParent.Password && foundParent.Password !== ParentPassword) {
            return res.status(401).json({ error: 'Incorrect password for existing parent account' });
          }
          callRegisterChild(foundParent.ParentID, (err3, childId) => {
            if (err3) return res.status(500).json({ error: err3.message });
            res.status(201).json({ message: 'Child added successfully', id: childId, parentId: foundParent.ParentID });
          });
        }
      }
    );
  } else {
    callRegisterChild(ParentID, (err, childId) => {
      if (err) return res.status(500).json({ error: err.message });
      res.status(201).json({ message: 'Child added successfully', id: childId });
    });
  }
};

// 4. Session APIs
exports.getSessionsByChild = (req, res) => {
  const { childId } = req.params;
  // CompletionPercentage is computed at query time (was a stored derived column).
  const query = `
    SELECT s.SessionID, s.SessionDate, s.Duration, s.IsCompleted,
           CASE
             WHEN s.IsCompleted = TRUE THEN 100
             WHEN att.total > 0 THEN ROUND(att.correct * 100 / att.total)
             ELSE 0
           END AS CompletionPercentage,
           l.LessonTitle, m.ModuleName, c.Name AS ChildName
    FROM Session s
    LEFT JOIN Lesson l ON s.LessonID = l.LessonID
    LEFT JOIN Module m ON l.ModuleID = m.ModuleID
    LEFT JOIN Child  c ON s.ChildID  = c.ChildID
    LEFT JOIN (
      SELECT SessionID, SUM(IsCorrect) AS correct, COUNT(*) AS total
      FROM Attempt GROUP BY SessionID
    ) att ON att.SessionID = s.SessionID
    WHERE s.ChildID = ?
    ORDER BY s.SessionDate DESC
  `;
  db.query(query, [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addSession = (req, res) => {
  const { ChildID, LessonID, SessionDate, Duration, IsCompleted } = req.body;
  const query = 'INSERT INTO Session (ChildID, LessonID, SessionDate, Duration, IsCompleted) VALUES (?, ?, ?, ?, ?)';
  db.query(query, [ChildID, LessonID, SessionDate, Duration, IsCompleted ? 1 : 0], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.status(201).json({ message: 'Session added successfully', id: results.insertId });
  });
};

// 5. Progress APIs
exports.getProgressByChild = (req, res) => {
  const { childId } = req.params;
  db.query('SELECT * FROM ProgressReport WHERE ChildID = ? ORDER BY GeneratedDate DESC', [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.callProgressStoredProcedure = (req, res) => {
  const { childId } = req.params;
  db.query('CALL GetChildProgressReport(?)', [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results[0]);
  });
};

// 6. Feedback APIs (new)
exports.getFeedbackForChild = (req, res) => {
  const { childId } = req.params;
  const query = `
    SELECT f.FeedbackID, f.Rating, f.Comment, f.SessionID, f.CreatedAt,
           e.EducatorID, e.Name AS EducatorName
    FROM Feedback f
    JOIN Educator e ON f.EducatorID = e.EducatorID
    WHERE f.ChildID = ?
    ORDER BY f.CreatedAt DESC
  `;
  db.query(query, [childId], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addFeedback = (req, res) => {
  const { EducatorID, ChildID, SessionID, Rating, Comment } = req.body;
  if (!EducatorID || !ChildID || !Rating) {
    return res.status(400).json({ error: 'EducatorID, ChildID and Rating are required' });
  }
  db.query('CALL SubmitFeedback(?, ?, ?, ?, ?)',
    [EducatorID, ChildID, SessionID || null, Rating, Comment || null],
    (err) => {
      if (err) return res.status(500).json({ error: err.message });
      res.status(201).json({ message: 'Feedback recorded' });
    }
  );
};

exports.appSession = (req, res) => {
  const { childId, gameType, score, total, duration } = req.body;
  
  // Map Android gameType to CRM LessonID
  let lessonId = 1;
  
  if (gameType === 'Trace Lines') lessonId = 1;
  else if (gameType === 'Draw Shapes') lessonId = 2;
  else if (gameType === 'Free Draw') lessonId = 3;
  else if (gameType === 'Image-based MCQ') lessonId = 4;
  else if (gameType === 'Color Sorting') lessonId = 5;
  else if (gameType === 'Memory Flip') lessonId = 6;
  else if (gameType === 'Drag and Match') lessonId = 7;

  const actualDuration = duration || 10;
  const totalCount = total || 0;
  const correctCount = score || 0;
  const incorrectCount = totalCount - correctCount;
  
  const isCompleted = (totalCount > 0) ? 1 : 0;

  const sessionQuery = 'INSERT INTO Session (ChildID, LessonID, SessionDate, Duration, IsCompleted) VALUES (?, ?, CURDATE(), ?, ?)';
  
  db.query(sessionQuery, [childId, lessonId, actualDuration, isCompleted], (err, sessionRes) => {
    if (err) return res.status(500).json({ error: err.message });
    
    const sessionId = sessionRes.insertId;
    
    if (totalCount === 0) {
      return res.status(201).json({ message: 'Session added without attempts', sessionId });
    }

    // Generate Attempt rows so the CRM calculates Accuracy correctly
    // We must use valid QuestionIDs that belong to this LessonID to satisfy the trigger
    db.query('SELECT QuestionID FROM Question WHERE LessonID = ?', [lessonId], (errQ, questions) => {
      if (errQ || questions.length === 0) {
        return res.status(201).json({ message: 'Session added (no questions found for attempts)', sessionId });
      }

      let attempts = [];
      let qIndex = 0;
      for (let i = 0; i < correctCount; i++) {
        const qId = questions[qIndex % questions.length].QuestionID;
        attempts.push([sessionId, qId, 1, 3.0]);
        qIndex++;
      }
      for (let i = 0; i < incorrectCount; i++) {
        const qId = questions[qIndex % questions.length].QuestionID;
        attempts.push([sessionId, qId, 0, 5.0]);
        qIndex++;
      }

      const attemptQuery = 'INSERT INTO Attempt (SessionID, QuestionID, IsCorrect, ResponseTime) VALUES ?';
      db.query(attemptQuery, [attempts], (err2) => {
        if (err2) return res.status(500).json({ error: err2.message });
        
        const accuracyPct = Math.round((correctCount / totalCount) * 100);
        res.status(201).json({ 
          message: 'Session and Attempts synced successfully',
          sessionId,
          isCompleted,
          accuracy: accuracyPct
        });
      });
    });
  });
};

