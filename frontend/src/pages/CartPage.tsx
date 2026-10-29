import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { EmptyState } from '../components/common/EmptyState';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { Trash2, Plus, Minus, ShoppingBag, ArrowRight, ShieldCheck } from 'lucide-react';
import { formatINR } from '../utils/currency';

export const CartPage: React.FC = () => {
  const { cart, isLoading, updateQuantity, removeFromCart, clearCart } = useCart();
  const navigate = useNavigate();

  if (isLoading && !cart) {
    return <LoadingSpinner fullScreen message="Loading shopping cart..." />;
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 bg-white min-h-[70vh]">
        <EmptyState
          icon={<ShoppingBag className="w-8 h-8 text-emerald-600" />}
          title="Your Shopping Cart is Empty"
          description="Browse our active group deals or retail products to find items you love."
          actionText="Browse Products"
          actionLink="/products"
        />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Shopping Cart</h1>
          <p className="text-sm text-slate-500 mt-1">
            {cart.totalQuantity} {cart.totalQuantity === 1 ? 'item' : 'items'} in your cart
          </p>
        </div>

        <button
          onClick={clearCart}
          className="text-xs font-semibold text-rose-600 hover:text-rose-700 hover:underline flex items-center gap-1 cursor-pointer"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Clear Cart</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Cart Item List */}
        <div className="lg:col-span-2 space-y-4">
          {cart.items.map((item) => (
            <div
              key={item.id}
              className="bg-white border border-slate-200 rounded-2xl p-5 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
            >
              <div className="flex items-center gap-4">
                <div className="w-20 h-20 bg-slate-50 border border-slate-100 rounded-xl overflow-hidden shrink-0 flex items-center justify-center p-2">
                  {item.productImageUrl ? (
                    <img
                      src={item.productImageUrl}
                      alt={item.productName}
                      className="w-full h-full object-contain"
                    />
                  ) : (
                    <ShoppingBag className="w-8 h-8 text-slate-300" />
                  )}
                </div>

                <div className="space-y-1">
                  <Link
                    to={`/products/${item.productId}`}
                    className="font-bold text-slate-900 text-sm hover:text-emerald-700 transition-colors line-clamp-1"
                  >
                    {item.productName}
                  </Link>
                  <p className="text-xs text-slate-400 font-mono">SKU: {item.productSku}</p>
                  {item.campaignTitle && (
                    <span className="inline-block bg-emerald-50 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded-md border border-emerald-200">
                      Group Buy Deal
                    </span>
                  )}
                  <div className="text-sm font-bold text-slate-900 sm:hidden">
                    {formatINR(item.unitPrice)}
                  </div>
                </div>
              </div>

              {/* Quantity Controls & Line Total */}
              <div className="flex items-center justify-between w-full sm:w-auto sm:gap-8 pt-3 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                <div className="hidden sm:block text-right">
                  <div className="text-sm font-bold text-slate-900">
                    {formatINR(item.unitPrice)}
                  </div>
                  <div className="text-[11px] text-slate-400">per unit</div>
                </div>

                {/* Counter */}
                <div className="flex items-center border border-slate-200 rounded-xl bg-slate-50">
                  <button
                    onClick={() => updateQuantity(item.id, Math.max(1, item.quantity - 1))}
                    disabled={item.quantity <= 1}
                    className="p-1.5 text-slate-500 hover:text-slate-900 disabled:opacity-30 cursor-pointer"
                    aria-label="Decrease quantity"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="w-8 text-center text-xs font-bold text-slate-900">
                    {item.quantity}
                  </span>
                  <button
                    onClick={() => updateQuantity(item.id, item.quantity + 1)}
                    disabled={item.quantity >= item.availableStock}
                    className="p-1.5 text-slate-500 hover:text-slate-900 disabled:opacity-30 cursor-pointer"
                    aria-label="Increase quantity"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Subtotal */}
                <div className="text-right min-w-20">
                  <span className="text-base font-black text-slate-900">
                    {formatINR(item.lineTotal)}
                  </span>
                </div>

                {/* Remove */}
                <button
                  onClick={() => removeFromCart(item.id)}
                  className="p-1.5 text-slate-400 hover:text-rose-600 transition-colors cursor-pointer"
                  title="Remove item"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Order Summary Sidebar */}
        <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-xs space-y-6 sticky top-24">
          <h3 className="font-bold text-slate-900 text-lg border-b border-slate-100 pb-3">
            Order Summary
          </h3>

          <div className="space-y-3 text-sm">
            <div className="flex justify-between text-slate-600">
              <span>Items Total ({cart.totalQuantity} items):</span>
              <span className="font-semibold text-slate-900">{formatINR(cart.subtotal)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Delivery Charges:</span>
              <span className="text-emerald-700 font-bold">FREE</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Taxes & Duties:</span>
              <span>Included</span>
            </div>

            <div className="border-t border-slate-200 pt-3 flex justify-between items-baseline">
              <span className="font-bold text-slate-900 text-base">Grand Total:</span>
              <span className="font-black text-2xl text-slate-900">
                {formatINR(cart.subtotal)}
              </span>
            </div>
          </div>

          <button
            onClick={() => navigate('/checkout')}
            className="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-base py-3.5 px-4 rounded-2xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer"
          >
            <span>Proceed to Checkout</span>
            <ArrowRight className="w-5 h-5" />
          </button>

          <div className="pt-2 flex items-center gap-2 text-xs text-slate-400 justify-center">
            <ShieldCheck className="w-4 h-4 text-emerald-700" />
            <span>Server-Verified Atomic Checkout</span>
          </div>
        </div>
      </div>
    </div>
  );
};
