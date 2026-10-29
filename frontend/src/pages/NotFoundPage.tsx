import React from 'react';
import { Link } from 'react-router-dom';
import { Home, Compass, AlertCircle } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4">
      <div className="w-20 h-20 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center mb-6 shadow-inner">
        <AlertCircle className="w-10 h-10" />
      </div>
      <span className="text-sm font-bold uppercase tracking-wider text-emerald-600 mb-2">404 Error</span>
      <h1 className="text-4xl sm:text-5xl font-black text-slate-900 tracking-tight mb-4">
        Page Not Found
      </h1>
      <p className="text-slate-600 max-w-md mb-8 leading-relaxed">
        The page you are looking for doesn't exist, has been removed, or is temporarily unavailable.
      </p>

      <div className="flex flex-wrap items-center justify-center gap-3">
        <Link
          to="/"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-emerald-600 text-white font-medium hover:bg-emerald-700 shadow-sm transition-colors"
        >
          <Home className="w-4 h-4" />
          Back to Home
        </Link>
        <Link
          to="/products"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-700 font-medium hover:bg-slate-50 transition-colors"
        >
          <Compass className="w-4 h-4" />
          Browse Catalog
        </Link>
      </div>
    </div>
  );
};

