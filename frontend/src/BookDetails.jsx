import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import Notification from './Notification';
import ConfirmationDialog from './ConfirmationDialog';
import BookFormats from './BookFormats';
import { useTranslation } from 'react-i18next';

const BookDetails = () => {
  const { t } = useTranslation();
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [bookCoverUrl, setBookCoverUrl] = useState(null);
  const [hasCover, setHasCover] = useState(false);
  const [showUploadConfirmDialog, setShowUploadConfirmDialog] = useState(false);
  const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [isCoverFileMissing, setIsCoverFileMissing] = useState(false);
  const descriptionRef = useRef(null);
  const [isScrolledToBottom, setIsScrolledToBottom] = useState(true); // Assume true initially if no scroll needed

  const handleScroll = () => {
    if (descriptionRef.current) {
      const { scrollTop, scrollHeight, clientHeight } = descriptionRef.current;
      // Check if scrolled to bottom (with a small tolerance for floating point inaccuracies)
      const atBottom = scrollHeight - scrollTop <= clientHeight + 1;
      setIsScrolledToBottom(atBottom);
    }
  };

  const fetchBook = async (isInitialLoad = false) => {
    try {
      const response = await fetch(`/api/books/${id}`);
      if (!response.ok) {
        throw new Error('Failed to fetch book details');
      }
      const data = await response.json();
      setBook(data);
    } catch (err) {
      setError(err.message);
    } finally {
      if (isInitialLoad) {
        setLoading(false);
      }
    }
  };

  const handleCoverUpload = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setSelectedFile(file); // Store the file in state

    if (hasCover) {
      setShowUploadConfirmDialog(true);
    } else {
      executeCoverUpload(file);
    }
    event.target.value = null; // Clear the file input
  };

  const executeCoverUpload = async (file) => {
    setShowUploadConfirmDialog(false); // Close dialog if open

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`/api/books/${id}/cover`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        let errorMessage = 'Failed to upload book cover';
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // ignore if response is not json
        }
        throw new Error(errorMessage);
      }

      setBookCoverUrl(`/api/books/${id}/cover?timestamp=${new Date().getTime()}`); // Force refresh
      setHasCover(true);
      setNotification({ type: 'success', message: 'Book cover uploaded successfully!' });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
    } finally {
      setSelectedFile(null); // Clear the selected file
    }
  };

  const handleCoverDelete = () => {
    setShowDeleteConfirmDialog(true);
  };

  const executeCoverDelete = async () => {
    setShowDeleteConfirmDialog(false); // Close dialog

    try {
      const response = await fetch(`/api/books/${id}/cover`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        let errorMessage = 'Failed to delete book cover';
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // ignore if response is not json
        }
        throw new Error(errorMessage);
      }

      setBookCoverUrl(null);
      setHasCover(false);
      setNotification({ type: 'success', message: 'Book cover deleted successfully!' });
    } catch (err) {
      setNotification({ type: 'error', message: err.message });
    }
  };

  useEffect(() => {
    fetchBook(true);
  }, [id]);


  useEffect(() => {
    // Initial check when component mounts or book data changes
    if (descriptionRef.current) {
      const { scrollHeight, clientHeight } = descriptionRef.current;
      // If content is not scrollable, it's effectively "scrolled to bottom"
      setIsScrolledToBottom(scrollHeight <= clientHeight);
    }
  }, [book]);

  useEffect(() => {
    const checkCoverExistence = async () => {
      if (!id) return;
      try {
        const response = await fetch(`/api/books/${id}/cover/exists`);
        if (!response.ok) {
          throw new Error('Failed to check cover existence');
        }
        const data = await response.json();
        setHasCover(data.exists);
      } catch (err) {
        console.error('Error checking cover existence:', err);
        setHasCover(false);
      }
    };
    checkCoverExistence();
  }, [id]);

  useEffect(() => {
    const fetchBookCover = async () => {
      if (hasCover && id) {
        try {
          const response = await fetch(`/api/books/${id}/cover`);
          if (response.status === 404) {
            setIsCoverFileMissing(true);
            setBookCoverUrl(null);
          } else if (!response.ok) {
            throw new Error('Failed to fetch book cover');
          } else {
            setBookCoverUrl(`/api/books/${id}/cover?timestamp=${new Date().getTime()}`);
            setIsCoverFileMissing(false);
          }
        } catch (err) {
          console.error('Error fetching book cover:', err);
          setBookCoverUrl(null);
          setIsCoverFileMissing(false); // Assume not missing if error is not 404
        }
      } else {
        setBookCoverUrl(null);
        setIsCoverFileMissing(false);
      }
    };
    fetchBookCover();
  }, [hasCover, id]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('bookDetails.title')}</h1>
        <p className="text-center text-gray-500">{t('bookDetails.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">{t('bookDetails.title')}</h1>
        <p className="text-center text-red-500">{t('common.error')}: {error}</p>
        <Link to="/" className="back-link">
          {t('common.backToList')}
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">{t('bookDetails.title')}</h1>
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
        <div className="flex flex-col md:flex-row gap-4">
          <div className="md:w-2/3 bg-white border border-gray-300 rounded p-6 shadow flex flex-col">
            <div className="flex-grow">
              <div className="mb-4">
                <strong>{t('bookDetails.display.title')}:</strong> {book.title}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.authors')}:</strong> {book.authors.map((author, index) => (
                  <span key={author.id}>
                      {author.firstName} {author.lastName}
                    {index < book.authors.length - 1 ? ', ' : ''}
                  </span>
                ))}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.series')}:</strong> {book.series ? <Link to={`/series/${book.series.id}`} className="series-link">{book.series.title}</Link> : t('common.na')}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.volume')}:</strong> {book.volume || t('common.na')}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.creationDate')}:</strong> {book.creationDate || t('common.na')}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.publicationDate')}:</strong> {book.publicationDate || t('common.na')}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.publisher')}:</strong> {book.publisher || t('common.na')}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.labels')}:</strong> {book.labels && book.labels.length > 0 ? book.labels.join(', ') : t('common.na')}
              </div>
              <div className="mb-4">
                <strong>{t('bookDetails.display.description')}:</strong>
                <div
                  ref={descriptionRef}
                  onScroll={handleScroll}
                  className={`mt-1 py-2 px-3 h-auto max-h-[30rem] overflow-y-auto whitespace-pre-wrap ${!isScrolledToBottom ? 'fade-bottom-scroll' : ''}`}
                >
                  {book.description || t('common.na')}
                </div>
              </div>
            </div>
            <div className="flex justify-end mt-4">
              <Link to={`/books/${book.id}/edit`} className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
                {t('common.edit')}
              </Link>
            </div>
          </div>
          <div className="md:w-1/3 flex flex-col gap-4">
            <div className="bg-white border border-gray-300 rounded p-6 shadow flex flex-col">
              <div className="flex-grow flex items-center justify-center">
                {bookCoverUrl ? (
                  <img src={bookCoverUrl} alt={t('bookDetails.coverImageAlt', { title: book.title })} className="max-w-full h-auto" />
                ) : isCoverFileMissing ? (
                  <div className="text-center text-red-500">
                    <p className="mb-2">{t('bookDetails.coverFileMissing')}</p>
                    <button
                      onClick={handleCoverDelete}
                      className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
                    >
                      {t('bookDetails.deleteCoverMetadata')}
                    </button>
                  </div>
                ) : (
                  <div className="text-gray-500 text-center">
                    <svg xmlns="http://www.w3.org/2000/svg" className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L20 16m-2-6a2 2 0 100-4 2 2 0 000 4z" />
                    </svg>
                    <p className="mt-2">{t('bookDetails.noCoverAvailable')}</p>
                  </div>
                )}
              </div>
              <div className="flex justify-end gap-2 mt-4">
                <input
                  type="file"
                  id="coverUpload"
                  accept="image/*"
                  className="hidden"
                  onChange={handleCoverUpload}
                />
                <button
                  onClick={() => document.getElementById('coverUpload').click()}
                  className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
                >
                  {hasCover ? t('bookDetails.changeCover') : t('bookDetails.uploadCover')}
                </button>
                {hasCover && !isCoverFileMissing && (
                  <button
                    onClick={handleCoverDelete}
                    className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
                  >
                    {t('bookDetails.deleteCover')}
                  </button>
                )}
              </div>
            </div>
            <BookFormats bookId={id} showNotification={setNotification} />
          </div>
        </div>
      <Link to="/" className="back-link">
        {t('common.backToList')}
      </Link>

      {showUploadConfirmDialog && (
        <ConfirmationDialog
          message={t('bookDetails.confirmCoverReplace')}
          onConfirm={() => executeCoverUpload(selectedFile)}
          onCancel={() => {
            setShowUploadConfirmDialog(false);
            setSelectedFile(null);
          }}
        />
      )}

      {showDeleteConfirmDialog && (
        <ConfirmationDialog
          message={t('bookDetails.confirmCoverDelete')}
          onConfirm={executeCoverDelete}
          onCancel={() => setShowDeleteConfirmDialog(false)}
        />
      )}
    </div>
  );
};

export default BookDetails;
