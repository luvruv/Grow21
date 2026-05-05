const db = require('./db');

db.query('ALTER TABLE Child ADD COLUMN Username VARCHAR(100) UNIQUE, ADD COLUMN Password VARCHAR(255);', (err, results) => {
  if (err) {
    console.error('Error adding columns:', err.message);
  } else {
    console.log('Username and Password columns successfully added to Child table.');
    
    // Auto-generate some usernames for existing children so it's not null and uniquely constrained properly
    db.query('UPDATE Child SET Username = CONCAT(REPLACE(LOWER(Name), " ", ""), ChildID), Password = "pass123" WHERE Username IS NULL;', (err2) => {
        if(err2) console.error(err2);
        else console.log('Existing children updated.');
        process.exit();
    });
  }
});
