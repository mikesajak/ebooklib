import React from 'react';
import { useTranslation } from 'react-i18next';

const Form = ({ children, onSave, onCancel, isSaveDisabled }) => {
  const { t } = useTranslation();

  return (
    <>
      {children}
      <div className="flex justify-end mt-4">
        <button onClick={onSave} disabled={isSaveDisabled} className={`font-bold py-2 px-4 rounded mr-2 ${isSaveDisabled ? 'bg-gray-400 text-white cursor-not-allowed' : 'bg-green-500 hover:bg-green-700 text-white'}`}>
          {t('common.save')}
        </button>
        <button onClick={onCancel} className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded">
          {t('common.cancel')}
        </button>
      </div>
    </>
  );
};

export default Form;
