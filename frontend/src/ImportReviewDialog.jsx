import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FaCheckCircle, FaExclamationCircle, FaPlusCircle, FaCopy } from 'react-icons/fa';
import MergeMetadataView from './MergeMetadataView';
import { fetchWithCsrf } from './api';

const ImportReviewDialog = ({
  stagedUpload,
  existingBook: initialExistingBook,
  draftBook,
  authorOptions,
  seriesOptions,
  onCancel,
  onConfirm,
  isProcessing
}) => {
  const { t } = useTranslation();
  
  const [activeMatch, setActiveMatch] = useState(initialExistingBook || null);
  const [mergedData, setMergedData] = useState(null);
  const [isLoadingMatch, setIsLoadingMatch] = useState(false);
  const [confirmDuplicate, setConfirmDuplicate] = useState(false);

  const candidates = stagedUpload.validation?.candidates || [];
  
  // Find duplicate flag for current active match
  const isDuplicate = activeMatch && candidates.find(c => c.bookId === activeMatch.id)?.duplicateFormat;

  // When a candidate is clicked, fetch its full details
  const handleSelectCandidate = async (candidate) => {
    if (activeMatch?.id === candidate.bookId) return;
    
    setConfirmDuplicate(false); // Reset duplicate confirmation when switching
    setIsLoadingMatch(true);
    try {
      const response = await fetchWithCsrf(`/api/books/${candidate.bookId}`);
      if (response.ok) {
        const fullBook = await response.json();
        setActiveMatch(fullBook);
      }
    } catch (err) {
      console.error("Failed to fetch candidate details:", err);
    } finally {
      setIsLoadingMatch(false);
    }
  };

  const handleSelectNew = () => {
    setActiveMatch(null);
  };

  const handleConfirm = () => {
    if (mergedData) {
      onConfirm({
        ...mergedData,
        uploadId: stagedUpload.id,
        bookId: activeMatch?.id, // Send selected bookId for update, or null for create
        skipFormatLink: isDuplicate && !confirmDuplicate // Skip file if duplicate but checkbox not checked
      });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-50 backdrop-blur-sm">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-5xl overflow-hidden flex flex-col max-h-[95vh]">
        <header className="px-6 py-4 bg-gray-50 border-b flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-800">
            {activeMatch ? t('import.review.titleUpdate') : t('import.review.titleNew')}
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

        <div className="flex-grow overflow-hidden flex flex-col md:flex-row">
          {/* Left Panel: Candidate Selection (if not targeted) */}
          {!initialExistingBook && candidates.length > 0 && (
            <div className="w-full md:w-1/3 bg-gray-50 border-r border-gray-200 overflow-y-auto p-4">
              <h3 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-4">
                {t('import.review.statusTitle')}
              </h3>
              
              <div className="space-y-3">
                <button
                  onClick={handleSelectNew}
                  className={`w-full text-left p-3 rounded-lg border-2 transition-all flex items-start gap-3
                    ${!activeMatch ? 'border-blue-500 bg-blue-50 shadow-sm' : 'border-gray-200 bg-white hover:border-gray-300'}`}
                >
                  <FaPlusCircle className={`mt-1 ${!activeMatch ? 'text-blue-600' : 'text-gray-400'}`} />
                  <div>
                    <div className="font-bold text-gray-800">{t('import.review.titleNew')}</div>
                    <div className="text-xs text-gray-500">Create a fresh entry in the library</div>
                  </div>
                </button>

                <div className="relative py-2">
                  <div className="absolute inset-0 flex items-center"><span className="w-full border-t border-gray-200"></span></div>
                  <div className="relative flex justify-center text-xs uppercase text-gray-400 bg-gray-50 px-2">Matches Found</div>
                </div>

                {candidates.map(c => (
                  <button
                    key={c.bookId}
                    onClick={() => handleSelectCandidate(c)}
                    className={`w-full text-left p-3 rounded-lg border-2 transition-all flex items-start gap-3
                      ${activeMatch?.id === c.bookId ? 'border-green-500 bg-green-50 shadow-sm' : 'border-gray-200 bg-white hover:border-gray-300'}`}
                  >
                    {c.score >= 80 ? (
                      <FaCheckCircle className={`mt-1 ${activeMatch?.id === c.bookId ? 'text-green-600' : 'text-green-400'}`} />
                    ) : (
                      <FaExclamationCircle className={`mt-1 ${activeMatch?.id === c.bookId ? 'text-yellow-600' : 'text-yellow-400'}`} />
                    )}
                    <div className="overflow-hidden">
                      <div className="font-bold text-gray-800 truncate" title={c.title}>{c.title}</div>
                      <div className="text-xs text-gray-500 truncate">{c.authors.join(', ') || 'Unknown Author'}</div>
                      <div className="mt-1 text-[10px] font-mono text-gray-400 uppercase">Match Score: {c.score}%</div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Right Panel: Metadata Merge */}
          <div className={`flex-grow p-6 overflow-y-auto ${isLoadingMatch ? 'opacity-50 pointer-events-none' : ''}`}>
            <div className="mb-4 text-sm text-gray-600">
              <p>{t('import.review.intro')}</p>
              <p className="mt-1 font-medium">{t('import.review.fileName')}: <span className="font-mono text-blue-600">{stagedUpload.fileName}</span></p>
            </div>

            {isDuplicate && (
              <div className="mb-6 p-4 bg-red-50 border-2 border-red-200 rounded-lg animate-pulse">
                <div className="flex items-start gap-3">
                  <FaCopy className="text-red-600 text-xl mt-1" />
                  <div>
                    <h4 className="font-bold text-red-800 uppercase text-xs tracking-wider">Potential Duplicate Detected</h4>
                    <p className="text-sm text-red-700">
                      This book already has a format with the same file name or size. Adding it again might create a duplicate.
                    </p>
                    <label className="mt-3 flex items-center gap-2 cursor-pointer group">
                      <input 
                        type="checkbox" 
                        checked={confirmDuplicate} 
                        onChange={(e) => setConfirmDuplicate(e.target.checked)}
                        className="w-4 h-4 text-red-600 border-red-300 rounded focus:ring-red-500"
                      />
                      <span className="text-sm font-bold text-red-800 group-hover:text-red-900 transition-colors">
                        Yes, I am sure I want to add this potential duplicate
                      </span>
                    </label>
                  </div>
                </div>
              </div>
            )}

            <MergeMetadataView 
              stagedUpload={stagedUpload}
              existingBook={activeMatch}
              draftBook={draftBook}
              authorOptions={authorOptions}
              seriesOptions={seriesOptions}
              onMergedDataChange={setMergedData}
            />
          </div>
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
            className={`px-6 py-2 text-white rounded font-bold shadow-md transition-all disabled:opacity-50 disabled:cursor-not-allowed
              ${activeMatch ? 'bg-green-600 hover:bg-green-700' : 'bg-blue-600 hover:bg-blue-700'}`}
            disabled={isProcessing || !mergedData || isLoadingMatch}
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
              activeMatch 
                ? (isDuplicate && !confirmDuplicate ? "Update Metadata Only" : t('import.review.confirmUpdate'))
                : t('import.review.confirmCreate')
            )}
          </button>
        </footer>
      </div>
    </div>
  );
};

export default ImportReviewDialog;
