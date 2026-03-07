import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';
import { Sidebar, Menu, MenuItem } from 'react-pro-sidebar';
import { 
  FaBook, FaUsers, FaLayerGroup, FaFileImport, FaShieldAlt, 
  FaBars, FaUserCog, FaCog, FaTools, FaHdd, FaChevronRight 
} from 'react-icons/fa';
import { useAuth } from './AuthContext';

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
  }, []);

  if (loading && !stats) return null;

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between text-[10px] font-black text-indigo-400 uppercase tracking-widest border-b border-indigo-50/50 pb-1 mb-2">
        <span className="flex items-center gap-1"><FaShieldAlt /> {t('admin.stats.title')}</span>
        <Link to="/admin" className="hover:text-indigo-600 transition-colors">{t('common.viewAll')}</Link>
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
    </div>
  );
};

const AppSidebar = ({ collapsed, setCollapsed }) => {
  const { t } = useTranslation();
  const { isAdmin } = useAuth();
  const location = useLocation();
  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleStatusChange = () => setIsOnline(navigator.onLine);
    window.addEventListener('online', handleStatusChange);
    window.addEventListener('offline', handleStatusChange);
    return () => {
      window.removeEventListener('online', handleStatusChange);
      window.removeEventListener('offline', handleStatusChange);
    };
  }, []);

  const menuItems = [
    { path: '/', icon: FaBook, label: t('header.books') },
    { path: '/authors', icon: FaUsers, label: t('header.authors') },
    { path: '/series', icon: FaLayerGroup, label: t('header.series') },
    { path: '/import', icon: FaFileImport, label: t('header.import') },
  ];

  const adminItems = [
    { path: '/admin/users', icon: FaUserCog, label: t('admin.users.title') },
    { path: '/admin/settings', icon: FaCog, label: t('admin.settings.title') },
    { path: '/admin/maintenance', icon: FaTools, label: t('admin.maintenance.title') },
  ];

  const isActive = (path) => location.pathname === path;

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

        <div className="flex-grow overflow-y-auto custom-scrollbar">
          <Menu
            menuItemStyles={{
              button: ({ active }) => ({
                color: active ? '#4f46e5' : '#9ca3af',
                background: active ? '#f5f3ff' : 'transparent',
                fontWeight: active ? '900' : '700',
                fontSize: '13px',
                paddingLeft: '24px',
                paddingRight: '24px',
                '&:hover': {
                  background: '#f5f3ff',
                  color: '#4f46e5',
                },
              }),
            }}
          >
            <div className="px-6 py-2">
              <p className={`text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ${collapsed ? 'hidden' : 'block'}`}>
                Library
              </p>
            </div>
            {menuItems.map((item) => (
              <MenuItem
                key={item.path}
                component={<Link to={item.path} />}
                icon={<item.icon size={18} />}
                active={isActive(item.path)}
              >
                {item.label}
              </MenuItem>
            ))}

            {isAdmin && (
              <>
                <div className="px-6 py-6 pb-2">
                  <p className={`text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ${collapsed ? 'hidden' : 'block'}`}>
                    {t('admin.dashboard')}
                  </p>
                </div>
                {adminItems.map((item) => (
                  <MenuItem
                    key={item.path}
                    component={<Link to={item.path} />}
                    icon={<item.icon size={18} />}
                    active={isActive(item.path)}
                  >
                    {item.label}
                  </MenuItem>
                ))}
              </>
            )}
          </Menu>
        </div>
        
        {!collapsed && (
          <div className="px-6 pb-8 mt-auto space-y-6">
            {isAdmin && <AdminStatusWidget />}

            <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 shadow-sm">
              <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-1">{t('admin.maintenance.system')}</p>
              <div className="flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${isOnline ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
                <span className="text-[10px] font-black text-gray-700 uppercase tracking-tight">{isOnline ? t('common.online') : t('common.offline')}</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </Sidebar>
  );
};

export default AppSidebar;
