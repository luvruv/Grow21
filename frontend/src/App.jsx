import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import Children from './pages/Children';
import Sessions from './pages/Sessions';
import Progress from './pages/Progress';
import './index.css';

function App() {
  return (
    <Router>
      <div className="app-container">
        <Navbar />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/children" element={<Children />} />
            <Route path="/sessions/:childId" element={<Sessions />} />
            <Route path="/progress/:childId" element={<Progress />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
