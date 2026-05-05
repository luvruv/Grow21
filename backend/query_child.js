const db = require('./db');

const query = `
  SELECT c.Name AS ChildName,
         p.Name AS ParentName,
         pu.Username AS ParentEmail,
         pu.Password AS ParentPassword
  FROM Child c
  JOIN Parent p ON c.ParentID = p.ParentID
  LEFT JOIN User pu ON pu.ParentID = p.ParentID AND pu.Role = 'Parent'
`;

setTimeout(() => {
  db.query(query, (err, results) => {
    if (err) throw err;
    console.log(JSON.stringify(results, null, 2));
    process.exit(0);
  });
}, 500);
