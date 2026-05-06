const fs = require('fs');
let c = fs.readFileSync('Grow21_Cloud_Init.sql', 'utf8');

c = c.replace(/ROUND\(AVG\(a\.IsCorrect\)\*100,2\)/g, 'ROUND(AVG(a.IsCorrect)*100,0)');
c = c.replace(/ROUND\(AVG\(a\.IsCorrect\) \* 100, 2\)/g, 'ROUND(AVG(a.IsCorrect) * 100, 0)');
c = c.replace(/ROUND\(SUM\(a\.IsCorrect\) \* 100\.0\s*\/\s*NULLIF\(COUNT\(a\.AttemptID\),\s*0\),\s*2\)/g, 'ROUND(SUM(a.IsCorrect) * 100.0\n        / NULLIF(COUNT(a.AttemptID), 0), 0)');

fs.writeFileSync('Grow21_Cloud_Init.sql', c);
console.log('SQL updated successfully');
