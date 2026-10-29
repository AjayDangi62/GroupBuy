import React from 'react';

export interface BadgeProps {
  status?: string;
  variant?: string;
  children?: React.ReactNode;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({ status, variant, children, className = '' }) => {
  const content = children || status || '';
  const statusStr = typeof content === 'string' ? content : status || '';

  const getBadgeStyle = () => {
    if (variant) {
      switch (variant) {
        case 'success':
          return 'bg-emerald-50 text-emerald-800 border-emerald-200';
        case 'warning':
          return 'bg-amber-50 text-amber-800 border-amber-200';
        case 'info':
          return 'bg-sky-50 text-sky-800 border-sky-200';
        case 'danger':
          return 'bg-rose-50 text-rose-800 border-rose-200';
        case 'neutral':
        default:
          return 'bg-slate-100 text-slate-700 border-slate-200';
      }
    }

    switch (statusStr.toUpperCase()) {
      case 'ACTIVE':
      case 'PAID':
      case 'DELIVERED':
      case 'SUCCESSFUL':
      case 'CONFIRMED':
        return 'bg-emerald-50 text-emerald-800 border-emerald-200';
      case 'SCHEDULED':
      case 'PROCESSING':
      case 'PENDING':
        return 'bg-sky-50 text-sky-800 border-sky-200';
      case 'SHIPPED':
      case 'SETTLED':
        return 'bg-emerald-100 text-emerald-900 border-emerald-300';
      case 'DRAFT':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'FAILED':
      case 'EXPIRED':
      case 'CANCELLED':
      case 'OUT_OF_STOCK':
        return 'bg-rose-50 text-rose-800 border-rose-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getBadgeStyle()} ${className}`}
    >
      {content}
    </span>
  );
};
