import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import MergeMetadataView from './MergeMetadataView';

const ImportReviewDialog = ({
  stagedUpload,
  existingBook,
  authorOptions,
  seriesOptions,
  onCancel,
  onConfirm,
  isProcessing
}) => {
  const { t } = useTranslation();
  const [mergedData, setMergedData] = useState(null);

  const handleConfirm = () => {
    if (mergedData) {
      onConfirm({
        ...mergedData,
        uploadId: stagedUpload.id
      });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50 backdrop-blur-sm">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl overflow-hidden flex flex-col max-h-[90vh]">
        <header className="px-6 py-4 bg-gray-50 border-b flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-800">
            {existingBook ? t('import.review.titleUpdate') : t('import.review.titleNew')}
          </h2>
          <button 
            onClick={onCancel}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            disabled={isProcessing}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div className="flex-grow overflow-hidden p-6">
          <div className="mb-4 text-sm text-gray-600">
            <p>{t('import.review.intro')}</p>
            <p className="mt-1 font-medium">{t('import.review.fileName')}: <span className="font-mono text-blue-600">{stagedUpload.fileName}</span></p>
          </div>

          <MergeMetadataView 
            stagedUpload={stagedUpload}
            existingBook={existingBook}
            authorOptions={authorOptions}
            seriesOptions={seriesOptions}
            onMergedDataChange={setMergedData}
          />
        </div>

        <footer className="px-6 py-4 bg-gray-50 border-t flex justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-gray-700 bg-white border border-gray-300 rounded hover:bg-gray-50 font-medium transition-colors"
            disabled={isProcessing}
          >
            {t('common.cancel')}
          </button>
          <button
            onClick={handleConfirm}
            className="px-6 py-2 text-white bg-blue-600 rounded hover:bg-blue-700 font-bold shadow-md transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={isProcessing || !mergedData}
          >
            {isProcessing ? (
              <div className="flex items-center gap-2">
                <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                {t('import.review.processing')}
              </div>
            ) : (
              existingBook ? t('import.review.confirmUpdate') : t('import.review.confirmCreate')
            )}
          </button>
        </footer>
      </div>
    </div>
  );
};

export default ImportReviewDialog;
