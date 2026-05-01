const db = require('../db');

// 1. Dashboard APIs
exports.getDashboardStats = (req, res) => {
  const query = `
    SELECT 
      (SELECT COUNT(*) FROM Child) AS total_children,
      (SELECT COUNT(*) FROM Session) AS total_sessions,
      (SELECT AVG(AccuracyRate) FROM ProgressReport) AS avg_accuracy
  `;
  db.query(query, (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results[0]);
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
    res.status(201).json({ message: 'Parent added successfully', id: results.insertId });
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
  // JOIN Query
  const query = `
    SELECT c.ChildID, c.Name AS ChildName, c.Age, c.BaselineSkillLevel,
           p.ParentID, p.Name AS ParentName, p.Email
    FROM Child c
    LEFT JOIN Parent p ON c.ParentID = p.ParentID
  `;
  db.query(query, (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
};

exports.addChild = (req, res) => {
  const { ParentID, Name, Age, BaselineSkillLevel } = req.body;
  const query = 'INSERT INTO Child (ParentID, Name, Age, BaselineSkillLevel) VALUES (?, ?, ?, ?)';
  db.query(query, [ParentID, Name, Age, BaselineSkillLevel], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.status(201).json({ message: 'Child added successfully', id: results.insertId });
  });
};

// 4. Session APIs
exports.getSessionsByChild = (req, res) => {
  const { childId } = req.params;
  const query = `
    SELECT s.SessionID, s.SessionDate, s.Duration, l.LessonTitle, m.ModuleName
    FROM Session s
    LEFT JOIN Lesson l ON s.LessonID = l.LessonID
    LEFT JOIN Module m ON l.ModuleID = m.ModuleID
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

