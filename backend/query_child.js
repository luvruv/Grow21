const mysql = require('mysql2');

const connection = mysql.createConnection({
  host: 'localhost',
  user: 'root',
  password: 'bhavya',
  database: 'Grow21_DB'
});

connection.connect((err) => {
  if (err) throw err;
  
  const query = `
    SELECT c.Name AS ChildName, p.Name AS ParentName, u.Email AS ParentEmail, u.Password AS ParentPassword
    FROM Child c
    JOIN Parent p ON c.ParentID = p.ParentID
    JOIN User u ON p.ParentID = u.ParentID
  `;
  
  connection.query(query, (err, results) => {
    if (err) throw err;
    console.log(JSON.stringify(results, null, 2));
    process.exit(0);
  });
});
