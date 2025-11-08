import React from 'react';
import { useTranslation } from 'react-i18next';

const ConfirmationDialog = ({
  title,
  message,
  onCancel,
  onConfirm,
  confirmButtonText,
  cancelButtonText,
}) => {
  const { t } = useTranslation();

  return (
    <div className="confirmation-dialog">
      <div className="confirmation-dialog-content">
        <h2>{title}</h2>
        <p>{message}</p>
        <div className="confirmation-dialog-actions">
          <button onClick={onCancel}>{cancelButtonText || t('common.cancel')}</button>
          <button onClick={onConfirm} className="delete-button">
            {confirmButtonText || t('common.delete')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationDialog;
