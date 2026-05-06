const fs = require('fs');
const path = require('path');

function walk(dir) {
  fs.readdirSync(dir).forEach(f => {
    let p = path.join(dir, f);
    if (fs.statSync(p).isDirectory()) {
      walk(p);
    } else if (p.endsWith('.jsx')) {
      let c = fs.readFileSync(p, 'utf8');
      
      let fixed = c.split('\\${import.meta.env.VITE_API_BASE_URL').join('${import.meta.env.VITE_API_BASE_URL');
      
      if (c !== fixed) {
        fs.writeFileSync(p, fixed);
        console.log('Fixed', p);
      }
    }
  });
}

walk('./src');
console.log('Done fixing template literals');
