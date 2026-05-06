import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import Children from './pages/Children';
import Sessions from './pages/Sessions';
import Progress from './pages/Progress';
import Login from './pages/Login';
import ProfileSelector from './pages/ProfileSelector';
import './index.css';

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [activeChild, setActiveChild] = useState(null);

  useEffect(() => {
    const savedUserStr = localStorage.getItem('grow21_user');
    const savedChildStr = localStorage.getItem('grow21_active_child');

    if (savedUserStr) {
      const savedUser = JSON.parse(savedUserStr);
      if (savedUser.token) {
        // Validate token
        fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/validate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token: savedUser.token })
        })
        .then(res => res.json())
        .then(data => {
          if (data.valid) {
            setCurrentUser({ ...data.user, token: savedUser.token });
            if (savedChildStr) setActiveChild(JSON.parse(savedChildStr));
          } else {
            // Invalid token or user doesn't exist
            handleLogout();
          }
        })
        .catch(err => {
          console.error('Validation error', err);
          // Don't auto-logout on network error to allow offline cache if needed
          setCurrentUser(savedUser);
          if (savedChildStr) setActiveChild(JSON.parse(savedChildStr));
        });
      } else {
        // Old user object without token, clear it
        handleLogout();
      }
    }
  }, []);

  const handleLogin = (user) => {
    setCurrentUser(user);
    localStorage.setItem('grow21_user', JSON.stringify(user));
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setActiveChild(null);
    localStorage.removeItem('grow21_user');
    localStorage.removeItem('grow21_active_child');
  };

  const handleSelectChild = (child) => {
    setActiveChild(child);
    localStorage.setItem('grow21_active_child', JSON.stringify(child));
  };

  const handleSwitchChild = () => {
    setActiveChild(null);
    localStorage.removeItem('grow21_active_child');
  };

  return (
    <Router>
      <div className="app-container">
        {currentUser && (
          <Navbar 
            currentUser={currentUser} 
            onLogout={handleLogout} 
            activeChild={activeChild} 
            onSwitchChild={handleSwitchChild} 
          />
        )}
        <main className="main-content">
          <Routes>
            {/* If not logged in, redirect to login unless on login page */}
            <Route path="/login" element={!currentUser ? <Login onLogin={handleLogin} /> : <Navigate to="/" />} />
            
            {/* Protected Routes */}
            <Route path="/" element={
              currentUser ? (
                currentUser.role === 'Parent' && !activeChild ? (
                  <ProfileSelector currentUser={currentUser} onSelectChild={handleSelectChild} />
                ) : (
                  <Dashboard currentUser={currentUser} activeChild={activeChild} />
                )
              ) : <Navigate to="/login" />
            } />
            <Route path="/children" element={currentUser ? <Children currentUser={currentUser} /> : <Navigate to="/login" />} />
            <Route path="/sessions/:childId" element={currentUser ? <Sessions /> : <Navigate to="/login" />} />
            <Route path="/progress/:childId" element={currentUser ? <Progress /> : <Navigate to="/login" />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
