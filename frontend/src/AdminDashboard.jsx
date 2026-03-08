import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { FaShieldAlt, FaBook, FaUsers, FaLayerGroup, FaFileArchive, FaImage, FaHdd, FaUserCog, FaTools, FaChevronRight, FaCog } from 'react-icons/fa';

const StatCard = ({ title, value, icon: Icon, colorClass, delay = "animate-fade-in" }) => (
  <div className={`bg-white p-6 rounded-3xl border border-gray-100 shadow-xl shadow-gray-100/50 flex flex-col gap-4 relative overflow-hidden group ${delay}`}>
    <div className={`absolute -right-4 -bottom-4 opacity-5 transform group-hover:scale-110 group-hover:rotate-12 transition-all duration-500`}>
      <Icon size={120} />
    </div>
    <div className="flex items-center justify-between relative z-10">
      <div className={`p-3 rounded-2xl ${colorClass} bg-opacity-10 text-xl`}>
        <Icon className={colorClass.replace('bg-', 'text-')} />
      </div>
    </div>
    <div className="relative z-10">
      <p className="text-xs font-black text-gray-400 uppercase tracking-widest mb-1">{title}</p>
      <p className="text-3xl font-black text-gray-800 tracking-tight">{value}</p>
    </div>
  </div>
);

const AdminDashboard = () => {
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

  const formatSize = (bytes) => {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-violet-100 border-t-violet-600"></div>
        <p className="text-sm font-bold text-gray-400 uppercase tracking-widest">{t('common.loading')}</p>
      </div>
    );
  }

  const totalSize = (stats?.totalFormatSize || 0) + (stats?.totalCoverSize || 0);

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-12">
        <div className="animate-slide-in-left">
          <div className="flex items-center gap-3 mb-2 text-violet-600">
            <FaShieldAlt className="text-xl" />
            <span className="text-xs font-black uppercase tracking-[0.3em]">{t('admin.dashboard')}</span>
          </div>
          <h1 className="text-4xl font-black text-gray-800 tracking-tighter">
            {t('admin.title', 'System')} <span className="text-violet-600">{t('admin.dashboard', 'Oversight')}</span>
          </h1>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
        <StatCard 
          title={t('admin.stats.books')} 
          value={stats?.bookCount || 0} 
          icon={FaBook} 
          colorClass="bg-indigo-500" 
          delay="animate-fade-in"
        />
        <StatCard 
          title={t('admin.stats.authors')} 
          value={stats?.authorCount || 0} 
          icon={FaUsers} 
          colorClass="bg-emerald-500" 
          delay="animate-fade-in [animation-delay:100ms]"
        />
        <StatCard 
          title={t('admin.stats.series')} 
          value={stats?.seriesCount || 0} 
          icon={FaLayerGroup} 
          colorClass="bg-amber-500" 
          delay="animate-fade-in [animation-delay:200ms]"
        />
        <StatCard 
          title={t('admin.stats.totalSize')} 
          value={formatSize(totalSize)} 
          icon={FaHdd} 
          colorClass="bg-violet-500" 
          delay="animate-fade-in [animation-delay:300ms]"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
        <div className="lg:col-span-2 space-y-8">
          <div className="bg-white rounded-[2.5rem] border border-gray-100 shadow-2xl shadow-gray-200/50 p-8 animate-fade-in [animation-delay:400ms]">
            <h2 className="text-xl font-black text-gray-800 tracking-tight mb-8 flex items-center gap-3">
              <FaHdd className="text-violet-500" /> {t('admin.stats.storageBreakdown')}
            </h2>
            
            <div className="space-y-6">
              <div>
                <div className="flex justify-between items-center mb-2">
                  <span className="text-xs font-black text-gray-500 uppercase tracking-widest flex items-center gap-2">
                    <FaFileArchive className="text-indigo-500" /> {t('admin.stats.formats')}
                  </span>
                  <span className="text-sm font-black text-gray-700">{formatSize(stats?.totalFormatSize)}</span>
                </div>
                <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-indigo-500 rounded-full transition-all duration-1000" 
                    style={{ width: totalSize > 0 ? `${(stats.totalFormatSize / totalSize) * 100}%` : '0%' }}
                  ></div>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-2">
                  <span className="text-xs font-black text-gray-500 uppercase tracking-widest flex items-center gap-2">
                    <FaImage className="text-violet-500" /> {t('admin.stats.covers')}
                  </span>
                  <span className="text-sm font-black text-gray-700">{formatSize(stats?.totalCoverSize)}</span>
                </div>
                <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-violet-500 rounded-full transition-all duration-1000" 
                    style={{ width: totalSize > 0 ? `${(stats.totalCoverSize / totalSize) * 100}%` : '0%' }}
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <h2 className="text-xs font-black text-gray-400 uppercase tracking-[0.3em] px-4 animate-fade-in [animation-delay:500ms]">
            {t('admin.quickActions')}
          </h2>
          
          <Link to="/admin/users" className="block group animate-fade-in [animation-delay:600ms]">
            <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-xl shadow-gray-100/50 flex items-center justify-between group-hover:border-violet-200 transition-all group-hover:-translate-y-1">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-2xl bg-violet-50 text-violet-600 group-hover:bg-violet-600 group-hover:text-white transition-all">
                  <FaUserCog size={20} />
                </div>
                <div>
                  <p className="text-sm font-black text-gray-800 tracking-tight">{t('admin.users.title')}</p>
                  <p className="text-[10px] text-gray-400 font-bold leading-tight">{t('admin.users.description')}</p>
                </div>
              </div>
              <FaChevronRight className="text-gray-300 group-hover:text-violet-600 transition-colors" />
            </div>
          </Link>

          <Link to="/admin/settings" className="block group animate-fade-in [animation-delay:700ms]">
            <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-xl shadow-gray-100/50 flex items-center justify-between group-hover:border-violet-200 transition-all group-hover:-translate-y-1">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-2xl bg-violet-50 text-violet-600 group-hover:bg-violet-600 group-hover:text-white transition-all">
                  <FaCog size={20} />
                </div>
                <div>
                  <p className="text-sm font-black text-gray-800 tracking-tight">{t('admin.settings.title')}</p>
                  <p className="text-[10px] text-gray-400 font-bold leading-tight">{t('admin.settings.description')}</p>
                </div>
              </div>
              <FaChevronRight className="text-gray-300 group-hover:text-violet-600 transition-colors" />
            </div>
          </Link>

          <Link to="/admin/maintenance" className="block group animate-fade-in [animation-delay:800ms]">
            <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-xl shadow-gray-100/50 flex items-center justify-between group-hover:border-violet-200 transition-all group-hover:-translate-y-1">
              <div className="flex items-center gap-4">
                <div className="p-3 rounded-2xl bg-violet-50 text-violet-600 group-hover:bg-violet-600 group-hover:text-white transition-all">
                  <FaTools size={20} />
                </div>
                <div>
                  <p className="text-sm font-black text-gray-800 tracking-tight">{t('admin.maintenance.title')}</p>
                  <p className="text-[10px] text-gray-400 font-bold leading-tight">{t('admin.maintenance.description')}</p>
                </div>
              </div>
              <FaChevronRight className="text-gray-300 group-hover:text-violet-600 transition-colors" />
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
