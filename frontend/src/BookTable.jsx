import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import Notification from './Notification';
import { useTranslation } from 'react-i18next';

const BookTable = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title'); // Default sort field
  const [sortDirection, setSortDirection] = useState('asc'); // Default sort direction
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    if (location.state && location.state.notification) {
      setNotification(location.state.notification);
      // Clear location state to prevent notification from re-appearing on refresh
      window.history.replaceState({}, document.title)
    }
  }, [location.state]);

  useEffect(() => {
    const fetchBooks = async () => {
      try {
        const response = await fetch(`/api/books?page=${page}&size=${size}&sort=${sortField},${sortDirection}`);
        if (!response.ok) {
          throw new Error('Failed to fetch books');
        }
        const data = await response.json();
        setBooks(data.content || []);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchBooks();
  }, [page, size, sortField, sortDirection]);

  if (loading) {
    return (
      <div style={{padding: '1rem 2rem'}}>
        <h1 style={{fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem'}}>{t('bookTable.title')}</h1>
        <p style={{textAlign: 'center', color: '#6b7280'}}>{t('bookTable.loadingBooks')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{padding: '1rem 2rem'}}>
        <h1 style={{fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem'}}>{t('bookTable.title')}</h1>
        <p style={{textAlign: 'center', color: '#dc2626'}}>{t('bookTable.error')}: {error}</p>
      </div>
    );
  }

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc'); // Default to ascending when changing sort field
    }
  };

  const getSortIndicator = (field) => {
    if (sortField === field) {
      return sortDirection === 'asc' ? ' ▲' : ' ▼';
    }
    return '';
  };

  return (
    <div style={{padding: '1rem 2rem'}}>
      {notification && (
        <Notification
          message={notification.message}
          type={notification.type}
          onClose={() => setNotification(null)}
        />
      )}
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem'}}>
        <h1 style={{fontSize: '1.5rem', fontWeight: 'bold'}}>{t('bookTable.title')}</h1>
        <Link to="/add-book">
          <button style={{backgroundColor: '#22c55e', color: 'white', fontWeight: 'bold', padding: '0.5rem 1rem', borderRadius: '0.25rem'}}>
            {t('bookTable.addBookButton')}
          </button>
        </Link>
      </div>
      <table style={{width: '100%', backgroundColor: 'white', border: '1px solid #d1d5db', borderCollapse: 'collapse'}}>
        <thead>
          <tr style={{backgroundColor: '#3b82f6', color: '#1e40af'}}>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('title')}>{t('bookTable.header.title')}{getSortIndicator('title')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{t('bookTable.header.authors')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', width: '60px', cursor: 'pointer'}} onClick={() => handleSort('series.title')}>{t('bookTable.header.series')}{getSortIndicator('series.title')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('volume')}>{t('bookTable.header.volume')}{getSortIndicator('volume')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('publicationDate')}>Publication Date{getSortIndicator('publicationDate')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('publisher')}>Publisher{getSortIndicator('publisher')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{t('bookTable.header.description')}</th>
          </tr>
        </thead>
        <tbody>
          {books.length === 0 ? (
            <tr>
              <td colSpan="7" style={{padding: '1rem', textAlign: 'center', color: '#6b7280'}}>
                {t('bookTable.noBooksFound')}
              </td>
            </tr>
          ) : (
            books.map((book, index) => (
              <tr
                key={book.id}
                onClick={() => navigate(`/book/${book.id}`)}
                style={{backgroundColor: index % 2 === 0 ? '#f9fafb' : 'white', cursor: 'pointer'}}
              >
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>
                  <Link
                    to={`/book/${book.id}`}
                    className="book-link"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {book.title}
                  </Link>
                </td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>
                  {book.authors.map((author, idx) => (
                    <span key={author.id}>
                      <Link
                        to={`/author/${author.id}`}
                        className="author-link"
                        onClick={(e) => e.stopPropagation()}
                      >
                        {author.firstName} {author.lastName}
                      </Link>
                      {idx < book.authors.length - 1 ? ' ' : ''}
                    </span>
                  ))}
                </td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', width: '60px'}}>
                  {book.series ? (
                    <Link
                      to={`/series/${book.series.id}`}
                      className="series-link"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {book.series.title}
                    </Link>
                  ) : t('common.na')}
                </td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.volume || t('common.na')}</td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.publicationDate || t('common.na')}</td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.publisher || t('common.na')}</td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.description || t('common.na')}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem'}}>
        <div>
          {t('bookTable.itemsPerPage')}
          <select
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0); // Reset to first page when size changes
            }}
            style={{marginLeft: '0.5rem', padding: '0.25rem', borderRadius: '0.25rem', border: '1px solid #d1d5db'}}
          >
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
        </div>

        <div>
          <button
            onClick={() => setPage(0)}
            disabled={page === 0}
            style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === 0 ? 'not-allowed' : 'pointer'}}
          >
            {t('bookTable.pagination.first')}
          </button>
          <button
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
            style={{marginLeft: '0.5rem', padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === 0 ? 'not-allowed' : 'pointer'}}
          >
            {t('bookTable.pagination.previous')}
          </button>
          <span style={{margin: '0 1rem'}}>
            {t('bookTable.pagination.pageOf', { page: page + 1, totalPages: totalPages })}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page === totalPages - 1}
            style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === totalPages - 1 ? 'not-allowed' : 'pointer'}}
          >
            {t('bookTable.pagination.next')}
          </button>
          <button
            onClick={() => setPage(totalPages - 1)}
            disabled={page === totalPages - 1}
            style={{marginLeft: '0.5rem', padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === totalPages - 1 ? 'not-allowed' : 'pointer'}}
          >
            {t('bookTable.pagination.last')}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BookTable;
