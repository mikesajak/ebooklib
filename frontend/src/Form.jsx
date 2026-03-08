import React from 'react';
import { useTranslation } from 'react-i18next';

const Form = ({ children, onSave, onCancel, isSaveDisabled, color = 'indigo' }) => {
  const { t } = useTranslation();

  const buttonColorMap = {
    indigo: 'bg-indigo-600 shadow-indigo-100 hover:bg-indigo-700',
    emerald: 'bg-emerald-600 shadow-emerald-100 hover:bg-emerald-700',
    amber: 'bg-amber-600 shadow-amber-100 hover:bg-amber-700',
    violet: 'bg-violet-600 shadow-violet-100 hover:bg-violet-700'
  };

  return (
    <>
      {children}
      <div className="flex justify-end mt-12 gap-4">
        <button 
          onClick={onSave} 
          disabled={isSaveDisabled} 
          className={`px-8 py-3 rounded-xl font-black text-sm uppercase tracking-wider transition-all transform active:scale-95 shadow-xl ${
            isSaveDisabled 
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none' 
              : `${buttonColorMap[color]} text-white hover:-translate-y-1`
          }`}
        >
          {t('common.save')}
        </button>
        <button 
          onClick={onCancel} 
          className="px-8 py-3 rounded-xl font-black text-sm uppercase tracking-wider bg-white text-gray-500 border border-gray-200 hover:bg-gray-50 hover:text-gray-700 transition-all transform active:scale-95 shadow-sm"
        >
          {t('common.cancel')}
        </button>
      </div>
    </>
  );
};

export default Form;
