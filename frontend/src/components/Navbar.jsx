import { Link, useLocation } from 'react-router-dom';

const Navbar = () => {
  const location = useLocation();

  const isActive = (path) => {
    return location.pathname === path ? 'nav-link active' : 'nav-link';
  };

  return (
    <nav className="navbar">
      <div className="nav-brand">GROW21 LMS</div>
      <ul className="nav-links">
        <li>
          <Link to="/" className={isActive('/')}>Dashboard</Link>
        </li>
        <li>
          <Link to="/children" className={isActive('/children')}>Children</Link>
        </li>
        <li>
          {/* Note: In a real app we might pass generic sessions, here we showcase the UI */}
          <Link to="/sessions/1" className={isActive('/sessions/1')}>Sessions</Link>
        </li>
        <li>
          <Link to="/progress/1" className={isActive('/progress/1')}>Progress</Link>
        </li>
      </ul>
    </nav>
  );
};

export default Navbar;
