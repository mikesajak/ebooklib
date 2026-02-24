import React from 'react';
import { Link } from 'react-router-dom';
import LanguageSelector from './LanguageSelector';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const Header = () => {
  const { t, ready } = useTranslation();
  const { user, isAuthenticated, logout } = useAuth();

  if (!ready) {
    return null; // Or a loading spinner
  }

  return (
    <header style={{backgroundColor: 'white', color: 'black', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', padding: '1rem 2rem'}}>
      <nav style={{}}>
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
          <h1 style={{fontSize: '1.25rem', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
            <img src="/assets/book.jpg" alt="Ebook Library Icon" style={{height: '2em', width: 'auto'}} />
            {t('header.title')}
          </h1>
          <div style={{display: 'flex', gap: '1rem', alignItems: 'center'}}>
            {isAuthenticated && (
              <div style={{display: 'flex', gap: '1rem', alignItems: 'center', marginRight: '1rem'}}>
                <span style={{fontSize: '0.875rem', fontWeight: '500', color: '#4b5563'}}>
                  {user?.username}
                </span>
                <button
                  onClick={logout}
                  style={{
                    fontSize: '0.875rem',
                    fontWeight: '600',
                    color: '#dc2626',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: '0.25rem 0.5rem',
                    borderRadius: '0.25rem'
                  }}
                  onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#fee2e2'}
                  onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                >
                  {t('auth.logoutButton')}
                </button>
              </div>
            )}
            <LanguageSelector />
          </div>
        </div>
      </nav>
    </header>
  );
};

export default Header;
