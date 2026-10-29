import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import {
  ShoppingBag,
  ShoppingCart,
  User as UserIcon,
  Search,
  Menu,
  X,
  Sparkles,
  LogOut,
  Package,
  Layers,
  ChevronDown,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, isAdmin, isSeller, logout } = useAuth();
  const { itemCount } = useCart();
  const navigate = useNavigate();
  const location = useLocation();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery('');
      setMobileMenuOpen(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    setUserDropdownOpen(false);
    navigate('/');
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-100 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-3 group">
            <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-xl bg-emerald-700 flex items-center justify-center text-white shadow-sm group-hover:bg-emerald-800 transition-colors">
              <ShoppingBag className="w-5 h-5 sm:w-6 sm:h-6" />
            </div>
            <div>
              <span className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight group-hover:text-emerald-700 transition-colors">
                Group<span className="text-emerald-600">Buy</span>
              </span>
              <span className="block text-[10px] uppercase font-bold tracking-widest text-emerald-800/70 -mt-1">
                Smart Group-Buying
              </span>
            </div>
          </Link>

          {/* Desktop Nav Links */}
          <nav className="hidden md:flex items-center gap-1 lg:gap-2">
            <Link
              to="/"
              className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors ${
                isActive('/')
                  ? 'text-emerald-700 bg-emerald-50/80 font-bold'
                  : 'text-slate-600 hover:text-emerald-700 hover:bg-slate-50'
              }`}
            >
              Home
            </Link>
            <Link
              to="/products"
              className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors ${
                isActive('/products')
                  ? 'text-emerald-700 bg-emerald-50/80 font-bold'
                  : 'text-slate-600 hover:text-emerald-700 hover:bg-slate-50'
              }`}
            >
              Products
            </Link>
            <Link
              to="/campaigns"
              className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-1.5 ${
                isActive('/campaigns')
                  ? 'text-emerald-700 bg-emerald-50/80 font-bold'
                  : 'text-slate-600 hover:text-emerald-700 hover:bg-slate-50'
              }`}
            >
              <Sparkles className="w-4 h-4 text-emerald-600" />
              <span>Group Deals</span>
              <span className="bg-emerald-600 text-white text-[10px] font-extrabold px-1.5 py-0.5 rounded-full">
                HOT
              </span>
            </Link>
          </nav>

          {/* Search bar */}
          <div className="hidden lg:block flex-1 max-w-xs mx-6">
            <form onSubmit={handleSearch} className="relative">
              <input
                type="text"
                placeholder="Search products & deals..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-full pl-9 pr-4 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition-all text-slate-800 placeholder-slate-400"
              />
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-2.5" />
            </form>
          </div>

          {/* Right Action Icons: Cart & Auth */}
          <div className="flex items-center gap-3">
            {/* Cart Link */}
            <Link
              to="/cart"
              className="relative p-2.5 rounded-xl text-slate-700 hover:text-emerald-700 hover:bg-emerald-50 transition-colors"
              title="Shopping Cart"
            >
              <ShoppingCart className="w-5 h-5 sm:w-6 sm:h-6" />
              {itemCount > 0 && (
                <span className="absolute top-1 right-1 bg-emerald-600 text-white font-black text-[10px] w-5 h-5 rounded-full flex items-center justify-center shadow-xs">
                  {itemCount > 99 ? '99+' : itemCount}
                </span>
              )}
            </Link>

            {/* Auth Dropdown or Login/Register Links */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                  className="flex items-center gap-2 p-1.5 pl-3 pr-2.5 rounded-xl border border-slate-200 hover:border-emerald-400 transition-colors bg-white text-slate-800 text-xs font-semibold"
                >
                  <div className="w-6 h-6 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold text-xs">
                    {user?.fullName?.charAt(0) || user?.email?.charAt(0) || 'U'}
                  </div>
                  <span className="hidden sm:inline max-w-[100px] truncate">
                    {user?.fullName || user?.email}
                  </span>
                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                </button>

                {userDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white border border-slate-200 rounded-2xl shadow-xl py-2 z-50">
                    <div className="px-4 py-2 border-b border-slate-100">
                      <p className="text-xs font-bold text-slate-900 truncate">{user?.fullName}</p>
                      <p className="text-[11px] text-slate-500 truncate">{user?.email}</p>
                    </div>

                    <Link
                      to="/profile"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-emerald-50 hover:text-emerald-800 transition"
                    >
                      <UserIcon className="w-4 h-4 text-emerald-600" />
                      <span>My Profile</span>
                    </Link>

                    <Link
                      to="/orders"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-emerald-50 hover:text-emerald-800 transition"
                    >
                      <Package className="w-4 h-4 text-emerald-600" />
                      <span>My Orders</span>
                    </Link>

                    <Link
                      to="/participations"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-emerald-50 hover:text-emerald-800 transition"
                    >
                      <Sparkles className="w-4 h-4 text-emerald-600" />
                      <span>Joined Group Deals</span>
                    </Link>

                    {(isAdmin || isSeller) && (
                      <Link
                        to="/admin"
                        onClick={() => setUserDropdownOpen(false)}
                        className="flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-purple-700 bg-purple-50/50 hover:bg-purple-100 transition"
                      >
                        <Layers className="w-4 h-4 text-purple-600" />
                        <span>Admin Console</span>
                      </Link>
                    )}

                    <div className="border-t border-slate-100 my-1"></div>

                    <button
                      onClick={handleLogout}
                      className="w-full text-left flex items-center gap-2.5 px-4 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50 transition"
                    >
                      <LogOut className="w-4 h-4 text-rose-600" />
                      <span>Sign Out</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="px-3 py-2 text-xs sm:text-sm font-semibold text-slate-700 hover:text-emerald-700 transition"
                >
                  Log In
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 text-xs sm:text-sm font-bold bg-emerald-700 hover:bg-emerald-800 text-white rounded-xl shadow-xs transition"
                >
                  Register
                </Link>
              </div>
            )}

            {/* Mobile menu toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 rounded-lg text-slate-600 hover:text-emerald-700 hover:bg-slate-100"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-white border-b border-slate-200 px-4 pt-3 pb-6 space-y-3">
          <form onSubmit={handleSearch} className="relative mb-3">
            <input
              type="text"
              placeholder="Search products & deals..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-50 border border-slate-200 rounded-lg pl-9 pr-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          </form>

          <Link
            to="/"
            onClick={() => setMobileMenuOpen(false)}
            className={`block px-3 py-2 rounded-lg text-sm font-semibold ${
              isActive('/') ? 'bg-emerald-50 text-emerald-800' : 'text-slate-700'
            }`}
          >
            Home
          </Link>
          <Link
            to="/products"
            onClick={() => setMobileMenuOpen(false)}
            className={`block px-3 py-2 rounded-lg text-sm font-semibold ${
              isActive('/products') ? 'bg-emerald-50 text-emerald-800' : 'text-slate-700'
            }`}
          >
            Products
          </Link>
          <Link
            to="/campaigns"
            onClick={() => setMobileMenuOpen(false)}
            className={`block px-3 py-2 rounded-lg text-sm font-semibold ${
              isActive('/campaigns') ? 'bg-emerald-50 text-emerald-800' : 'text-slate-700'
            }`}
          >
            Group Deals
          </Link>
          <Link
            to="/cart"
            onClick={() => setMobileMenuOpen(false)}
            className="flex items-center justify-between px-3 py-2 rounded-lg text-sm font-semibold text-slate-700"
          >
            <span>Cart</span>
            <span className="bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full text-xs font-bold">
              {itemCount}
            </span>
          </Link>

          {!isAuthenticated && (
            <div className="pt-3 border-t border-slate-100 flex gap-2">
              <Link
                to="/login"
                onClick={() => setMobileMenuOpen(false)}
                className="flex-1 py-2 text-center text-sm font-semibold border border-slate-300 rounded-lg text-slate-700"
              >
                Log In
              </Link>
              <Link
                to="/register"
                onClick={() => setMobileMenuOpen(false)}
                className="flex-1 py-2 text-center text-sm font-semibold bg-emerald-700 text-white rounded-lg"
              >
                Register
              </Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
};
