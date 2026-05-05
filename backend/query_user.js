const db = require('./db');

async function findChild() {
    try {
        const [rows] = await db.promise().query(
            `SELECT c.ChildID, c.Name AS ChildName, c.ParentID,
                    p.Name AS ParentName,
                    pu.Username AS ParentEmail,
                    pu.Password AS ParentPassword
             FROM Child c
             JOIN Parent p ON c.ParentID = p.ParentID
             LEFT JOIN User pu ON pu.ParentID = p.ParentID AND pu.Role = 'Parent'
             WHERE c.Name LIKE '%hein%'`
        );
        if (rows.length > 0) {
            console.log("Found child 'hein':");
            rows.forEach(r => console.log(r));
        } else {
            console.log("No child named 'hein' found. Listing all children:");
            const [all] = await db.promise().query('SELECT ChildID, Name, ParentID FROM Child');
            all.forEach(r => console.log(r));
        }
    } catch (err) {
        console.error(err);
    } finally {
        process.exit();
    }
}

findChild();
