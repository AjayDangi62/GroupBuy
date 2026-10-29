import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { Order } from '../types/order';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import {
  ArrowLeft,
  MapPin,
  CheckCircle2,
  Package,
  XCircle,
} from 'lucide-react';
import { formatCurrency } from '../utils/currency';

export const OrderDetailPage: React.FC = () => {
  const { orderNumber } = useParams<{ orderNumber: string }>();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [cancelling, setCancelling] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchOrder = async () => {
    if (!orderNumber) return;
    setLoading(true);
    setError(null);
    try {
      const data = await orderApi.getOrderByNumber(orderNumber);
      setOrder(data);
    } catch {
      setError('Order not found or permission denied.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrder();
  }, [orderNumber]);

  const handleCancelOrder = async () => {
    if (!order) return;
    if (!window.confirm('Are you sure you want to cancel this order? Reserved stock will be restored immediately.')) {
      return;
    }

    setCancelling(true);
    try {
      const updated = await orderApi.cancelOrder(order.orderNumber);
      setOrder(updated);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || 'Failed to cancel order.');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullScreen message="Loading order details..." />;
  }

  if (error || !order) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center space-y-4 bg-white">
        <ErrorAlert message={error || 'Order not found'} />
        <Link to="/orders" className="text-sm font-semibold text-emerald-700 hover:underline">
          Return to My Orders
        </Link>
      </div>
    );
  }

  const canCancel = order.status === 'CREATED' || order.status === 'PAID';
  const steps = ['CREATED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
  const currentStepIdx = steps.indexOf(order.status);

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      {/* Top navigation */}
      <div>
        <Link
          to="/orders"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-emerald-700 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Order History</span>
        </Link>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight font-mono">
                {order.orderNumber}
              </h1>
              <Badge status={order.status} />
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Placed on {new Date(order.createdAt).toLocaleString()}
            </p>
          </div>

          {canCancel && (
            <button
              onClick={handleCancelOrder}
              disabled={cancelling}
              className="bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 font-semibold text-xs px-4 py-2 rounded-xl transition-colors cursor-pointer flex items-center gap-1.5 self-start sm:self-auto"
            >
              <XCircle className="w-4 h-4" />
              <span>{cancelling ? 'Cancelling...' : 'Cancel Order'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Progress status pipeline */}
      {order.status !== 'CANCELLED' ? (
        <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-xs">
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-6">
            Fulfillment Progress
          </h3>
          <div className="grid grid-cols-5 gap-2 relative">
            {steps.map((step, idx) => {
              const isDone = idx <= currentStepIdx;
              const isCurrent = idx === currentStepIdx;

              return (
                <div key={step} className="flex flex-col items-center text-center space-y-2">
                  <div
                    className={`w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                      isDone
                        ? 'bg-emerald-700 text-white shadow-xs'
                        : 'bg-slate-100 text-slate-400'
                    } ${isCurrent ? 'ring-4 ring-emerald-100' : ''}`}
                  >
                    {isDone ? <CheckCircle2 className="w-5 h-5" /> : idx + 1}
                  </div>
                  <span
                    className={`text-[11px] font-bold ${
                      isCurrent ? 'text-emerald-800' : isDone ? 'text-slate-800' : 'text-slate-400'
                    }`}
                  >
                    {step}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="bg-rose-50 border border-rose-200 rounded-3xl p-6 text-rose-900 flex items-center gap-3">
          <XCircle className="w-6 h-6 text-rose-600 shrink-0" />
          <div>
            <h3 className="font-bold text-sm">Order Cancelled</h3>
            <p className="text-xs text-rose-700 mt-0.5">
              This order was cancelled and inventory was returned to the platform.
            </p>
          </div>
        </div>
      )}

      {/* Order Item Details Table */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex items-center gap-2 pb-4 border-b border-slate-100">
          <Package className="w-5 h-5 text-emerald-700" />
          <h2 className="font-bold text-slate-900 text-base">Ordered Products</h2>
        </div>

        <div className="divide-y divide-slate-100">
          {order.items.map((item) => (
            <div key={item.id} className="py-4 flex items-center justify-between gap-4">
              <div className="space-y-1">
                <Link
                  to={`/products/${item.productId}`}
                  className="font-bold text-slate-900 text-sm hover:text-emerald-700 transition-colors"
                >
                  {item.productName}
                </Link>
                <div className="text-xs text-slate-400 font-mono">
                  SKU: {item.productSku} • Qty: {item.quantity} × {formatCurrency(item.unitPrice, order.currency)}
                </div>
              </div>
              <span className="font-black text-slate-900 text-sm">
                {formatCurrency(item.lineTotal, order.currency)}
              </span>
            </div>
          ))}
        </div>

        <div className="pt-4 border-t border-slate-100 space-y-2 text-xs">
          <div className="flex justify-between text-slate-600">
            <span>Subtotal:</span>
            <span>{formatCurrency(order.totalPrice, order.currency)}</span>
          </div>
          <div className="flex justify-between text-slate-600">
            <span>Shipping:</span>
            <span className="text-emerald-700 font-bold">FREE</span>
          </div>
          <div className="border-t border-slate-100 pt-2 flex justify-between text-base font-extrabold text-slate-900">
            <span>Total:</span>
            <span>{formatCurrency(order.totalPrice, order.currency)}</span>
          </div>
        </div>

        {/* Shipping Destination */}
        <div className="pt-4 border-t border-slate-100 flex items-start gap-3">
          <MapPin className="w-4 h-4 text-emerald-700 mt-0.5 shrink-0" />
          <div className="text-xs">
            <span className="font-bold text-slate-900 block">Shipping Address:</span>
            <span className="text-slate-600">{order.shippingAddress}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
