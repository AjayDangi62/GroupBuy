import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { cartApi } from '../api/cartApi';
import { ErrorAlert } from '../components/common/ErrorAlert';
import {
  ShieldCheck,
  Lock,
  MapPin,
  ArrowRight,
  Loader2,
  CheckCircle2,
  ShoppingBag,
} from 'lucide-react';
import { formatINR } from '../utils/currency';

export const CheckoutPage: React.FC = () => {
  const { cart, refreshCart } = useCart();
  const navigate = useNavigate();

  const [shippingAddress, setShippingAddress] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [phone, setPhone] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!cart || cart.items.length === 0) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center bg-white min-h-[60vh]">
        <h2 className="text-xl font-bold text-slate-900">Your cart is empty</h2>
        <p className="text-sm text-slate-500 mt-2 mb-6">Add items to cart before proceeding to checkout.</p>
        <Link
          to="/products"
          className="inline-block bg-emerald-700 hover:bg-emerald-800 text-white font-semibold text-sm px-5 py-2.5 rounded-xl transition"
        >
          Browse Products
        </Link>
      </div>
    );
  }

  const handleCheckout = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shippingAddress.trim() || !city.trim() || !state.trim() || !postalCode.trim()) {
      setError('Please provide a complete shipping address.');
      return;
    }

    const fullAddress = `${shippingAddress.trim()}, ${city.trim()}, ${state.trim()} - ${postalCode.trim()}${
      phone.trim() ? ` (Phone: ${phone.trim()})` : ''
    }`;

    setIsSubmitting(true);
    setError(null);

    // Generate unique idempotency key to prevent double checkout
    const idempotencyKey = `chk_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;

    try {
      const order = await cartApi.checkout({
        shippingAddress: fullAddress,
        idempotencyKey,
      });

      await refreshCart();
      navigate(`/orders/confirmed/${order.orderNumber}`);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      const msg =
        e.response?.data?.message || 'Checkout failed. An item may have gone out of stock. Please review your cart.';
      setError(msg);
      await refreshCart();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      <div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Checkout</h1>
        <p className="text-sm text-slate-500 mt-1">
          Complete your delivery address. Inventory is locked and prices are verified on the server.
        </p>
      </div>

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      <form onSubmit={handleCheckout} className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Left 2 Cols: Shipping Details */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-xs space-y-5">
            <div className="flex items-center gap-2.5 pb-4 border-b border-slate-100">
              <MapPin className="w-5 h-5 text-emerald-700" />
              <h2 className="text-lg font-bold text-slate-900">Delivery Address</h2>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  Street Address *
                </label>
                <input
                  type="text"
                  required
                  value={shippingAddress}
                  onChange={(e) => setShippingAddress(e.target.value)}
                  placeholder="Flat 402, Shanti Niketan, 12th Cross"
                  className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-sm text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    City *
                  </label>
                  <input
                    type="text"
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Bengaluru"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-sm text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    State *
                  </label>
                  <input
                    type="text"
                    required
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    placeholder="Karnataka"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-sm text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    PIN Code *
                  </label>
                  <input
                    type="text"
                    required
                    value={postalCode}
                    onChange={(e) => setPostalCode(e.target.value)}
                    placeholder="560103"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-sm text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Mobile Number (Optional)
                  </label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+91 98765 43210"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-sm text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="bg-emerald-50/70 border border-emerald-200 rounded-2xl p-4 flex items-start gap-3 text-xs text-emerald-900">
            <Lock className="w-5 h-5 text-emerald-700 shrink-0 mt-0.5" />
            <div>
              <span className="font-bold block">Transactional Concurrency Safe</span>
              <span>Our backend uses database-level row locking to ensure stock is never oversold during checkout.</span>
            </div>
          </div>
        </div>

        {/* Right 1 Col: Summary & Place Order */}
        <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-xs space-y-6 sticky top-24">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="font-bold text-slate-900 text-lg">Order Review</h3>
            <Link to="/cart" className="text-xs text-emerald-700 font-bold hover:underline">
              Edit Cart
            </Link>
          </div>

          <div className="divide-y divide-slate-100 max-h-60 overflow-y-auto pr-1">
            {cart.items.map((item) => (
              <div key={item.id} className="py-2.5 flex items-center justify-between text-xs">
                <div className="pr-2 truncate">
                  <span className="font-bold text-slate-800 block truncate">{item.productName}</span>
                  <span className="text-slate-400">Qty: {item.quantity} × {formatINR(item.unitPrice)}</span>
                </div>
                <span className="font-bold text-slate-900 shrink-0">
                  {formatINR(item.lineTotal)}
                </span>
              </div>
            ))}
          </div>

          <div className="pt-3 border-t border-slate-200 space-y-2 text-sm">
            <div className="flex justify-between text-slate-600">
              <span>Items Subtotal:</span>
              <span>{formatINR(cart.subtotal)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>Standard Shipping:</span>
              <span className="text-emerald-700 font-bold">FREE</span>
            </div>
            <div className="border-t border-slate-200 pt-3 flex justify-between items-baseline font-bold">
              <span className="text-slate-900">Grand Total:</span>
              <span className="text-2xl font-black text-slate-900">
                {formatINR(cart.subtotal)}
              </span>
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-base py-3.5 px-4 rounded-2xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                <span>Confirming Order...</span>
              </>
            ) : (
              <>
                <span>Place Order</span>
                <ArrowRight className="w-5 h-5" />
              </>
            )}
          </button>

          <div className="flex items-center justify-center gap-1.5 text-xs text-slate-400 text-center">
            <ShieldCheck className="w-4 h-4 text-emerald-700" />
            <span>Guaranteed Price Ceiling Protection</span>
          </div>
        </div>
      </form>
    </div>
  );
};
