import { Link, useLocation } from 'react-router-dom';

const Navbar = ({ currentUser, onLogout, activeChild, onSwitchChild }) => {
  const location = useLocation();

  const isActive = (path) => {
    return location.pathname === path ? 'nav-link active' : 'nav-link';
  };

  return (
    <nav className="navbar">
      <div className="nav-brand" style={{ display: 'flex', flexDirection: 'column' }}>
        <span style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>Grow21</span>
        <span style={{ fontSize: '0.875rem', fontWeight: 'normal', opacity: 0.8 }}>Learning Management System</span>
      </div>
      
      {activeChild && (
        <div style={{ marginBottom: '2rem', padding: '1rem', backgroundColor: 'var(--card-bg)', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '8px', backgroundColor: 'var(--primary-color)', color: '#0F172A', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '1.2rem' }}>
            {activeChild.ChildName.charAt(0).toUpperCase()}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>{activeChild.ChildName}</span>
            <button 
              onClick={onSwitchChild}
              style={{ background: 'none', border: 'none', color: 'var(--primary-color)', fontSize: '0.75rem', cursor: 'pointer', textAlign: 'left', padding: 0, textDecoration: 'underline' }}
            >
              Switch Profile
            </button>
          </div>
        </div>
      )}

      <ul className="nav-links">
        <li>
          <Link to="/" className={isActive('/')}>Dashboard</Link>
        </li>
        {(!activeChild && currentUser?.role !== 'Parent') && (
          <li>
            <Link to="/children" className={isActive('/children')}>Children</Link>
          </li>
        )}
        {activeChild && (
          <li>
            <a href="#" className="nav-link" onClick={(e) => { e.preventDefault(); onSwitchChild(); }}>Switch Profile</a>
          </li>
        )}
      </ul>
      <div style={{ marginTop: 'auto', display: 'flex', alignItems: 'center', gap: '1rem', justifyContent: 'center', padding: '1rem', borderRadius: '8px', backgroundColor: 'rgba(255, 255, 255, 0.03)' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', color: 'white' }}>
          <span style={{ fontSize: '0.9rem', fontWeight: '600' }}>{currentUser?.name}</span>
          <span style={{ fontSize: '0.75rem', opacity: 0.8, backgroundColor: 'rgba(255,255,255,0.2)', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
            {currentUser?.role}
          </span>
        </div>
        <button 
          onClick={onLogout}
          style={{ padding: '0.35rem 0.75rem', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.5)', backgroundColor: 'transparent', color: 'white', cursor: 'pointer', transition: 'all 0.2s' }}
          onMouseOver={(e) => { e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.1)' }}
          onMouseOut={(e) => { e.currentTarget.style.backgroundColor = 'transparent' }}
        >
          Logout
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
