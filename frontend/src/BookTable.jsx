import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';

const BookTable = () => {
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('title'); // Default sort field
  const [sortDirection, setSortDirection] = useState('asc'); // Default sort direction

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
      <div style={{maxWidth: '1280px', margin: '0 auto', padding: '1rem'}}>
        <h1 style={{fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem'}}>Book List</h1>
        <p style={{textAlign: 'center', color: '#6b7280'}}>Loading books...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{maxWidth: '1280px', margin: '0 auto', padding: '1rem'}}>
        <h1 style={{fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem'}}>Book List</h1>
        <p style={{textAlign: 'center', color: '#dc2626'}}>Error: {error}</p>
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
    <div style={{maxWidth: '1280px', margin: '0 auto', padding: '1rem'}}>
      <h1 style={{fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem'}}>Book List</h1>
      <table style={{width: '100%', backgroundColor: 'white', border: '1px solid #d1d5db', borderCollapse: 'collapse'}}>
        <thead>
          <tr style={{backgroundColor: '#3b82f6', color: '#1e40af'}}>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('title')}>Title{getSortIndicator('title')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Authors</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', width: '60px', cursor: 'pointer'}} onClick={() => handleSort('series.title')}>Series{getSortIndicator('series.title')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('volume')}>Volume{getSortIndicator('volume')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('publicationDate')}>Publication Date{getSortIndicator('publicationDate')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', cursor: 'pointer'}} onClick={() => handleSort('publisher')}>Publisher{getSortIndicator('publisher')}</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Description</th>
          </tr>
        </thead>
        <tbody>
          {books.length === 0 ? (
            <tr>
              <td colSpan="7" style={{padding: '1rem', textAlign: 'center', color: '#6b7280'}}>
                No books found.
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
                        {author.name}
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
                  ) : '-'}
                </td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.volume || '-'}</td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.publicationDate || '-'}</td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.publisher || '-'}</td>
                <td style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>{book.description || '-'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>

      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem'}}>
        <div>
          Items per page:
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
            First
          </button>
          <button
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
            style={{marginLeft: '0.5rem', padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === 0 ? 'not-allowed' : 'pointer'}}
          >
            Previous
          </button>
          <span style={{margin: '0 1rem'}}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page === totalPages - 1}
            style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === totalPages - 1 ? 'not-allowed' : 'pointer'}}
          >
            Next
          </button>
          <button
            onClick={() => setPage(totalPages - 1)}
            disabled={page === totalPages - 1}
            style={{marginLeft: '0.5rem', padding: '0.5rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.25rem', backgroundColor: '#f9fafb', cursor: page === totalPages - 1 ? 'not-allowed' : 'pointer'}}
          >
            Last
          </button>
        </div>
      </div>
    </div>
  );
};

export default BookTable;
