import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const AdminRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isAdmin, isSeller, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingSpinner fullScreen message="Verifying permissions..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin && !isSeller) {
    return (
      <div className="max-w-md mx-auto my-16 p-6 bg-white border border-rose-200 rounded-2xl text-center shadow-xs">
        <h3 className="text-lg font-bold text-rose-900">Access Restricted</h3>
        <p className="text-sm text-rose-700 mt-2">
          You do not have administrative or seller privileges to view this section.
        </p>
      </div>
    );
  }

  return <>{children}</>;
};

