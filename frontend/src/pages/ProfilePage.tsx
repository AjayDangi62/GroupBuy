import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Badge } from '../components/common/Badge';
import { User, Mail, Shield, Calendar, Package, Sparkles } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      <div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">My Profile</h1>
        <p className="text-sm text-slate-500 mt-1">Manage your account details and view platform activity.</p>
      </div>

      <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex items-center gap-4 border-b border-slate-100 pb-6">
          <div className="w-16 h-16 rounded-2xl bg-emerald-700 text-white font-black text-2xl flex items-center justify-center shadow-md">
            {user.fullName.charAt(0) || user.username.charAt(0)}
          </div>
          <div className="space-y-1">
            <h2 className="text-xl font-bold text-slate-900">{user.fullName}</h2>
            <p className="text-sm text-slate-500 font-mono">@{user.username}</p>
            <div className="flex items-center gap-2 pt-1">
              <Badge status={user.status} />
              <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-800 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                {user.role}
              </span>
            </div>
          </div>
        </div>

        {/* Profile Info Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center gap-3">
            <Mail className="w-5 h-5 text-emerald-700 shrink-0" />
            <div>
              <span className="text-xs text-slate-400 block font-semibold">Email Address</span>
              <span className="font-semibold text-slate-800">{user.email}</span>
            </div>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center gap-3">
            <User className="w-5 h-5 text-emerald-700 shrink-0" />
            <div>
              <span className="text-xs text-slate-400 block font-semibold">User ID</span>
              <span className="font-semibold text-slate-800 font-mono">#{user.id}</span>
            </div>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center gap-3">
            <Shield className="w-5 h-5 text-emerald-700 shrink-0" />
            <div>
              <span className="text-xs text-slate-400 block font-semibold">Platform Role</span>
              <span className="font-semibold text-slate-800">{user.role}</span>
            </div>
          </div>

          <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-center gap-3">
            <Calendar className="w-5 h-5 text-emerald-700 shrink-0" />
            <div>
              <span className="text-xs text-slate-400 block font-semibold">Account Status</span>
              <span className="font-semibold text-slate-800">{user.status}</span>
            </div>
          </div>
        </div>

        {/* Quick Navigation Cards */}
        <div className="pt-4 border-t border-slate-100">
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4">
            Quick Actions
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Link
              to="/orders"
              className="p-4 bg-white border border-slate-200 hover:border-emerald-500 rounded-2xl transition-all shadow-2xs hover:shadow-xs flex items-center gap-3 group"
            >
              <Package className="w-5 h-5 text-emerald-700 group-hover:scale-110 transition-transform" />
              <div>
                <h4 className="font-bold text-slate-900 text-sm group-hover:text-emerald-700 transition-colors">
                  Order History
                </h4>
                <p className="text-xs text-slate-500">Track shipments and view past receipts</p>
              </div>
            </Link>

            <Link
              to="/participations"
              className="p-4 bg-white border border-slate-200 hover:border-emerald-500 rounded-2xl transition-all shadow-2xs hover:shadow-xs flex items-center gap-3 group"
            >
              <Sparkles className="w-5 h-5 text-emerald-700 group-hover:scale-110 transition-transform" />
              <div>
                <h4 className="font-bold text-slate-900 text-sm group-hover:text-emerald-700 transition-colors">
                  Joined Group Drops
                </h4>
                <p className="text-xs text-slate-500">View active commitments and tier savings</p>
              </div>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
