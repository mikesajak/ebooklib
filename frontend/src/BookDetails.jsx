import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const BookDetails = () => {
  const { id } = useParams();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchBook = async () => {
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
        setLoading(false);
      }
    };

    fetchBook();
  }, [id]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Book Details</h1>
        <p className="text-center text-gray-500">Loading book details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Book Details</h1>
        <p className="text-center text-red-500">Error: {error}</p>
        <Link to="/" className="back-link">
          Back to List
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Book Details</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow">
        <div className="mb-4">
          <strong>Title:</strong> {book.title}
        </div>
        <div className="mb-4">
          <strong>Authors:</strong> {book.authors.map((author, index) => (
            <span key={author.id}>
              <Link to={`/author/${author.id}`} className="author-link">
                {author.name}
              </Link>
              {index < book.authors.length - 1 ? ', ' : ''}
            </span>
          ))}
        </div>
        <div className="mb-4">
          <strong>Series:</strong> {book.series ? <Link to={`/series/${book.series.id}`} className="series-link">{book.series.title}</Link> : 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Volume:</strong> {book.volume || 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Creation Date:</strong> {book.creationDate || 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Publication Date:</strong> {book.publicationDate || 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Publisher:</strong> {book.publisher || 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Description:</strong> {book.description || 'N/A'}
        </div>
      </div>
      <Link to="/" className="back-link">
        Back to List
      </Link>
    </div>
  );
};

export default BookDetails;
