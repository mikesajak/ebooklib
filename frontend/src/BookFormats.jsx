import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import ConfirmationDialog from './ConfirmationDialog';
import ImportReviewDialog from './ImportReviewDialog';
import { fetchWithCsrf } from './api';

const BookFormats = ({ book, showNotification, onRefreshRequested }) => {
  const { t } = useTranslation();
  const bookId = book.id;
  const [formats, setFormats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);
  const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);
  const [formatToDelete, setFormatToDelete] = useState(null);

  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [stagedUpload, setStagedUpload] = useState(null);
  const [isFinalizing, setIsFinalizing] = useState(false);

  const [authors, setAuthors] = useState([]);
  const [series, setSeries] = useState([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [authorsResponse, seriesResponse] = await Promise.all([
          fetchWithCsrf('/api/authors?size=1000&sort=firstName,asc&sort=lastName,asc'),
          fetchWithCsrf('/api/series?size=1000&sort=title,asc')
        ]);

        if (authorsResponse.ok) {
          const data = await authorsResponse.json();
          setAuthors(data.content || []);
        }
        if (seriesResponse.ok) {
          const data = await seriesResponse.json();
          setSeries(data.content || []);
        }
      } catch (err) {
        console.error("Failed to fetch reference data:", err);
      }
    };
    fetchData();
  }, []);

  const authorOptions = useMemo(() => 
    authors.map(a => ({ id: a.id, name: `${a.firstName} ${a.lastName}` })), 
    [authors]
  );

  const seriesOptions = useMemo(() => 
    series.map(s => ({ id: s.id, name: s.title })), 
    [series]
  );

  const fetchFormats = async () => {
    try {
      const response = await fetchWithCsrf(`/api/books/${bookId}/formats`);
      if (!response.ok) {
        throw new Error('Failed to fetch book formats');
      }
      const data = await response.json();
      setFormats(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFormats();
  }, [bookId]);

  const formatBytes = (bytes, decimals = 2) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  };

  const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setUploading(true);
    setProgress(0);

    const formData = new FormData();
    formData.append('file', file);

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `/api/import/upload?currentBookId=${bookId}`);

    // Progress tracking
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        setProgress(Math.round((e.loaded * 100) / e.total));
      }
    };

    xhr.onload = () => {
      setUploading(false);
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const data = JSON.parse(xhr.responseText);
          setStagedUpload(data);
        } catch (e) {
          showNotification({ type: 'error', message: 'Failed to parse upload response' });
        }
      } else {
        let errorMessage = 'Failed to upload file';
        try {
          const errorData = JSON.parse(xhr.responseText);
          errorMessage = errorData.message || errorMessage;
        } catch (e) {}
        showNotification({ type: 'error', message: errorMessage });
      }
    };

    xhr.onerror = () => {
      setUploading(false);
      showNotification({ type: 'error', message: 'Network error occurred during upload.' });
    };

    xhr.send(formData);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleFinalize = async (mergedData) => {
    setIsFinalizing(true);
    try {
      const response = await fetchWithCsrf('/api/import/finalize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...mergedData,
          bookId: bookId // Ensure we are targeting this book
        }),
      });

      if (!response.ok) {
        throw new Error(t('import.finalizeError'));
      }

      showNotification({ type: 'success', message: t('import.success', { title: book.title }) });
      setStagedUpload(null);
      fetchFormats();
      if (onRefreshRequested) onRefreshRequested();
    } catch (err) {
      showNotification({ type: 'error', message: err.message });
    } finally {
      setIsFinalizing(false);
    }
  };

  const handleDeleteClick = (format) => {
    setFormatToDelete(format);
    setShowDeleteConfirmDialog(true);
  };

  const handleConfirmDelete = async () => {
    if (!formatToDelete) return;

    try {
      const response = await fetchWithCsrf(`/api/books/${bookId}/formats/${formatToDelete.id}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error('Failed to delete format');
      }

      showNotification({ type: 'success', message: 'Format deleted successfully!' });
      fetchFormats();
    } catch (err) {
      showNotification({ type: 'error', message: err.message });
    } finally {
      setShowDeleteConfirmDialog(false);
      setFormatToDelete(null);
    }
  };

  return (
    <div className="bg-white border border-gray-300 rounded p-6 shadow">
      <h2 className="text-xl font-bold mb-4">{t('bookFormats.title')}</h2>
      {loading && <p>{t('bookFormats.loading')}</p>}
      {error && <p className="text-red-500">{t('common.error')}: {error}</p>}
      {!loading && !error && formats.length === 0 && <p>{t('bookFormats.noFormats')}</p>}
      {!loading && !error && formats.length > 0 && (
        <ul>
          {formats.map(format => (
            <li key={format.id} className="flex justify-between items-center mb-2">
              <div>
                <a href={`/api/books/${bookId}/formats/${format.id}/download`} className="text-blue-500 hover:underline">
                  {format.formatType}
                </a>
                <span className="text-gray-500 text-sm ml-2">({formatBytes(format.size)})</span>
              </div>
              <button
                onClick={() => handleDeleteClick(format)}
                className="bg-red-500 hover:bg-red-700 text-white font-bold py-1 px-2 rounded text-xs"
              >
                {t('common.delete')}
              </button>
            </li>
          ))}
          {uploading && (
            <li className="flex items-center mb-2">
              <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
                {progress < 100 ? (
                  <div
                    className="bg-blue-600 h-2.5 rounded-full"
                    style={{ width: `${progress}%` }}
                  ></div>
                ) : (
                  <div
                    className="bg-blue-600 h-2.5 rounded-full animate-pulse"
                    style={{ width: '100%' }}
                  ></div>
                )}
              </div>
              <span className="text-sm text-gray-500 ml-2">
                {progress < 100 ? `${progress}%` : t('bookFormats.processing')}
              </span>
            </li>
          )}
        </ul>
      )}
      <div className="flex justify-end mt-4">
        <input
          type="file"
          ref={fileInputRef}
          className="hidden"
          onChange={handleFileChange}
          accept=".epub,.pdf,.mobi,.azw3"
        />
        <button
          type="button"
          onClick={() => fileInputRef.current.click()}
          className={`font-bold py-2 px-4 rounded ${uploading ? 'bg-gray-400 text-white cursor-not-allowed' : 'bg-green-500 hover:bg-green-700 text-white'}`}
          disabled={uploading}
        >
          {t('bookFormats.upload')}
        </button>
      </div>

      {stagedUpload && (
        <ImportReviewDialog
          stagedUpload={stagedUpload}
          existingBook={book}
          authorOptions={authorOptions}
          seriesOptions={seriesOptions}
          onCancel={() => setStagedUpload(null)}
          onConfirm={handleFinalize}
          isProcessing={isFinalizing}
        />
      )}

      {showDeleteConfirmDialog && (
        <ConfirmationDialog
          message={t('bookFormats.deleteConfirmation.message', { formatType: formatToDelete?.formatType })}
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowDeleteConfirmDialog(false)}
        />
      )}
    </div>
  );
};

export default BookFormats;

