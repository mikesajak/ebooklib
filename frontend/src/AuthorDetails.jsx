import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';

const AuthorDetails = () => {
  const { id } = useParams();
  const [author, setAuthor] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAuthor = async () => {
      try {
        const response = await fetch(`/api/authors/${id}`);
        if (!response.ok) {
          throw new Error('Failed to fetch author details');
        }
        const data = await response.json();
        setAuthor(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchAuthor();
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
        <Link to="/" className="mt-4 inline-block bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700">
          Back to List
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Author Details</h1>
      <div className="bg-white border border-gray-300 rounded p-6 shadow">
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
      <Link to="/" className="mt-4 inline-block bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-700">
        Back to List
      </Link>
    </div>
  );
};

export default AuthorDetails;