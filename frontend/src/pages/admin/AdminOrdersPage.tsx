import React, { useEffect, useState } from 'react';
import { ShoppingBag, RefreshCw, Eye, ChevronDown } from 'lucide-react';
import { Link } from 'react-router-dom';
import { orderApi } from '../../api/orderApi';
import { Order } from '../../types/order';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Badge } from '../../components/common/Badge';
import { formatCurrency } from '../../utils/currency';

export const AdminOrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [updatingOrderId, setUpdatingOrderId] = useState<number | null>(null);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await orderApi.getAllOrdersAdmin(0, 50);
      setOrders(res.content);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleStatusChange = async (order: Order, newStatus: Order['status']) => {
    if (order.status === newStatus) return;

    try {
      setUpdatingOrderId(order.id);
      setError(null);
      await orderApi.updateOrderStatusAdmin(order.id, newStatus);
      setActionSuccess(`Order #${order.orderNumber} status changed to ${newStatus}.`);
      await fetchOrders();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to update order status');
    } finally {
      setUpdatingOrderId(null);
    }
  };

  const getStatusBadgeVariant = (status: Order['status']) => {
    switch (status) {
      case 'DELIVERED':
        return 'success';
      case 'PAID':
      case 'PROCESSING':
      case 'SHIPPED':
        return 'info';
      case 'CREATED':
        return 'warning';
      case 'CANCELLED':
      default:
        return 'danger';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <ShoppingBag className="w-6 h-6 text-amber-600" />
            Orders Management
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Review customer orders, inspect line items, and manage fulfillment workflow.
          </p>
        </div>
        <button
          onClick={fetchOrders}
          className="inline-flex items-center gap-2 px-4 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {actionSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-sm flex items-center justify-between">
          <span>{actionSuccess}</span>
          <button onClick={() => setActionSuccess(null)} className="text-emerald-600 hover:text-emerald-900 font-bold ml-2">
            ×
          </button>
        </div>
      )}

      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}

      {loading ? (
        <div className="py-20 flex justify-center">
          <LoadingSpinner size="lg" text="Loading orders..." />
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 font-semibold text-xs uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-4">Order #</th>
                  <th className="px-6 py-4">Date</th>
                  <th className="px-6 py-4">User ID</th>
                  <th className="px-6 py-4">Items</th>
                  <th className="px-6 py-4">Total</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-8 text-center text-slate-400">
                      No orders found in the platform.
                    </td>
                  </tr>
                ) : (
                  orders.map((o) => {
                    const isUpdating = updatingOrderId === o.id;
                    const totalQty = o.items.reduce((acc, item) => acc + item.quantity, 0);

                    return (
                      <tr key={o.id} className="hover:bg-slate-50/60 transition-colors">
                        <td className="px-6 py-4 font-mono font-semibold text-slate-900">
                          {o.orderNumber}
                        </td>
                        <td className="px-6 py-4 text-xs text-slate-500">
                          {new Date(o.createdAt).toLocaleDateString()} {new Date(o.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </td>
                        <td className="px-6 py-4 font-mono text-xs text-slate-500">
                          User #{o.userId}
                        </td>
                        <td className="px-6 py-4 text-slate-700">
                          {totalQty} {totalQty === 1 ? 'item' : 'items'}
                        </td>
                        <td className="px-6 py-4 font-semibold text-slate-900">
                          {formatCurrency(o.totalPrice, o.currency)}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <Badge variant={getStatusBadgeVariant(o.status)}>
                              {o.status}
                            </Badge>
                            <div className="relative">
                              <select
                                disabled={isUpdating}
                                value={o.status}
                                onChange={(e) => handleStatusChange(o, e.target.value as Order['status'])}
                                className="text-xs border border-slate-300 rounded px-2 py-1 bg-white hover:bg-slate-50 focus:outline-none focus:ring-1 focus:ring-emerald-500 font-medium text-slate-700 disabled:opacity-50"
                              >
                                <option value="CREATED">CREATED</option>
                                <option value="PAID">PAID</option>
                                <option value="PROCESSING">PROCESSING</option>
                                <option value="SHIPPED">SHIPPED</option>
                                <option value="DELIVERED">DELIVERED</option>
                                <option value="CANCELLED">CANCELLED</option>
                              </select>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <Link
                            to={`/orders/${o.orderNumber}`}
                            className="inline-flex items-center gap-1 p-1.5 text-slate-500 hover:text-emerald-600 rounded hover:bg-slate-100 transition"
                            title="View Full Order Summary"
                          >
                            <Eye className="w-4 h-4" />
                          </Link>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

