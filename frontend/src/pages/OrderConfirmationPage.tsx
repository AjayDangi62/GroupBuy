import React, { useEffect, useState } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { Order } from '../types/order';
import { apiClient } from '../api/client';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { Badge } from '../components/common/Badge';
import {
  CheckCircle2,
  Package,
  MapPin,
  CreditCard,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react';
import { formatCurrency } from '../utils/currency';

export const OrderConfirmationPage: React.FC = () => {
  const { orderNumber: paramOrderNumber } = useParams<{ orderNumber: string }>();
  const [searchParams] = useSearchParams();
  const orderNumber = paramOrderNumber || searchParams.get('orderNumber');

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [paymentLoading, setPaymentLoading] = useState<boolean>(false);
  const [paymentSuccess, setPaymentSuccess] = useState<boolean>(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  useEffect(() => {
    const fetchOrder = async () => {
      if (!orderNumber) {
        setError('No order specified.');
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const data = await orderApi.getOrderByNumber(orderNumber);
        setOrder(data);
      } catch {
        setError('Order not found or access restricted.');
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();
  }, [orderNumber]);

  const handleSimulatePayment = async () => {
    if (!order) return;
    setPaymentLoading(true);
    setPaymentError(null);

    try {
      // 1. Initiate payment with the backend
      const { data: initData } = await apiClient.post('/api/v1/payments/initiate', {
        orderId: order.id,
        paymentGateway: 'MOCK',
      });

      // 2. Verify / simulate gateway settlement
      await apiClient.post('/api/v1/payments/verify', {
        transactionId: initData.transactionId || `tx_${Date.now()}`,
        status: 'SUCCESS',
        gatewaySignature: 'sig_mock_signature_valid',
      });

      setPaymentSuccess(true);
      // Refresh order
      const refreshed = await orderApi.getOrderByNumber(order.orderNumber);
      setOrder(refreshed);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setPaymentError(e.response?.data?.message || 'Payment simulation failed.');
    } finally {
      setPaymentLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullScreen message="Loading order details..." />;
  }

  if (error || !order) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center bg-white">
        <ErrorAlert message={error || 'Order not found'} />
        <Link to="/" className="text-sm font-semibold text-emerald-700 hover:underline mt-4 inline-block">
          Return to Home
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 space-y-8 bg-white min-h-[80vh]">
      {/* Success banner */}
      <div className="text-center space-y-3">
        <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-700 mx-auto flex items-center justify-center">
          <CheckCircle2 className="w-10 h-10" />
        </div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Order Confirmed!</h1>
        <p className="text-sm text-slate-500 max-w-md mx-auto">
          Thank you for your order. We’ve received your request and reserved inventory on the server.
        </p>
        <div className="inline-block bg-slate-50 border border-slate-200 px-4 py-1.5 rounded-full text-xs font-mono font-bold text-slate-700">
          Order #{order.orderNumber}
        </div>
      </div>

      {paymentError && <ErrorAlert message={paymentError} onDismiss={() => setPaymentError(null)} />}

      {/* Payment Action if order is still in CREATED state */}
      {order.status === 'CREATED' && (
        <div className="bg-emerald-50 border border-emerald-200 rounded-3xl p-6 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-sm">Complete Payment Simulation</h3>
              <p className="text-xs text-emerald-800 mt-0.5">
                Simulate payment authorization with the server-side payment engine.
              </p>
            </div>
          </div>
          <button
            onClick={handleSimulatePayment}
            disabled={paymentLoading}
            className="shrink-0 bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-sm px-5 py-3 rounded-xl transition-all shadow-md flex items-center gap-2 cursor-pointer disabled:opacity-50"
          >
            {paymentLoading ? 'Authorizing...' : 'Pay with Sandbox Gateway'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Invoice Details */}
      <div className="bg-white border border-slate-200 rounded-3xl p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <Package className="w-5 h-5 text-emerald-700" />
            <h2 className="font-bold text-slate-900 text-base">Items Summary</h2>
          </div>
          <Badge status={order.status} />
        </div>

        <div className="divide-y divide-slate-100">
          {order.items.map((item) => (
            <div key={item.id} className="py-4 flex items-center justify-between gap-4">
              <div>
                <h4 className="font-bold text-slate-900 text-sm">{item.productName}</h4>
                <div className="text-xs text-slate-400 mt-0.5 font-mono">
                  SKU: {item.productSku} • Qty: {item.quantity} × {formatCurrency(item.unitPrice, order.currency)}
                </div>
              </div>
              <div className="font-extrabold text-slate-900 text-sm">
                {formatCurrency(item.lineTotal, order.currency)}
              </div>
            </div>
          ))}
        </div>

        <div className="pt-4 border-t border-slate-100 space-y-2 text-xs">
          <div className="flex justify-between text-slate-600">
            <span>Subtotal:</span>
            <span>{formatCurrency(order.totalPrice, order.currency)}</span>
          </div>
          <div className="flex justify-between text-slate-600">
            <span>Delivery:</span>
            <span className="text-emerald-700 font-bold">FREE</span>
          </div>
          <div className="flex justify-between text-base font-extrabold text-slate-900 pt-2 border-t border-slate-100">
            <span>Grand Total:</span>
            <span>{formatCurrency(order.totalPrice, order.currency)}</span>
          </div>
        </div>

        {/* Shipping Address */}
        <div className="pt-4 border-t border-slate-100 flex items-start gap-3">
          <MapPin className="w-4 h-4 text-emerald-700 mt-0.5 shrink-0" />
          <div className="text-xs">
            <span className="font-bold text-slate-900 block">Delivery Destination:</span>
            <span className="text-slate-600">{order.shippingAddress}</span>
          </div>
        </div>
      </div>

      {/* Bottom Shortcuts */}
      <div className="flex flex-wrap items-center justify-between gap-4 pt-4">
        <Link
          to="/orders"
          className="text-sm font-semibold text-emerald-700 hover:text-emerald-800 flex items-center gap-1.5"
        >
          <span>View all orders</span>
          <ArrowRight className="w-4 h-4" />
        </Link>

        <Link
          to="/products"
          className="bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-sm px-6 py-2.5 rounded-xl transition"
        >
          Continue Shopping
        </Link>
      </div>
    </div>
  );
};
