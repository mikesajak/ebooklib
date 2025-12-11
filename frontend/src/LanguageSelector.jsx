import React from 'react';
import { useTranslation } from 'react-i18next';

const LanguageSelector = () => {
  const { i18n, t, ready } = useTranslation();

  if (!ready) {
    return null; // Render nothing or a loading indicator until translations are ready
  }

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  return (
    <div className="flex items-center space-x-2">
      <button
        onClick={() => changeLanguage('en')}
        className={`px-3 py-1 rounded-md text-sm font-medium ${i18n.language === 'en' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
      >
        {t('common.language.en')}
      </button>
      <button
        onClick={() => changeLanguage('pl')}
        className={`px-3 py-1 rounded-md text-sm font-medium ${i18n.language === 'pl' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'}`}
      >
        {t('common.language.pl')}
      </button>
    </div>
  );
};

export default LanguageSelector;
