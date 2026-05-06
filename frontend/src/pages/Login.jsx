import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const Login = ({ onLogin }) => {
  const [step, setStep] = useState('role'); // 'role', 'login', 'forgot', 'reset'
  const [selectedRole, setSelectedRole] = useState(null);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  const handleRoleSelect = (role) => {
    setSelectedRole(role);
    setStep('login');
    setError('');

    // Demo credentials per role (kept for the viva walkthrough).
    if (role === 'Admin') {
      setEmail('admin@grow21.com');
      setPassword('admin123');
    } else if (role === 'Teacher') {
      setEmail('ananya@school.com');
      setPassword('pass123');
    } else if (role === 'Parent') {
      setEmail('sunita@gmail.com');
      setPassword('pass123');
    } else {
      setEmail('');
      setPassword('');
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      const data = await response.json();

      if (response.ok) {
        // Enforce role check from DB (optional security layer)
        if (data.user.role !== selectedRole && selectedRole !== null) {
          // In a real app we might throw an error, but here we proceed
        }
        onLogin({ ...data.user, token: data.token });
        navigate('/');
      } else {
        setError(data.message || 'Login failed');
      }
    } catch (err) {
      setError('Cannot connect to server. Please ensure backend is running.');
    }
  };

  const handleForgotPassword = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    
    try {
      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });
      const data = await response.json();
      if (response.ok) {
        setMessage(data.message);
        setTimeout(() => setStep('reset'), 2000); // Simulate email sent -> link clicked -> reset page
      } else {
        setError(data.message || 'Error processing request');
      }
    } catch (err) {
      setError('Cannot connect to server.');
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    
    try {
      const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'}/api/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, newPassword })
      });
      const data = await response.json();
      if (response.ok) {
        setMessage(data.message);
        setTimeout(() => {
          setStep('login');
          setPassword('');
          setMessage('');
        }, 2500);
      } else {
        setError(data.message || 'Error processing request');
      }
    } catch (err) {
      setError('Cannot connect to server.');
    }
  };

  const renderRoleSelection = () => (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh', flexDirection: 'column', position: 'relative' }}>
      <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
        <h1 style={{ fontSize: '3rem', color: 'var(--primary-color)', marginBottom: '0.5rem', fontWeight: 800 }}>Grow21</h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '1.2rem' }}>Who is logging in?</p>
      </div>
      
      <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', justifyContent: 'center' }}>
        {['Parent', 'Teacher', 'Admin'].map(role => (
          <div 
            key={role}
            onClick={() => handleRoleSelect(role)}
            className="card"
            style={{ 
              width: '180px', height: '180px', 
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', transition: 'all 0.2s',
              backgroundColor: 'var(--card-bg)'
            }}
          >
            <div style={{ 
              width: '80px', height: '80px', borderRadius: '50%', 
              backgroundColor: 'rgba(245, 197, 24, 0.1)', 
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              marginBottom: '1rem', border: '2px solid var(--primary-color)'
            }}>
              {role === 'Parent' && <span style={{ fontSize: '2rem' }}>👨‍👩‍👧</span>}
              {role === 'Teacher' && <span style={{ fontSize: '2rem' }}>👩‍🏫</span>}
              {role === 'Admin' && <span style={{ fontSize: '2rem' }}>⚙️</span>}
            </div>
            <span style={{ fontSize: '1.25rem', fontWeight: 600 }}>{role}</span>
          </div>
        ))}
      </div>
    </div>
  );

  const renderLoginForm = () => (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', position: 'relative' }}>
        <button 
          onClick={() => setStep('role')}
          style={{ position: 'absolute', top: '1.5rem', left: '1.5rem', background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
        >
          &larr; Back
        </button>
        
        <div style={{ textAlign: 'center', marginBottom: '2rem', marginTop: '2rem' }}>
          <h2 style={{ marginBottom: '0.5rem' }}>{selectedRole} Login</h2>
        </div>

        {error && <div style={{ backgroundColor: 'rgba(248, 113, 113, 0.1)', color: '#F87171', padding: '0.75rem', borderRadius: '4px', marginBottom: '1.5rem', border: '1px solid rgba(248, 113, 113, 0.2)', textAlign: 'center' }}>{error}</div>}
        {message && <div style={{ backgroundColor: 'rgba(74, 222, 128, 0.1)', color: '#4ADE80', padding: '0.75rem', borderRadius: '4px', marginBottom: '1.5rem', border: '1px solid rgba(74, 222, 128, 0.2)', textAlign: 'center' }}>{message}</div>}
        
        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Email Address</label>
            <input 
              type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
              style={{ width: '100%', padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white', outline: 'none' }}
              placeholder="Enter your email"
            />
          </div>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <label style={{ fontWeight: '500', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Password</label>
              <button type="button" onClick={() => setStep('forgot')} style={{ background: 'none', border: 'none', color: 'var(--primary-color)', fontSize: '0.875rem', cursor: 'pointer', padding: 0 }}>Forgot Password?</button>
            </div>
            <input 
              type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
              style={{ width: '100%', padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white', outline: 'none' }}
              placeholder="Enter your password"
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ marginTop: '1rem', padding: '1rem', fontSize: '1rem', width: '100%' }}>
            Sign In
          </button>
        </form>
      </div>
    </div>
  );

  const renderForgotPassword = () => (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', position: 'relative' }}>
        <button 
          onClick={() => setStep('login')}
          style={{ position: 'absolute', top: '1.5rem', left: '1.5rem', background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
        >
          &larr; Back
        </button>
        
        <div style={{ textAlign: 'center', marginBottom: '2rem', marginTop: '2rem' }}>
          <h2 style={{ marginBottom: '0.5rem' }}>Reset Password</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Enter your email to receive a reset link.</p>
        </div>

        {error && <div style={{ backgroundColor: 'rgba(248, 113, 113, 0.1)', color: '#F87171', padding: '0.75rem', borderRadius: '4px', marginBottom: '1.5rem', border: '1px solid rgba(248, 113, 113, 0.2)', textAlign: 'center' }}>{error}</div>}
        {message && <div style={{ backgroundColor: 'rgba(74, 222, 128, 0.1)', color: '#4ADE80', padding: '0.75rem', borderRadius: '4px', marginBottom: '1.5rem', border: '1px solid rgba(74, 222, 128, 0.2)', textAlign: 'center' }}>{message}</div>}
        
        <form onSubmit={handleForgotPassword} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Email Address</label>
            <input 
              type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
              style={{ width: '100%', padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white', outline: 'none' }}
              placeholder="Enter your email"
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ marginTop: '1rem', padding: '1rem', fontSize: '1rem', width: '100%' }}>
            Send Reset Link
          </button>
        </form>
      </div>
    </div>
  );

  const renderResetPassword = () => (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px', position: 'relative' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem', marginTop: '1rem' }}>
          <h2 style={{ marginBottom: '0.5rem' }}>Create New Password</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>For {email}</p>
        </div>

        {error && <div style={{ backgroundColor: 'rgba(248, 113, 113, 0.1)', color: '#F87171', padding: '0.75rem', borderRadius: '4px', marginBottom: '1.5rem', border: '1px solid rgba(248, 113, 113, 0.2)', textAlign: 'center' }}>{error}</div>}
        {message && <div style={{ backgroundColor: 'rgba(74, 222, 128, 0.1)', color: '#4ADE80', padding: '0.75rem', borderRadius: '4px', marginBottom: '1.5rem', border: '1px solid rgba(74, 222, 128, 0.2)', textAlign: 'center' }}>{message}</div>}
        
        <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: '500', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>New Password</label>
            <input 
              type="password" required value={newPassword} onChange={(e) => setNewPassword(e.target.value)}
              style={{ width: '100%', padding: '0.875rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white', outline: 'none' }}
              placeholder="Enter new password"
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ marginTop: '1rem', padding: '1rem', fontSize: '1rem', width: '100%' }}>
            Update Password
          </button>
        </form>
      </div>
    </div>
  );

  return (
    <>
      {step === 'role' && renderRoleSelection()}
      {step === 'login' && renderLoginForm()}
      {step === 'forgot' && renderForgotPassword()}
      {step === 'reset' && renderResetPassword()}
    </>
  );
};

export default Login;
