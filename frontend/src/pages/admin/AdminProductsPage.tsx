import React, { useEffect, useState } from 'react';
import { Package, Plus, Edit2, Trash2, Search, RefreshCw } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import { Product, ProductCreateRequest, ProductUpdateRequest } from '../../types/product';
import { Category } from '../../types/category';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Badge } from '../../components/common/Badge';
import { formatCurrency } from '../../utils/currency';

export const AdminProductsPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');

  // Form modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [formData, setFormData] = useState<{
    sku: string;
    name: string;
    description: string;
    basePrice: string;
    totalInventory: string;
    availableInventory: string;
    categoryId: string;
    imageUrl: string;
    status: 'ACTIVE' | 'ARCHIVED' | 'OUT_OF_STOCK';
  }>({
    sku: '',
    name: '',
    description: '',
    basePrice: '',
    totalInventory: '',
    availableInventory: '',
    categoryId: '',
    imageUrl: '',
    status: 'ACTIVE',
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [prodRes, catRes] = await Promise.all([
        productApi.getProducts({
          search: searchTerm || undefined,
          categoryId: selectedCategory ? Number(selectedCategory) : undefined,
          size: 50,
        }),
        categoryApi.getAllCategoriesAdmin(),
      ]);
      setProducts(prodRes.content);
      setCategories(catRes);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedCategory]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchData();
  };

  const openCreateModal = () => {
    setEditingProduct(null);
    setFormData({
      sku: `SKU-${Date.now().toString().slice(-6)}`,
      name: '',
      description: '',
      basePrice: '',
      totalInventory: '100',
      availableInventory: '100',
      categoryId: categories[0]?.id?.toString() || '',
      imageUrl: '',
      status: 'ACTIVE',
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const openEditModal = (prod: Product) => {
    setEditingProduct(prod);
    setFormData({
      sku: prod.sku,
      name: prod.name,
      description: prod.description || '',
      basePrice: prod.basePrice.toString(),
      totalInventory: prod.totalInventory.toString(),
      availableInventory: prod.availableInventory.toString(),
      categoryId: prod.categoryId?.toString() || '',
      imageUrl: prod.imageUrl || '',
      status: prod.status,
    });
    setFormError(null);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingProduct(null);
    setFormError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const priceNum = parseFloat(formData.basePrice);
    const totalInv = parseInt(formData.totalInventory, 10);
    const availInv = parseInt(formData.availableInventory, 10);

    if (isNaN(priceNum) || priceNum <= 0) {
      setFormError('Base price must be a positive number.');
      return;
    }
    if (isNaN(totalInv) || totalInv < 0 || isNaN(availInv) || availInv < 0) {
      setFormError('Inventory values must be non-negative integers.');
      return;
    }

    try {
      setSubmitting(true);
      setFormError(null);

      if (editingProduct) {
        const updatePayload: ProductUpdateRequest = {
          name: formData.name,
          description: formData.description,
          basePrice: priceNum,
          totalInventory: totalInv,
          availableInventory: availInv,
          categoryId: formData.categoryId ? Number(formData.categoryId) : undefined,
          status: formData.status,
          imageUrl: formData.imageUrl || undefined,
        };
        await productApi.updateProduct(editingProduct.id, updatePayload);
        setActionSuccess(`Product "${formData.name}" updated successfully.`);
      } else {
        const createPayload: ProductCreateRequest = {
          sku: formData.sku,
          name: formData.name,
          description: formData.description,
          basePrice: priceNum,
          currency: 'INR',
          totalInventory: totalInv,
          availableInventory: availInv,
          categoryId: formData.categoryId ? Number(formData.categoryId) : undefined,
          imageUrl: formData.imageUrl || undefined,
        };
        await productApi.createProduct(createPayload);
        setActionSuccess(`Product "${formData.name}" created successfully.`);
      }

      closeModal();
      await fetchData();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setFormError(e.response?.data?.message || e.message || 'Failed to save product');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (prod: Product) => {
    if (!window.confirm(`Are you sure you want to delete/archive product "${prod.name}"?`)) {
      return;
    }

    try {
      setError(null);
      await productApi.deleteProduct(prod.id);
      setActionSuccess(`Product "${prod.name}" removed successfully.`);
      await fetchData();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e.response?.data?.message || e.message || 'Failed to delete product');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2">
            <Package className="w-6 h-6 text-blue-600" />
            Products Management
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Create products, manage SKU inventory, and control base catalog pricing.
          </p>
        </div>
        <button
          onClick={openCreateModal}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-emerald-600 text-white font-medium hover:bg-emerald-700 transition shadow-sm text-sm"
        >
          <Plus className="w-4 h-4" />
          Add Product
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

      {/* Search & Filter Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 flex flex-col md:flex-row gap-3">
        <form onSubmit={handleSearchSubmit} className="flex-1 relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by product name..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
        </form>
        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          className="px-3.5 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none bg-white text-slate-700"
        >
          <option value="">All Categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={fetchData}
          className="px-4 py-2 border border-slate-300 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition flex items-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {loading ? (
        <div className="py-20 flex justify-center">
          <LoadingSpinner size="lg" text="Loading products..." />
        </div>
      ) : (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-slate-700 font-semibold text-xs uppercase border-b border-slate-200">
                <tr>
                  <th className="px-6 py-4">Product</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">SKU</th>
                  <th className="px-6 py-4">Base Price</th>
                  <th className="px-6 py-4">Inventory</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {products.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-8 text-center text-slate-400">
                      No products found.
                    </td>
                  </tr>
                ) : (
                  products.map((p) => (
                    <tr key={p.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <img
                            src={p.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=100&auto=format&fit=crop&q=60'}
                            alt={p.name}
                            className="w-10 h-10 object-cover rounded-lg border border-slate-100"
                          />
                          <div>
                            <span className="font-semibold text-slate-900 block">{p.name}</span>
                            <span className="text-xs text-slate-400 font-mono">ID: #{p.id}</span>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-slate-600">{p.categoryName || 'Uncategorized'}</td>
                      <td className="px-6 py-4 font-mono text-xs text-slate-500">{p.sku}</td>
                      <td className="px-6 py-4 font-semibold text-slate-900">
                        {formatCurrency(p.basePrice, p.currency)}
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-xs">
                          <span className="font-semibold text-slate-800">{p.availableInventory}</span>
                          <span className="text-slate-400"> / {p.totalInventory} left</span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Badge
                          variant={
                            p.status === 'ACTIVE' ? 'success' : p.status === 'OUT_OF_STOCK' ? 'warning' : 'neutral'
                          }
                        >
                          {p.status}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => openEditModal(p)}
                            className="p-1.5 text-slate-500 hover:text-emerald-600 rounded hover:bg-slate-100 transition"
                            title="Edit Product"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDelete(p)}
                            className="p-1.5 text-slate-500 hover:text-red-600 rounded hover:bg-red-50 transition"
                            title="Delete Product"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs overflow-y-auto">
          <div className="bg-white w-full max-w-lg rounded-2xl p-6 shadow-xl border border-slate-200 my-8">
            <h2 className="text-lg font-bold text-slate-900 mb-4">
              {editingProduct ? 'Edit Product' : 'Add New Product'}
            </h2>

            {formError && (
              <div className="mb-4">
                <ErrorAlert message={formError} />
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Product Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                    placeholder="e.g. Wireless Headphones"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    SKU {!editingProduct && '*'}
                  </label>
                  <input
                    type="text"
                    required={!editingProduct}
                    disabled={!!editingProduct}
                    value={formData.sku}
                    onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none font-mono disabled:bg-slate-100"
                    placeholder="SKU-100234"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Category
                  </label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none bg-white"
                  >
                    <option value="">Select Category</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Base Price (INR ₹) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={formData.basePrice}
                    onChange={(e) => setFormData({ ...formData, basePrice: e.target.value })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none font-mono"
                    placeholder="1999"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Total Inventory *
                  </label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formData.totalInventory}
                    onChange={(e) => setFormData({ ...formData, totalInventory: e.target.value })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Available Inventory *
                  </label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={formData.availableInventory}
                    onChange={(e) => setFormData({ ...formData, availableInventory: e.target.value })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none font-mono"
                  />
                </div>
              </div>

              {editingProduct && (
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                    Status
                  </label>
                  <select
                    value={formData.status}
                    onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none bg-white"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="OUT_OF_STOCK">OUT_OF_STOCK</option>
                    <option value="ARCHIVED">ARCHIVED</option>
                  </select>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Image URL
                </label>
                <input
                  type="url"
                  value={formData.imageUrl}
                  onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  placeholder="https://images.unsplash.com/..."
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none resize-none"
                  placeholder="Product specifications and details..."
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={closeModal}
                  className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2 text-sm font-semibold rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition"
                >
                  {submitting ? 'Saving...' : editingProduct ? 'Save Changes' : 'Create Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

