import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { Layout } from './components/layout/Layout';
import { ProtectedRoute } from './components/layout/ProtectedRoute';
import { AdminRoute } from './components/layout/AdminRoute';

// Public & Customer Pages
import { HomePage } from './pages/HomePage';
import { ProductsPage } from './pages/ProductsPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { CampaignsPage } from './pages/CampaignsPage';
import { CampaignDetailPage } from './pages/CampaignDetailPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { OrderConfirmationPage } from './pages/OrderConfirmationPage';
import { OrdersPage } from './pages/OrdersPage';
import { OrderDetailPage } from './pages/OrderDetailPage';
import { MyParticipationsPage } from './pages/MyParticipationsPage';
import { ProfilePage } from './pages/ProfilePage';
import { NotFoundPage } from './pages/NotFoundPage';

// Admin Pages
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminProductsPage } from './pages/admin/AdminProductsPage';
import { AdminCategoriesPage } from './pages/admin/AdminCategoriesPage';
import { AdminCampaignsPage } from './pages/admin/AdminCampaignsPage';
import { AdminOrdersPage } from './pages/admin/AdminOrdersPage';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Routes>
            <Route path="/" element={<Layout />}>
              {/* Public Routes */}
              <Route index element={<HomePage />} />
              <Route path="products" element={<ProductsPage />} />
              <Route path="products/:id" element={<ProductDetailPage />} />
              <Route path="campaigns" element={<CampaignsPage />} />
              <Route path="campaigns/:id" element={<CampaignDetailPage />} />
              <Route path="login" element={<LoginPage />} />
              <Route path="register" element={<RegisterPage />} />
              <Route path="cart" element={<CartPage />} />

              {/* Customer Protected Routes */}
              <Route
                path="checkout"
                element={
                  <ProtectedRoute>
                    <CheckoutPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="orders/confirmed"
                element={
                  <ProtectedRoute>
                    <OrderConfirmationPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="orders/confirmed/:orderNumber"
                element={
                  <ProtectedRoute>
                    <OrderConfirmationPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="orders/confirmation/:orderNumber"
                element={
                  <ProtectedRoute>
                    <OrderConfirmationPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="orders"
                element={
                  <ProtectedRoute>
                    <OrdersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="orders/:orderNumber"
                element={
                  <ProtectedRoute>
                    <OrderDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="participations"
                element={
                  <ProtectedRoute>
                    <MyParticipationsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Admin Protected Routes */}
              <Route
                path="admin"
                element={
                  <AdminRoute>
                    <AdminDashboardPage />
                  </AdminRoute>
                }
              />
              <Route
                path="admin/products"
                element={
                  <AdminRoute>
                    <AdminProductsPage />
                  </AdminRoute>
                }
              />
              <Route
                path="admin/categories"
                element={
                  <AdminRoute>
                    <AdminCategoriesPage />
                  </AdminRoute>
                }
              />
              <Route
                path="admin/campaigns"
                element={
                  <AdminRoute>
                    <AdminCampaignsPage />
                  </AdminRoute>
                }
              />
              <Route
                path="admin/orders"
                element={
                  <AdminRoute>
                    <AdminOrdersPage />
                  </AdminRoute>
                }
              />

              {/* Catch-all 404 Route */}
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
