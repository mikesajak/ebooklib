import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

const ProtectedRoute = ({ children, requiredRole }) => {
  const { user, isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-100 border-t-indigo-600"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to /login, but save the current location they were trying to go to
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRole) {
    const hasRole = user?.roles?.includes(requiredRole) || user?.roles?.includes(`ROLE_${requiredRole}`);
    if (!hasRole) {
      // User is authenticated but doesn't have the required role
      return <Navigate to="/" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;
