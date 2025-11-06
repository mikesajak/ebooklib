import React from 'react';
import { Link } from 'react-router-dom';
import LanguageSelector from './LanguageSelector';
import { useTranslation } from 'react-i18next';

const Header = () => {
  const { t } = useTranslation();

  return (
    <header style={{backgroundColor: 'white', color: 'black', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', padding: '1rem 2rem'}}>
      <nav style={{}}>
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
          <h1 style={{fontSize: '1.25rem', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
            <img src="/assets/book.jpg" alt="Ebook Library Icon" style={{height: '2em', width: 'auto'}} />
            {t('header.title')}
          </h1>
          <div style={{display: 'flex', gap: '1rem', alignItems: 'center'}}>
            <LanguageSelector />
          </div>
        </div>
      </nav>
    </header>
  );
};

export default Header;
