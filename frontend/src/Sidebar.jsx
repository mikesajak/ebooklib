import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';
import { Sidebar, Menu, MenuItem, SubMenu } from 'react-pro-sidebar';
import { 
  FaBook, FaUsers, FaLayerGroup, FaFileImport, FaShieldAlt, 
  FaBars, FaUserCog, FaCog, FaTools, FaHdd, FaChevronRight, FaSyncAlt
} from 'react-icons/fa';
import { useAuth } from './AuthContext';
import { useImport } from './ImportContext';
import { useNavigate } from 'react-router-dom';

const ImportStatusWidget = () => {
  const { t } = useTranslation();
  const { sessions, scanStats } = useImport();
  const navigate = useNavigate();

  const isScanRunning = scanStats?.status === 'RUNNING' || scanStats?.status === 'PURGING';
  const hasActiveSessions = sessions.length > 0;

  if (!isScanRunning && !hasActiveSessions) return null;

  return (
    <div className="space-y-4 mb-6 animate-fade-in">
      <div className="flex items-center justify-between text-[10px] font-black text-indigo-400 uppercase tracking-widest border-b border-indigo-50/50 pb-1">
        <span className="flex items-center gap-1"><FaSyncAlt className="animate-spin-slow" /> {t('import.activeSessions')}</span>
      </div>

      {isScanRunning && (
        <div 
          onClick={() => navigate('/admin/maintenance')}
          className="bg-white p-3 rounded-2xl border border-indigo-100 shadow-sm hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex justify-between items-center mb-2">
            <span className="text-[9px] font-black text-gray-500 uppercase tracking-tighter">{t('admin.maintenance.scan.sidebarTitle')}</span>
            <span className="text-[10px] font-black text-indigo-600">{scanStats.progressPercent}%</span>
          </div>
          <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden border border-gray-50 relative">
            {scanStats.status === 'RUNNING' ? (
              <div className="absolute inset-0 bg-indigo-600/20">
                <div className="h-full bg-indigo-600 w-1/3 rounded-full animate-indeterminate-progress"></div>
              </div>
            ) : (
              <div 
                className="h-full bg-indigo-600 rounded-full transition-all duration-500"
                style={{ width: `${scanStats.progressPercent}%` }}
              ></div>
            )}
          </div>
        </div>
      )}

      {sessions.map(session => {
        const progress = session.totalFiles > 0 
          ? Math.round(((session.processedFiles + session.failedFiles) * 100) / session.totalFiles)
          : 0;
        
        return (
          <div 
            key={session.id}
            onClick={() => navigate(`/import/session/${session.id}`)}
            className="bg-white p-3 rounded-2xl border border-indigo-100 shadow-sm hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="flex justify-between items-center mb-2">
              <span className="text-[9px] font-black text-gray-400 uppercase tracking-tighter truncate max-w-[100px]">Session {session.id.substring(0, 8)}</span>
              <span className="text-[10px] font-black text-indigo-600">{progress}%</span>
            </div>
            <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden border border-gray-50">
              <div 
                className="h-full bg-indigo-600 rounded-full transition-all duration-500"
                style={{ width: `${progress}%` }}
              ></div>
            </div>
            <div className="mt-2 flex justify-between text-[8px] font-bold uppercase tracking-widest text-gray-400">
              <span>{session.processedFiles} {t('common.done', 'Done')}</span>
              {session.failedFiles > 0 && <span className="text-rose-500">{session.failedFiles} {t('common.failed', 'Failed')}</span>}
            </div>
          </div>
        );
      })}
    </div>
  );
};

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

  const adminSubItems = [
    { path: '/admin/users', icon: FaUserCog, label: t('admin.users.title') },
    { path: '/admin/settings', icon: FaCog, label: t('admin.settings.title') },
    { path: '/admin/maintenance', icon: FaTools, label: t('admin.maintenance.title') },
  ];

  const isActive = (path) => {
    if (path === '/' && location.pathname === '/') return true;
    if (path !== '/' && location.pathname.startsWith(path)) return true;
    return false;
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

        <div className="flex-grow overflow-y-auto custom-scrollbar">
          <Menu
            menuItemStyles={{
              button: ({ active, level }) => {
                const getActiveColor = () => {
                  if (!active) return '#9ca3af';
                  const path = location.pathname;
                  if (path.startsWith('/authors')) return '#10b981'; // emerald-600
                  if (path.startsWith('/series')) return '#d97706';  // amber-600
                  if (path.startsWith('/import')) return '#4f46e5';  // indigo-600
                  if (path.startsWith('/admin')) return '#4f46e5';   // indigo-600
                  return '#4f46e5'; // default indigo-600
                };

                const getHoverColor = () => {
                  const path = location.pathname;
                  if (path.startsWith('/authors')) return '#10b981';
                  if (path.startsWith('/series')) return '#d97706';
                  return '#4f46e5';
                };

                const getBgColor = () => {
                  if (!active) return 'transparent';
                  const path = location.pathname;
                  if (path.startsWith('/authors')) return '#ecfdf5'; // emerald-50
                  if (path.startsWith('/series')) return '#fffbeb';  // amber-50
                  return '#f5f3ff'; // indigo-50
                };

                return {
                  color: getActiveColor(),
                  background: getBgColor(),
                  fontWeight: active ? '900' : '700',
                  fontSize: '13px',
                  paddingLeft: level === 0 ? '24px' : '48px',
                  paddingRight: '24px',
                  '&:hover': {
                    background: getBgColor() !== 'transparent' ? getBgColor() : '#f5f3ff',
                    color: getActiveColor() !== '#9ca3af' ? getActiveColor() : '#4f46e5',
                  },
                };
              },
              label: ({ open }) => ({
                fontWeight: open ? '900' : '700',
              }),
            }}
          >
            <div className="px-6 py-2">
              <p className={`text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ${collapsed ? 'hidden' : 'block'}`}>
                {t('header.title', 'Library')}
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
                    {t('admin.oversight', 'Oversight')}
                  </p>
                </div>
                
                <SubMenu
                  label={t('admin.oversight', 'System Oversight')}
                  icon={<FaShieldAlt size={18} />}
                  component={<Link to="/admin" />}
                  active={location.pathname === '/admin'}
                  defaultOpen={location.pathname.startsWith('/admin')}
                >
                  {adminSubItems.map((item) => (
                    <MenuItem
                      key={item.path}
                      component={<Link to={item.path} />}
                      icon={<item.icon size={16} />}
                      active={isActive(item.path)}
                    >
                      {item.label}
                    </MenuItem>
                  ))}
                </SubMenu>
              </>
            )}
          </Menu>
        </div>
        
        {!collapsed && (
          <div className="px-6 pb-8 mt-auto space-y-6">
            <ImportStatusWidget />
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
