const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, 'src');
const walk = function(dir, done) {
  let results = [];
  fs.readdir(dir, function(err, list) {
    if (err) return done(err);
    let pending = list.length;
    if (!pending) return done(null, results);
    list.forEach(function(file) {
      file = path.resolve(dir, file);
      fs.stat(file, function(err, stat) {
        if (stat && stat.isDirectory()) {
          walk(file, function(err, res) {
            results = results.concat(res);
            if (!--pending) done(null, results);
          });
        } else {
          if (file.endsWith('.jsx') || file.endsWith('.js')) {
            results.push(file);
          }
          if (!--pending) done(null, results);
        }
      });
    });
  });
};

walk(dir, function(err, results) {
  if (err) throw err;
  results.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');
    let modified = false;
    
    // Create the replacement string: `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}$1`
    const replacement = "`\\${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}$1`";
    
    const singleQuoteRegex = /'http:\/\/localhost:5000([^']*)'/g;
    if (singleQuoteRegex.test(content)) {
      content = content.replace(singleQuoteRegex, replacement);
      modified = true;
    }
    
    const backtickRegex = /`http:\/\/localhost:5000([^`]*)`/g;
    if (backtickRegex.test(content)) {
      content = content.replace(backtickRegex, replacement);
      modified = true;
    }
    
    if (modified) {
      fs.writeFileSync(file, content);
      console.log('Updated', file);
    }
  });
});
