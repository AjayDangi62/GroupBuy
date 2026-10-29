import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingBag, ShieldCheck, Zap, Users, RefreshCw } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-slate-900 text-slate-400 mt-auto border-t border-slate-800">
      {/* Value props banner */}
      <div className="border-b border-slate-800 py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center shrink-0">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-sm">Volume Discounts</h4>
              <p className="text-xs text-slate-400 mt-0.5">Unit prices drop as more buyers join</p>
            </div>
          </div>

          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center shrink-0">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-sm">Price Protection</h4>
              <p className="text-xs text-slate-400 mt-0.5">Guaranteed maximum authorization ceiling</p>
            </div>
          </div>

          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center shrink-0">
              <RefreshCw className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-sm">Differential Refunds</h4>
              <p className="text-xs text-slate-400 mt-0.5">Automatic refunds when tiers unlock</p>
            </div>
          </div>

          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center shrink-0">
              <Zap className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-white font-semibold text-sm">Direct Retail Cart</h4>
              <p className="text-xs text-slate-400 mt-0.5">Instant purchasing always supported</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-8">
          <div className="md:col-span-2 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-emerald-600 flex items-center justify-center text-white">
                <ShoppingBag className="w-5 h-5" />
              </div>
              <span className="text-xl font-bold text-white tracking-tight">
                Group<span className="text-emerald-400">Buy</span>
              </span>
            </div>
            <p className="text-sm text-slate-400 leading-relaxed max-w-sm">
              The smart e-commerce platform that pools individual consumer demand to unlock real wholesale pricing tiers. Better together, cheaper together.
            </p>
          </div>

          <div>
            <h5 className="text-white font-semibold text-sm mb-3">Shop & Deals</h5>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/products" className="hover:text-emerald-400 transition">
                  Product Catalog
                </Link>
              </li>
              <li>
                <Link to="/campaigns" className="hover:text-emerald-400 transition">
                  Active Group Deals
                </Link>
              </li>
              <li>
                <Link to="/cart" className="hover:text-emerald-400 transition">
                  Shopping Cart
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-white font-semibold text-sm mb-3">Customer Account</h5>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/orders" className="hover:text-emerald-400 transition">
                  My Orders
                </Link>
              </li>
              <li>
                <Link to="/participations" className="hover:text-emerald-400 transition">
                  Joined Group Deals
                </Link>
              </li>
              <li>
                <Link to="/profile" className="hover:text-emerald-400 transition">
                  Profile Settings
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h5 className="text-white font-semibold text-sm mb-3">Platform</h5>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/login" className="hover:text-emerald-400 transition">
                  Log In
                </Link>
              </li>
              <li>
                <Link to="/register" className="hover:text-emerald-400 transition">
                  Create Account
                </Link>
              </li>
              <li>
                <span className="text-emerald-400 font-medium">Spring Boot 3 + PostgreSQL</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t border-slate-800 mt-10 pt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <p>© {new Date().getFullYear()} GroupBuy Platform. All rights reserved.</p>
          <div className="flex gap-4">
            <span className="hover:text-slate-400 transition cursor-pointer">Privacy Policy</span>
            <span className="hover:text-slate-400 transition cursor-pointer">Terms of Service</span>
            <span className="hover:text-slate-400 transition cursor-pointer">Tier Pricing Policy</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
