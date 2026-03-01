import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { FaCog, FaSave, FaShieldAlt, FaTrashAlt, FaInfoCircle, FaCheck, FaExclamationTriangle, FaChevronLeft } from 'react-icons/fa';
import { fetchWithCsrf } from './api';
import Notification from './Notification';

const SettingRow = ({ setting, onSave }) => {
  const [value, setValue] = useState(setting.value || '');
  const [isSaving, setIsSubmitting] = useState(false);
  const [isModified, setIsModified] = useState(false);

  const handleSave = async () => {
    setIsSubmitting(true);
    const success = await onSave(setting.key, value);
    if (success) {
      setIsModified(false);
    }
    setIsSubmitting(false);
  };

  return (
    <div className="flex flex-col md:flex-row md:items-center justify-between p-6 bg-white rounded-2xl border border-gray-100 group hover:border-indigo-200 transition-all shadow-sm">
      <div className="flex-grow mb-4 md:mb-0">
        <p className="text-[10px] font-black text-indigo-400 uppercase tracking-widest mb-1">{setting.key}</p>
        <p className="text-sm font-bold text-gray-700 mb-1">{setting.description || 'No description available.'}</p>
        <input
          type="text"
          className="w-full md:max-w-md px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all text-sm font-medium"
          value={value}
          onChange={(e) => {
            setValue(e.target.value);
            setIsModified(true);
          }}
        />
      </div>
      <div className="flex items-center justify-end">
        <button
          onClick={handleSave}
          disabled={!isModified || isSaving}
          className={`flex items-center gap-2 px-6 py-2.5 rounded-xl font-black text-xs uppercase tracking-wider transition-all transform active:scale-95 ${
            !isModified 
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
              : 'bg-indigo-600 text-white shadow-lg shadow-indigo-100 hover:bg-indigo-700'
          }`}
        >
          {isSaving ? (
            <div className="animate-spin h-4 w-4 border-2 border-white/30 border-t-white rounded-full"></div>
          ) : (
            <>
              <FaSave />
              Save
            </>
          )}
        </button>
      </div>
    </div>
  );
};

const SettingsPanel = () => {
  const { t } = useTranslation();
  const [settings, setSettings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);

  const fetchSettings = async () => {
    try {
      const response = await fetch('/api/admin/settings');
      if (response.ok) {
        const data = await response.json();
        setSettings(data);
      }
    } catch (err) {
      console.error('Failed to fetch settings', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSettings();
  }, []);

  const handleUpdateSetting = async (key, value) => {
    try {
      const response = await fetchWithCsrf(`/api/admin/settings/${key}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(value),
      });

      if (response.ok) {
        setNotification({ type: 'success', message: t('admin.settings.saveSuccess') });
        return true;
      } else {
        setNotification({ type: 'error', message: t('admin.settings.saveFailure') });
        return false;
      }
    } catch (err) {
      setNotification({ type: 'error', message: t('admin.settings.saveFailure') });
      return false;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-100 border-t-indigo-600"></div>
      </div>
    );
  }

  const groupedSettings = settings.reduce((groups, setting) => {
    let group = 'other';
    if (setting.key.startsWith('security.')) group = 'security';
    else if (setting.key.startsWith('cleanup.')) group = 'cleanup';
    else if (setting.key.startsWith('app.')) group = 'general';
    
    if (!groups[group]) groups[group] = [];
    groups[group].push(setting);
    return groups;
  }, {});

  const groupOrder = ['general', 'security', 'cleanup', 'other'];

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      {notification && (
        <Notification
          type={notification.type}
          message={notification.message}
          onClose={() => setNotification(null)}
        />
      )}

      <div className="animate-slide-in-left mb-12">
        <div className="flex items-center gap-3 mb-2 text-indigo-600">
          <FaCog className="text-xl animate-spin-slow" />
          <Link to="/admin" className="text-xs font-black uppercase tracking-[0.3em] hover:text-indigo-800 transition-colors flex items-center gap-2 group/back">
            <FaChevronLeft className="text-[8px] transform group-hover/back:-translate-x-1 transition-transform" />
            {t('admin.backToDashboard')}
          </Link>
        </div>
        <h1 className="text-4xl font-black text-gray-800 tracking-tighter">
          System <span className="text-indigo-600">Configuration</span>
        </h1>
        <p className="text-gray-400 font-bold mt-2">{t('admin.settings.description')}</p>
      </div>

      <div className="space-y-12">
        {settings.length === 0 ? (
          <div className="bg-white p-12 rounded-[2.5rem] border border-gray-100 shadow-xl text-center">
            <FaInfoCircle className="text-4xl text-gray-200 mx-auto mb-4" />
            <p className="text-gray-400 font-bold italic">{t('admin.settings.noSettings')}</p>
          </div>
        ) : (
          groupOrder.map(groupKey => {
            const groupSettings = groupedSettings[groupKey];
            if (!groupSettings || groupSettings.length === 0) return null;

            return (
              <div key={groupKey} className="animate-fade-in">
                <h2 className="text-xs font-black text-gray-400 uppercase tracking-[0.3em] mb-6 px-4 flex items-center gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-indigo-500"></div>
                  {t(`admin.settings.groups.${groupKey}`)}
                </h2>
                <div className="grid grid-cols-1 gap-4">
                  {groupSettings.map(setting => (
                    <SettingRow 
                      key={setting.key} 
                      setting={setting} 
                      onSave={handleUpdateSetting} 
                    />
                  ))}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default SettingsPanel;
