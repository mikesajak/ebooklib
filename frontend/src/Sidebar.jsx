import React, { useState, useEffect } from 'react';
import { Sidebar, Menu, MenuItem } from 'react-pro-sidebar';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import { FaBook, FaUsers, FaLayerGroup, FaBars, FaShieldAlt, FaHdd } from 'react-icons/fa';

const AdminStatusWidget = () => {
  const { t } = useTranslation();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await fetch('/api/admin/stats');
        if (response.ok) {
          const data = await response.json();
          setStats(data);
        }
      } catch (err) {
        console.error('Failed to fetch admin stats', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
    const interval = setInterval(fetchStats, 60000); // Refresh every minute
    return () => clearInterval(interval);
  }, []);

  const formatSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  if (loading && !stats) return null;

  const totalSize = (stats?.totalFormatSize || 0) + (stats?.totalCoverSize || 0);

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between text-[10px] font-black text-indigo-400 uppercase tracking-widest border-b border-indigo-50/50 pb-1 mb-2">
        <span className="flex items-center gap-1"><FaShieldAlt /> {t('admin.stats.title')}</span>
        <Link to="/admin" className="hover:text-indigo-600 transition-colors">View All</Link>
      </div>
      
      <div className="grid grid-cols-2 gap-2">
        <div className="bg-white p-2 rounded-xl border border-gray-100 shadow-sm">
          <p className="text-[9px] font-bold text-gray-400 uppercase leading-none mb-1">{t('admin.stats.books')}</p>
          <p className="text-sm font-black text-gray-700 leading-none">{stats?.bookCount || 0}</p>
        </div>
        <div className="bg-white p-2 rounded-xl border border-gray-100 shadow-sm">
          <p className="text-[9px] font-bold text-gray-400 uppercase leading-none mb-1">{t('admin.stats.authors')}</p>
          <p className="text-sm font-black text-gray-700 leading-none">{stats?.authorCount || 0}</p>
        </div>
      </div>

      <div className="bg-indigo-600 p-2.5 rounded-xl shadow-lg shadow-indigo-100 text-white relative overflow-hidden group">
        <div className="absolute -right-2 -bottom-2 opacity-10 transform group-hover:scale-110 transition-transform">
          <FaHdd className="text-4xl" />
        </div>
        <div className="relative z-10">
          <p className="text-[9px] font-black uppercase tracking-widest opacity-80 mb-0.5">{t('admin.stats.totalSize')}</p>
          <p className="text-base font-black tracking-tight">{formatSize(totalSize)}</p>
        </div>
      </div>
    </div>
  );
};

const AppSidebar = () => {
  const { t } = useTranslation();
  const { user, isAuthenticated } = useAuth();
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const [isOnline, setIsOnline] = useState(true);

  const isAdmin = isAuthenticated && (user?.roles?.includes('ADMIN') || user?.roles?.includes('ROLE_ADMIN'));

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

    checkHealth();
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  const getActiveTheme = (path) => {
    const currentPath = location.pathname;
    if (path === '/' && (currentPath === '/' || currentPath.startsWith('/book/'))) {
      return { active: true, bg: '#e0e7ff', text: '#3730a3', border: '#4f46e5' };
    }
    if (path === '/authors' && (currentPath.startsWith('/authors') || currentPath.startsWith('/author/'))) {
      return { active: true, bg: '#d1fae5', text: '#065f46', border: '#059669' };
    }
    if (path === '/series' && (currentPath.startsWith('/series'))) {
      return { active: true, bg: '#fef3c7', text: '#92400e', border: '#d97706' };
    }
    if (path === '/admin' && (currentPath.startsWith('/admin'))) {
      return { active: true, bg: '#ede9fe', text: '#5b21b6', border: '#8b5cf6' };
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
    <Sidebar collapsed={collapsed} backgroundColor="#fff" rootStyles={{ borderRight: '1px solid #f3f4f6', height: '100%' }}>
      <div className="flex flex-col h-full overflow-hidden">
        <div className="p-6 flex items-center justify-center border-b border-gray-50 mb-4">
          <button onClick={() => setCollapsed(!collapsed)} className="text-gray-400 hover:text-indigo-600 transition-colors">
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

            {isAdmin && (
              <MenuItem
                component={<Link to="/admin" />}
                icon={<FaShieldAlt className={getActiveTheme('/admin').active ? 'text-violet-600' : ''} />}
                style={getMenuItemStyles('/admin')}
              >
                {t('admin.dashboard')}
              </MenuItem>
            )}
          </Menu>
        </div>
        
        {!collapsed && (
          <div className="px-6 pb-8 mt-auto space-y-6">
            {isAdmin && <AdminStatusWidget />}

            <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-1">System</p>
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
