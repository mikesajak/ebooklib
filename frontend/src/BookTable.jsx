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
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Book List</h1>
        <p className="text-center text-gray-500">Loading books...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Book List</h1>
        <p className="text-center text-red-500">Error: {error}</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Book List</h1>
      <table className="min-w-full bg-white border border-gray-300">
        <thead>
          <tr className="bg-gray-100">
            <th className="py-2 px-4 border-b">Title</th>
            <th className="py-2 px-4 border-b">Authors</th>
            <th className="py-2 px-4 border-b">Series</th>
            <th className="py-2 px-4 border-b">Volume</th>
            <th className="py-2 px-4 border-b">Publication Date</th>
            <th className="py-2 px-4 border-b">Publisher</th>
            <th className="py-2 px-4 border-b">Description</th>
          </tr>
        </thead>
        <tbody>
          {books.length === 0 ? (
            <tr>
              <td colSpan="7" className="py-4 px-4 text-center text-gray-500">
                No books found.
              </td>
            </tr>
          ) : (
            books.map((book) => (
              <tr
                key={book.id}
                onClick={() => navigate(`/book/${book.id}`)}
                className="hover:bg-gray-50 cursor-pointer"
              >
                <td className="py-2 px-4 border-b">
                  <Link
                    to={`/book/${book.id}`}
                    className="text-blue-500 hover:underline"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {book.title}
                  </Link>
                </td>
                <td className="py-2 px-4 border-b">
                  {book.authors.map((author, index) => (
                    <span key={author.id}>
                      <Link
                        to={`/author/${author.id}`}
                        className="text-blue-500 hover:underline"
                        onClick={(e) => e.stopPropagation()} // Prevent row click
                      >
                        {author.name}
                      </Link>
                      {index < book.authors.length - 1 ? ', ' : ''}
                    </span>
                  ))}
                </td>
                <td className="py-2 px-4 border-b">
                  {book.series ? book.series.name : '-'}
                </td>
                <td className="py-2 px-4 border-b">{book.volume || '-'}</td>
                <td className="py-2 px-4 border-b">{book.publicationDate || '-'}</td>
                <td className="py-2 px-4 border-b">{book.publisher || '-'}</td>
                <td className="py-2 px-4 border-b">{book.description || '-'}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

export default BookTable;
