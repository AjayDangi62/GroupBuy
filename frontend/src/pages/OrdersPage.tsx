import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { Order } from '../types/order';
import { Badge } from '../components/common/Badge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { Package, ArrowRight, XCircle, ChevronLeft, ChevronRight, MapPin } from 'lucide-react';
import { formatCurrency } from '../utils/currency';

export const OrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [error, setError] = useState<string | null>(null);
  const [cancellingOrderNumber, setCancellingOrderNumber] = useState<string | null>(null);

  const fetchOrders = async (pageNumber = 0) => {
    setLoading(true);
    setError(null);
    try {
      const res = await orderApi.getMyOrders(pageNumber, 10);
      setOrders(res.content || []);
      setTotalPages(res.totalPages || 1);
      setPage(res.number || 0);
    } catch {
      setError('Failed to load order history.');
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders(page);
  }, [page]);

  const handleCancelOrder = async (orderNumber: string) => {
    if (!window.confirm(`Are you sure you want to cancel order ${orderNumber}? Inventory stock will be automatically restored to catalog.`)) {
      return;
    }

    setCancellingOrderNumber(orderNumber);
    try {
      await orderApi.cancelOrder(orderNumber);
      await fetchOrders(page);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || 'Failed to cancel order.');
    } finally {
      setCancellingOrderNumber(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      <div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">My Orders</h1>
        <p className="text-sm text-slate-500 mt-1">
          View your order history, delivery details, and self-service cancellation status.
        </p>
      </div>

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      {loading ? (
        <LoadingSpinner message="Loading orders..." />
      ) : orders.length === 0 ? (
        <EmptyState
          icon={<Package className="w-8 h-8 text-emerald-600" />}
          title="No Orders Placed Yet"
          description="When you place orders through standard checkout or settled group-buys, they will appear here."
          actionText="Browse Products"
          actionLink="/products"
        />
      ) : (
        <div className="space-y-6">
          {orders.map((order) => {
            const canCancel = order.status === 'CREATED' || order.status === 'PAID';

            return (
              <div
                key={order.id}
                className="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-xs hover:shadow-md transition-shadow"
              >
                {/* Order Top Bar */}
                <div className="bg-slate-50 border-b border-slate-100 p-5 flex flex-wrap items-center justify-between gap-4">
                  <div className="flex flex-wrap items-center gap-4 text-xs">
                    <div>
                      <span className="text-slate-400 block font-semibold uppercase tracking-wider text-[10px]">
                        Order Placed
                      </span>
                      <span className="font-bold text-slate-800">
                        {new Date(order.createdAt).toLocaleDateString()}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block font-semibold uppercase tracking-wider text-[10px]">
                        Order Number
                      </span>
                      <span className="font-mono font-bold text-slate-900">{order.orderNumber}</span>
                    </div>

                    <div>
                      <span className="text-slate-400 block font-semibold uppercase tracking-wider text-[10px]">
                        Total Amount
                      </span>
                      <span className="font-bold text-slate-900 text-sm">
                        {formatCurrency(order.totalPrice, order.currency)}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <Badge status={order.status} />

                    {canCancel && (
                      <button
                        onClick={() => handleCancelOrder(order.orderNumber)}
                        disabled={cancellingOrderNumber === order.orderNumber}
                        className="text-xs font-semibold text-rose-600 hover:text-rose-700 hover:underline flex items-center gap-1 cursor-pointer disabled:opacity-50"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                        <span>Cancel Order</span>
                      </button>
                    )}
                  </div>
                </div>

                {/* Items Preview */}
                <div className="p-5 space-y-4">
                  <div className="divide-y divide-slate-100">
                    {order.items.map((item) => (
                      <div key={item.id} className="py-2.5 flex items-center justify-between text-sm">
                        <div className="space-y-0.5">
                          <span className="font-semibold text-slate-900">{item.productName}</span>
                          <div className="text-xs text-slate-400">
                            SKU: {item.productSku} • {item.quantity} unit{item.quantity > 1 ? 's' : ''} @ {formatCurrency(item.unitPrice, order.currency)} each
                          </div>
                        </div>
                        <span className="font-bold text-slate-900">
                          {formatCurrency(item.lineTotal, order.currency)}
                        </span>
                      </div>
                    ))}
                  </div>

                  <div className="pt-3 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between text-xs text-slate-500 gap-2">
                    <div className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-emerald-700 shrink-0" />
                      <span className="truncate max-w-md">{order.shippingAddress}</span>
                    </div>

                    <Link
                      to={`/orders/${order.orderNumber}`}
                      className="font-bold text-emerald-700 hover:text-emerald-800 flex items-center gap-1 self-end sm:self-auto"
                    >
                      <span>Full Details & Invoice</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}

          {totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 pt-4">
              <button
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
                className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-30"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <span className="text-xs font-semibold text-slate-600">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
                className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-30"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
