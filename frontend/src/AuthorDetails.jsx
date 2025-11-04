import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const AuthorDetails = () => {
  const { id } = useParams();
  const [author, setAuthor] = useState(null);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAuthorAndBooks = async () => {
      try {
        const authorResponse = await fetch(`/api/authors/${id}`);
        if (!authorResponse.ok) {
          throw new Error('Failed to fetch author details');
        }
        const authorData = await authorResponse.json();
        setAuthor(authorData);

        const booksResponse = await fetch(`/api/authors/${id}/books?page=0&size=100`);
        if (!booksResponse.ok) {
          throw new Error('Failed to fetch author books');
        }
        const booksData = await booksResponse.json();
        setBooks(booksData.content || []);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuthorAndBooks();
  }, [id]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Author Details</h1>
        <p className="text-center text-gray-500">Loading author details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Author Details</h1>
        <p className="text-center text-red-500">Error: {error}</p>
        <Link to="/" style={{marginTop: '1rem', display: 'inline-block', color: '#2563eb', textDecoration: 'none', fontWeight: '600'}}>
          Back to List
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Author Details</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow mb-6">
        <div className="mb-4">
          <strong>Name:</strong> {author.name}
        </div>
        <div className="mb-4">
          <strong>Bio:</strong> {author.bio || 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Birth Date:</strong> {author.birthDate || 'N/A'}
        </div>
        <div className="mb-4">
          <strong>Death Date:</strong> {author.deathDate || 'N/A'}
        </div>
      </div>

      <h2 className="text-xl font-semibold mb-4">Books by this Author</h2>
      {books.length === 0 ? (
        <p className="text-gray-500">No books found.</p>
      ) : (
        <ul className="list-disc list-inside bg-white border border-gray-300 rounded p-4 shadow">
          {books.map((book) => (
            <li key={book.id} className="mb-2">
              <Link to={`/book/${book.id}`} style={{color: '#2563eb', textDecoration: 'none'}}>
                {book.title}
              </Link>
            </li>
          ))}
        </ul>
      )}

      <Link to="/" style={{marginTop: '1rem', display: 'inline-block', color: '#2563eb', textDecoration: 'none', fontWeight: '600'}}>
        Back to List
      </Link>
    </div>
  );
};

export default AuthorDetails;
