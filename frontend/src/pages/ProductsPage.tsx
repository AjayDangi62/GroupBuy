import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { productApi } from '../api/productApi';
import { categoryApi } from '../api/categoryApi';
import { Product } from '../types/product';
import { Category } from '../types/category';
import { ProductCard } from '../components/product/ProductCard';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { MOCK_PRODUCTS, MOCK_CATEGORIES } from '../mock/mockData';
import { Search, ArrowUpDown, ChevronLeft, ChevronRight, X } from 'lucide-react';

export const ProductsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  const searchParam = searchParams.get('search') || '';
  const categoryIdParam = searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined;
  const pageParam = searchParams.get('page') ? Number(searchParams.get('page')) : 0;
  const sortParam = searchParams.get('sort') || 'createdAt,desc';

  const [searchInput, setSearchInput] = useState(searchParam);

  const fetchCategories = async () => {
    try {
      const data = await categoryApi.getActiveCategories();
      if (data && data.length > 0) {
        setCategories(data);
      } else {
        setCategories(MOCK_CATEGORIES);
      }
    } catch {
      setCategories(MOCK_CATEGORIES);
    }
  };

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await productApi.getProducts({
        search: searchParam || undefined,
        categoryId: categoryIdParam,
        page: pageParam,
        size: 12,
        sort: sortParam,
      });

      if (res && res.content && res.content.length > 0) {
        setProducts(res.content);
        setTotalPages(res.totalPages || 1);
        setTotalElements(res.totalElements || 0);
      } else {
        // Fallback to client filtered mock data
        let filtered = [...MOCK_PRODUCTS];
        if (searchParam) {
          filtered = filtered.filter((p) =>
            p.name.toLowerCase().includes(searchParam.toLowerCase()) ||
            p.sku.toLowerCase().includes(searchParam.toLowerCase())
          );
        }
        if (categoryIdParam) {
          filtered = filtered.filter((p) => p.categoryId === categoryIdParam);
        }
        setProducts(filtered);
        setTotalPages(1);
        setTotalElements(filtered.length);
      }
    } catch {
      // Offline / API error fallback
      let filtered = [...MOCK_PRODUCTS];
      if (searchParam) {
        filtered = filtered.filter((p) =>
          p.name.toLowerCase().includes(searchParam.toLowerCase()) ||
          p.sku.toLowerCase().includes(searchParam.toLowerCase())
        );
      }
      if (categoryIdParam) {
        filtered = filtered.filter((p) => p.categoryId === categoryIdParam);
      }
      setProducts(filtered);
      setTotalPages(1);
      setTotalElements(filtered.length);
    } finally {
      setLoading(false);
    }
  }, [searchParam, categoryIdParam, pageParam, sortParam]);

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const updateParam = (key: string, value?: string | number) => {
    const next = new URLSearchParams(searchParams);
    if (value !== undefined && value !== '') {
      next.set(key, String(value));
    } else {
      next.delete(key);
    }
    if (key !== 'page') {
      next.delete('page');
    }
    setSearchParams(next);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateParam('search', searchInput.trim());
  };

  const clearFilters = () => {
    setSearchInput('');
    setSearchParams({});
  };

  const activeCategory = categories.find((c) => c.id === categoryIdParam);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8 bg-white min-h-[80vh]">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Product Catalog</h1>
          <p className="text-sm text-slate-500 mt-1">
            Browse our wide selection of consumer goods and tech products available for immediate retail purchase or group buying.
          </p>
        </div>

        {/* Total count badge */}
        <div className="text-sm font-semibold text-emerald-800 bg-emerald-50 border border-emerald-200 px-3.5 py-1.5 rounded-xl self-start sm:self-auto">
          {totalElements} {totalElements === 1 ? 'Product' : 'Products'} Found
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white border border-slate-200 rounded-2xl p-4 shadow-xs flex flex-col md:flex-row gap-4 justify-between items-center">
        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} className="relative w-full md:max-w-md">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by product name or SKU..."
            className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 pl-9 pr-20 text-sm text-slate-800 focus:outline-hidden focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
          />
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
          <button
            type="submit"
            className="absolute right-1.5 top-1.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold px-3 py-1 rounded-lg transition-colors"
          >
            Search
          </button>
        </form>

        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          {/* Category Dropdown */}
          <select
            value={categoryIdParam || ''}
            onChange={(e) => updateParam('categoryId', e.target.value ? Number(e.target.value) : undefined)}
            className="bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>

          {/* Sort Dropdown */}
          <div className="relative flex items-center">
            <ArrowUpDown className="w-3.5 h-3.5 text-slate-400 absolute left-3 pointer-events-none" />
            <select
              value={sortParam}
              onChange={(e) => updateParam('sort', e.target.value)}
              className="bg-slate-50 border border-slate-200 rounded-xl pl-8 pr-4 py-2 text-sm text-slate-700 focus:outline-hidden focus:ring-2 focus:ring-emerald-500"
            >
              <option value="createdAt,desc">Newest First</option>
              <option value="basePrice,asc">Price: Low to High</option>
              <option value="basePrice,desc">Price: High to Low</option>
              <option value="name,asc">Name: A-Z</option>
            </select>
          </div>
        </div>
      </div>

      {/* Active Filter Tags */}
      {(searchParam || activeCategory) && (
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs font-semibold text-slate-400">Active Filters:</span>
          {searchParam && (
            <span className="inline-flex items-center gap-1 bg-emerald-50 text-emerald-800 text-xs px-2.5 py-1 rounded-full font-medium border border-emerald-200">
              Keyword: "{searchParam}"
              <button onClick={() => updateParam('search', undefined)} className="hover:text-emerald-950">
                <X className="w-3 h-3" />
              </button>
            </span>
          )}
          {activeCategory && (
            <span className="inline-flex items-center gap-1 bg-emerald-50 text-emerald-800 text-xs px-2.5 py-1 rounded-full font-medium border border-emerald-200">
              Category: {activeCategory.name}
              <button onClick={() => updateParam('categoryId', undefined)} className="hover:text-emerald-950">
                <X className="w-3 h-3" />
              </button>
            </span>
          )}
          <button
            onClick={clearFilters}
            className="text-xs text-slate-500 hover:text-emerald-700 underline font-semibold ml-2"
          >
            Clear All
          </button>
        </div>
      )}

      {/* Product Grid / Loading / Empty */}
      {loading ? (
        <div className="py-24 flex justify-center">
          <LoadingSpinner size="lg" text="Loading product catalog..." />
        </div>
      ) : products.length === 0 ? (
        <EmptyState
          title="No Products Found"
          description="We couldn't find any products matching your selected search or filters. Try adjusting your search query."
          actionText="Reset Filters"
          onAction={clearFilters}
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="pt-8 flex items-center justify-center gap-3">
          <button
            onClick={() => updateParam('page', Math.max(0, pageParam - 1))}
            disabled={pageParam === 0}
            className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <span className="text-sm font-semibold text-slate-700">
            Page {pageParam + 1} of {totalPages}
          </span>
          <button
            onClick={() => updateParam('page', Math.min(totalPages - 1, pageParam + 1))}
            disabled={pageParam >= totalPages - 1}
            className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      )}
    </div>
  );
};
