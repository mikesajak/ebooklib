import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const SeriesDetails = () => {
  const { id } = useParams();
  const [series, setSeries] = useState(null);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchSeriesAndBooks = async () => {
      try {
        const seriesResponse = await fetch(`/api/series/${id}`);
        if (!seriesResponse.ok) {
          throw new Error('Failed to fetch series details');
        }
        const seriesData = await seriesResponse.json();
        setSeries(seriesData);

        const booksResponse = await fetch(`/api/series/${id}/books?page=0&size=100`);
        if (!booksResponse.ok) {
          throw new Error('Failed to fetch series books');
        }
        const booksData = await booksResponse.json();
        const sortedBooks = (booksData.content || []).sort((a, b) => (a.volume || 0) - (b.volume || 0));
        setBooks(sortedBooks);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchSeriesAndBooks();
  }, [id]);

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Series Details</h1>
        <p className="text-center text-gray-500">Loading series details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Series Details</h1>
        <p className="text-center text-red-500">Error: {error}</p>
        <Link to="/" style={{marginTop: '1rem', display: 'inline-block', color: '#2563eb', textDecoration: 'none', fontWeight: '600'}}>
          Back to List
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Series Details</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow mb-6">
        <div className="mb-4">
          <strong>Title:</strong> {series.title}
        </div>
        <div className="mb-4">
          <strong>Description:</strong> {series.description || 'N/A'}
        </div>
      </div>

      <h2 className="text-xl font-semibold mb-4">Books in this Series</h2>
      {books.length === 0 ? (
        <p className="text-gray-500">No books found.</p>
      ) : (
        <table className="table-fixed w-full bg-white border border-gray-300">
          <thead>
            <tr className="bg-gray-100">
              <th className="py-2 px-4 border-b w-1/6">Volume</th>
              <th className="py-2 px-4 border-b">Title</th>
            </tr>
          </thead>
          <tbody>
            {books.map((book) => (
              <tr key={book.id} className="hover:bg-gray-50">
                <td className="py-2 px-4 border-b w-16">{book.volume || '-'}</td>
                <td className="py-2 px-4 border-b">
                  <Link to={`/book/${book.id}`} style={{color: '#2563eb', textDecoration: 'none'}}>
                    {book.title}
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Link to="/" style={{marginTop: '1rem', display: 'inline-block', color: '#2563eb', textDecoration: 'none', fontWeight: '600'}}>
        Back to List
      </Link>
    </div>
  );
};

export default SeriesDetails;
