import React from 'react';
import { Link } from 'react-router-dom';

const Header = () => {
  return (
    <header style={{backgroundColor: 'white', color: 'black', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', padding: '1rem 2rem'}}>
      <nav style={{maxWidth: '1280px', margin: '0 auto'}}>
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
          <h1 style={{fontSize: '1.25rem', fontWeight: 'bold'}}>Ebook Library</h1>
          <div style={{display: 'flex', gap: '1rem'}}>
            <Link to="/" style={{color: '#2563eb', textDecoration: 'none', fontWeight: '600'}}>
              Books
            </Link>
            <Link to="/authors" style={{color: '#059669', textDecoration: 'none', fontWeight: '600'}}>
              Authors
            </Link>
            <Link to="/series" style={{color: '#d97706', textDecoration: 'none', fontWeight: '600'}}>
              Series
            </Link>
          </div>
        </div>
      </nav>
    </header>
  );
};

export default Header;
