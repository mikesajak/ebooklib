import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';

const BookTable = () => {
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchBooks = async () => {
      try {
        const response = await fetch('/api/books?page=0&size=100'); // Adjust size as needed
        if (!response.ok) {
          throw new Error('Failed to fetch books');
        }
        const data = await response.json();
        setBooks(data.content || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchBooks();
  }, []);

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

  return (
    <div style={{maxWidth: '1280px', margin: '0 auto', padding: '1rem'}}>
      <h1 style={{fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem'}}>Book List</h1>
      <table style={{width: '100%', backgroundColor: 'white', border: '1px solid #d1d5db', borderCollapse: 'collapse'}}>
        <thead>
          <tr style={{backgroundColor: '#3b82f6', color: '#1e40af'}}>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Title</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Authors</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db', width: '60px'}}>Series</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Volume</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Publication Date</th>
            <th style={{padding: '0.5rem 1rem', border: '1px solid #d1d5db'}}>Publisher</th>
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
    </div>
  );
};

export default BookTable;
