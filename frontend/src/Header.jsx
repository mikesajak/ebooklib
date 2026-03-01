import React from 'react';
import { Link } from 'react-router-dom';
import LanguageSelector from './LanguageSelector';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import { FaUserCircle, FaSignOutAlt, FaCog } from 'react-icons/fa';

const Header = () => {
  const { t, ready } = useTranslation();
  const { user, isAuthenticated, logout } = useAuth();

  if (!ready) {
    return null;
  }

  return (
    <header className="bg-white border-b border-gray-100 shadow-sm sticky top-0 z-40 px-6 py-3">
      <nav className="container mx-auto max-w-7xl">
        <div className="flex justify-between items-center">
          <Link to="/" className="flex items-center gap-3 group">
            <div className="bg-indigo-50 p-1.5 rounded-lg group-hover:bg-indigo-100 transition-colors">
              <img src="/assets/book.jpg" alt="Logo" className="h-8 w-auto mix-blend-multiply" />
            </div>
            <span className="text-xl font-black text-gray-800 tracking-tight">
              Ebook<span className="text-indigo-600">Lib</span>
            </span>
          </Link>

          <div className="flex items-center gap-6">
            {isAuthenticated && (
              <div className="flex items-center gap-4 bg-gray-50 px-4 py-1.5 rounded-full border border-gray-100 shadow-inner">
                <div className="flex items-center gap-2">
                  <FaUserCircle className="text-gray-400 text-lg" />
                  <span className="text-sm font-bold text-gray-700">
                    {user?.username}
                  </span>
                </div>
                <div className="w-px h-4 bg-gray-200"></div>
                <Link
                  to="/admin/settings"
                  className="p-2 text-gray-400 hover:text-indigo-600 transition-colors"
                  title={t('admin.settings.title')}
                >
                  <FaCog className="text-lg hover:rotate-90 transition-transform duration-500" />
                </Link>
                <div className="w-px h-4 bg-gray-200"></div>
                <button
                  onClick={logout}
                  className="flex items-center gap-1.5 text-xs font-black text-red-500 hover:text-red-700 uppercase tracking-wider transition-colors"
                  title={t('auth.logoutButton')}
                >
                  <FaSignOutAlt />
                  <span className="hidden sm:inline">{t('auth.logoutButton')}</span>
                </button>
              </div>
            )}
            <div className="bg-gray-50 rounded-xl p-1 border border-gray-100">
              <LanguageSelector />
            </div>
          </div>
        </div>
      </nav>
    </header>
  );
};

export default Header;
