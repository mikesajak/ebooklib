import React, { useState, useEffect } from 'react';
import { Sidebar, Menu, MenuItem, useProSidebar } from 'react-pro-sidebar';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FaBook, FaUsers, FaLayerGroup, FaBars } from 'react-icons/fa';

const AppSidebar = () => {
  const { t } = useTranslation();
  const { collapseSidebar, collapsed } = useProSidebar();
  const location = useLocation();
  const [isOnline, setIsOnline] = useState(true);

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const response = await fetch('/actuator/health');
        if (response.ok) {
          const data = await response.json();
          setIsOnline(data.status === 'UP');
        } else {
          setIsOnline(false);
        }
      } catch (err) {
        setIsOnline(false);
      }
    };

    checkHealth(); // Initial check
    const interval = setInterval(checkHealth, 30000); // Check every 30 seconds
    return () => clearInterval(interval);
  }, []);

  const getActiveTheme = (path) => {
    const currentPath = location.pathname;
    if (path === '/' && (currentPath === '/' || currentPath.startsWith('/book/'))) {
      return { active: true, bg: '#e0e7ff', text: '#3730a3', border: '#4f46e5' }; // Indigo
    }
    if (path === '/authors' && (currentPath.startsWith('/authors') || currentPath.startsWith('/author/'))) {
      return { active: true, bg: '#d1fae5', text: '#065f46', border: '#059669' }; // Emerald
    }
    if (path === '/series' && (currentPath.startsWith('/series'))) {
      return { active: true, bg: '#fef3c7', text: '#92400e', border: '#d97706' }; // Amber
    }
    return { active: false };
  };

  const getMenuItemStyles = (path) => {
    const theme = getActiveTheme(path);
    if (theme.active) {
      return {
        backgroundColor: theme.bg,
        color: theme.text,
        borderRight: `4px solid ${theme.border}`,
        fontWeight: 'bold'
      };
    }
    return {};
  };

  const menuItemStyles = {
    root: { fontSize: '0.9rem', fontWeight: 500 },
    icon: { color: '#6b7280' },
    button: { '&:hover': { backgroundColor: '#f9fafb', color: '#111827' } },
  };

  return (
    <Sidebar backgroundColor="#fff" rootStyles={{ borderRight: '1px solid #f3f4f6', height: '100%' }}>
      <div className="flex flex-col h-full overflow-hidden">
        <div className="p-6 flex items-center justify-center border-b border-gray-50 mb-4">
          <button onClick={() => collapseSidebar()} className="text-gray-400 hover:text-indigo-600 transition-colors">
            <FaBars className="text-xl" />
          </button>
          {!collapsed && (
            <span className="ml-3 font-black text-gray-800 tracking-tighter text-xl">
              Ebook<span className="text-indigo-600">Lib</span>
            </span>
          )}
        </div>

        <div className="flex-grow">
          <Menu menuItemStyles={menuItemStyles}>
            <MenuItem
              component={<Link to="/" />}
              icon={<FaBook className={getActiveTheme('/').active ? 'text-indigo-600' : ''} />}
              style={getMenuItemStyles('/')}
            >
              {t('header.books')}
            </MenuItem>
            
            <MenuItem
              component={<Link to="/authors" />} 
              icon={<FaUsers className={getActiveTheme('/authors').active ? 'text-emerald-600' : ''} />}
              style={getMenuItemStyles('/authors')}
            >
              {t('header.authors')}
            </MenuItem>
            
            <MenuItem
              component={<Link to="/series" />} 
              icon={<FaLayerGroup className={getActiveTheme('/series').active ? 'text-amber-600' : ''} />}
              style={getMenuItemStyles('/series')}
            >
              {t('header.series')}
            </MenuItem>
          </Menu>
        </div>
        
        {!collapsed && (
          <div className="px-6 pb-8 mt-auto">
            <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">Status</p>
              <div className="flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${isOnline ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
                <span className="text-xs font-bold text-gray-700 uppercase">{isOnline ? 'Online' : 'Offline'}</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </Sidebar>
  );
};

export default AppSidebar;
