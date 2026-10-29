import React from 'react';
import { Link } from 'react-router-dom';
import { Package, Tag, Users, ShoppingBag, ShieldAlert, Sparkles, Layers } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const AdminDashboardPage: React.FC = () => {
  const { user } = useAuth();

  const adminModules = [
    {
      title: 'Products Management',
      description: 'Manage catalog products, stock levels, pricing, descriptions, and SKU details.',
      icon: Package,
      link: '/admin/products',
      color: 'bg-blue-500 text-white',
      badge: 'Active',
    },
    {
      title: 'Categories Management',
      description: 'Create and configure product categories, slugs, descriptions, and active status.',
      icon: Tag,
      link: '/admin/categories',
      color: 'bg-emerald-500 text-white',
      badge: 'Active',
    },
    {
      title: 'Campaigns Management',
      description: 'Oversee group-buy campaigns, monitor lifecycle transitions, finalize successful campaigns, or cancel failed deals.',
      icon: Sparkles,
      link: '/admin/campaigns',
      color: 'bg-purple-500 text-white',
      badge: 'Active',
    },
    {
      title: 'Orders Management',
      description: 'Track customer orders, inspect order items and shipping details, and update fulfillment states.',
      icon: ShoppingBag,
      link: '/admin/orders',
      color: 'bg-amber-500 text-white',
      badge: 'Active',
    },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="bg-gradient-to-r from-slate-900 to-slate-800 text-white rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="flex items-center gap-3 mb-2">
          <div className="p-2 rounded-lg bg-emerald-500/20 text-emerald-400">
            <Layers className="w-6 h-6" />
          </div>
          <span className="text-xs font-semibold uppercase tracking-wider text-emerald-400">
            Operations & Control
          </span>
        </div>
        <h1 className="text-3xl font-extrabold tracking-tight">Admin Console</h1>
        <p className="mt-2 text-slate-300 max-w-2xl text-sm sm:text-base">
          Welcome back, {user?.fullName || 'Administrator'}. Manage platform inventory, group-buy campaigns, orders, and product taxonomy.
        </p>
      </div>

      {/* Grid of Admin Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {adminModules.map((module) => {
          const Icon = module.icon;
          return (
            <Link
              key={module.title}
              to={module.link}
              className="group block p-6 bg-white border border-slate-200 rounded-xl hover:border-emerald-500 hover:shadow-md transition-all"
            >
              <div className="flex items-start justify-between">
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${module.color} shadow-sm group-hover:scale-105 transition-transform`}>
                  <Icon className="w-6 h-6" />
                </div>
                <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                  {module.badge}
                </span>
              </div>
              <h2 className="mt-4 text-lg font-bold text-slate-900 group-hover:text-emerald-600 transition-colors">
                {module.title}
              </h2>
              <p className="mt-1 text-sm text-slate-600 leading-relaxed">
                {module.description}
              </p>
            </Link>
          );
        })}
      </div>

      {/* Unavailable Backend Feature Notice */}
      <div className="p-5 bg-slate-50 border border-slate-200 rounded-xl flex items-start gap-4">
        <div className="p-2 bg-amber-100 text-amber-700 rounded-lg shrink-0 mt-0.5">
          <ShieldAlert className="w-5 h-5" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-semibold text-slate-900">User Management Note</h3>
            <span className="text-xs px-2 py-0.5 rounded bg-slate-200 text-slate-700 font-medium">Backend Not Available</span>
          </div>
          <p className="mt-1 text-xs sm:text-sm text-slate-600">
            The backend does not currently implement a global user listing endpoint (<code className="px-1.5 py-0.5 bg-slate-100 rounded text-slate-800">GET /api/v1/admin/users</code>). Individual user registration and profile APIs are available to authenticated users via <code className="px-1.5 py-0.5 bg-slate-100 rounded text-slate-800">/api/v1/users/me</code>.
          </p>
        </div>
      </div>
    </div>
  );
};

