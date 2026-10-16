import { useEffect, useMemo, useState } from 'react';
import {
  Search,
  Plus,
  Package,
  ArrowDownToLine,
  ArrowUpToLine,
  AlertTriangle,
  Pencil,
  Trash2,
  History,
  X,
  Save,
} from 'lucide-react';
import ResponsiveTable from '../components/ui/ResponsiveTable';
import type { ResponsiveColumn } from '../components/ui/ResponsiveTable';
import { cn } from '../lib/utils';
import { useToast } from '../components/ui/Toast';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { usePermissions } from '../context/PermissionContext';
import {
  getAllProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  adjustStock,
  getProductTransactions,
} from '../services/productService';
import type { Product, ProductPayload, InventoryTransactionInfo } from '../services/productService';

function Spinner({ className }: { className?: string }) {
  return (
    <svg className={cn('h-3.5 w-3.5 animate-spin', className)} viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  );
}

const formatMoney = (n?: number) =>
  new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(n || 0);

interface ProductForm {
  code: string;
  name: string;
  description: string;
  brand: string;
  category: string;
  purchasePrice: string;
  salePrice: string;
  currentStock: string;
  minStock: string;
  active: boolean;
}

const emptyForm = (): ProductForm => ({
  code: '',
  name: '',
  description: '',
  brand: '',
  category: '',
  purchasePrice: '',
  salePrice: '',
  currentStock: '0',
  minStock: '5',
  active: true,
});

export default function Inventory() {
  const toast = useToast();
  const { hasPermission, hasAny } = usePermissions();

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showInactive, setShowInactive] = useState(false);

  // Crear / editar producto
  const [formOpen, setFormOpen] = useState<'create' | 'edit' | null>(null);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [form, setForm] = useState<ProductForm>(emptyForm);
  const [initialForm, setInitialForm] = useState<ProductForm>(emptyForm);
  const [saving, setSaving] = useState(false);

  // Confirmación para no perder datos al cerrar un modal con cambios sin guardar.
  const [discard, setDiscard] = useState<{
    title: string;
    message: string;
    confirmLabel: string;
    action: () => void;
  } | null>(null);

  // Movimiento de stock
  const [stockProduct, setStockProduct] = useState<Product | null>(null);
  const [stockType, setStockType] = useState<'IN' | 'OUT'>('IN');
  const [quantity, setQuantity] = useState('');
  const [reference, setReference] = useState('');
  const [savingStock, setSavingStock] = useState(false);

  // Historial
  const [historyProduct, setHistoryProduct] = useState<Product | null>(null);
  const [transactions, setTransactions] = useState<InventoryTransactionInfo[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  const refresh = async () => {
    try {
      const data = await getAllProducts({ incluirInactivos: showInactive });
      setProducts(data);
    } catch {
      toast.error('Error', 'No se pudieron cargar los productos.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showInactive]);

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim();
    if (!q) return products;
    return products.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.code.toLowerCase().includes(q) ||
        (p.brand || '').toLowerCase().includes(q) ||
        (p.category || '').toLowerCase().includes(q)
    );
  }, [products, search]);

  const stats = useMemo(() => {
    const total = products.length;
    const lowStock = products.filter((p) => p.currentStock <= p.minStock).length;
    const value = products.reduce((acc, p) => acc + (p.salePrice || 0) * p.currentStock, 0);
    return { total, lowStock, value };
  }, [products]);

  const canCreate = hasPermission('PRODUCTOS:CREAR');
  const canEdit = hasPermission('PRODUCTOS:EDITAR');
  const canDelete = hasPermission('PRODUCTOS:ELIMINAR');
  const canMove = hasAny(['MOVIMIENTOS:CREAR', 'PRODUCTOS:EDITAR']);
  const canSeeHistory = hasPermission('MOVIMIENTOS:VER');

  const openCreate = () => {
    setEditingProduct(null);
    const base = emptyForm();
    setForm(base);
    setInitialForm(base);
    setFormOpen('create');
  };

  const openEdit = (p: Product) => {
    setEditingProduct(p);
    const base: ProductForm = {
      code: p.code,
      name: p.name,
      description: p.description || '',
      brand: p.brand || '',
      category: p.category || '',
      purchasePrice: String(p.purchasePrice ?? ''),
      salePrice: String(p.salePrice ?? ''),
      currentStock: String(p.currentStock ?? '0'),
      minStock: String(p.minStock ?? '5'),
      active: p.active,
    };
    setForm(base);
    setInitialForm(base);
    setFormOpen('edit');
  };

  const closeForm = () => {
    setFormOpen(null);
    setEditingProduct(null);
  };

  // Solo cierra si no hay cambios; si los hay, pide confirmación.
  const requestCloseForm = () => {
    if (JSON.stringify(form) !== JSON.stringify(initialForm)) {
      setDiscard({
        title: '¿Descartar cambios?',
        message: 'Se perderán los datos del producto que ingresaste.',
        confirmLabel: 'Descartar',
        action: closeForm,
      });
    } else {
      closeForm();
    }
  };

  const requestCloseMovement = () => {
    if (quantity.trim() !== '' || reference.trim() !== '') {
      setDiscard({
        title: '¿Descartar movimiento?',
        message: 'Se perderá el movimiento de stock que ingresaste.',
        confirmLabel: 'Descartar',
        action: () => setStockProduct(null),
      });
    } else {
      setStockProduct(null);
    }
  };

  const saveProduct = async () => {
    if (!form.code.trim() || !form.name.trim()) {
      toast.error('Validación', 'El código y el nombre son obligatorios.');
      return;
    }
    const purchase = Number(form.purchasePrice);
    const sale = Number(form.salePrice);
    if (Number.isNaN(purchase) || purchase < 0) {
      toast.error('Validación', 'Ingresa un precio de compra válido (≥ 0).');
      return;
    }
    if (Number.isNaN(sale) || sale <= 0) {
      toast.error('Validación', 'Ingresa un precio de venta válido (> 0).');
      return;
    }
    setSaving(true);
    try {
      const payload: ProductPayload = {
        code: form.code.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        brand: form.brand.trim() || undefined,
        category: form.category.trim() || undefined,
        purchasePrice: purchase,
        salePrice: sale,
        minStock: Number(form.minStock) || 0,
        active: form.active,
      };
      if (formOpen === 'create') {
        payload.currentStock = Number(form.currentStock) || 0;
        payload.active = true;
        await createProduct(payload);
        toast.success('Producto creado', `«${form.name.trim()}» se registró en el inventario.`);
      } else if (editingProduct) {
        await updateProduct(editingProduct.id, payload);
        toast.success('Producto actualizado', `«${form.name.trim()}» se actualizó correctamente.`);
      }
      closeForm();
      await refresh();
    } catch (err: any) {
      toast.error('Error', err?.response?.data?.message || 'No se pudo guardar el producto.');
    } finally {
      setSaving(false);
    }
  };

  const openMovement = (p: Product, type: 'IN' | 'OUT') => {
    setStockProduct(p);
    setStockType(type);
    setQuantity('');
    setReference('');
  };

  const saveMovement = async () => {
    if (!stockProduct) return;
    const qty = Number(quantity);
    if (!qty || qty < 1) {
      toast.error('Validación', 'La cantidad debe ser mayor a 0.');
      return;
    }
    setSavingStock(true);
    try {
      await adjustStock(stockProduct.id, {
        quantity: qty,
        type: stockType,
        reference: reference.trim() || undefined,
      });
      toast.success(
        'Movimiento registrado',
        `${stockType === 'IN' ? 'Entrada' : 'Salida'} de ${qty} unidad(es) de «${stockProduct.name}».`
      );
      setStockProduct(null);
      await refresh();
    } catch (err: any) {
      toast.error('Error', err?.response?.data?.message || 'No se pudo registrar el movimiento.');
    } finally {
      setSavingStock(false);
    }
  };

  const removeProduct = async (p: Product) => {
    if (!window.confirm(`¿Desactivar el producto «${p.name}»? Puedes volver a activarlo editándolo.`)) return;
    try {
      await deleteProduct(p.id);
      toast.success('Producto desactivado', `«${p.name}» ya no aparece en el inventario activo.`);
      await refresh();
    } catch (err: any) {
      toast.error('Error', err?.response?.data?.message || 'No se pudo desactivar el producto.');
    }
  };

  const openHistory = async (p: Product) => {
    setHistoryProduct(p);
    setTransactions([]);
    setLoadingHistory(true);
    try {
      setTransactions(await getProductTransactions(p.id));
    } catch {
      toast.error('Error', 'No se pudo cargar el historial de movimientos.');
    } finally {
      setLoadingHistory(false);
    }
  };

  const columns: ResponsiveColumn<Product>[] = [
    {
      key: 'product',
      header: 'Código / Producto',
      render: (p) => (
        <div className="flex items-center">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-slate-200 bg-slate-100 text-slate-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-400">
            <Package className="h-5 w-5" />
          </div>
          <div className="ml-4">
            <div className="text-sm font-medium text-slate-900 dark:text-white">{p.name}</div>
            <div className="text-xs text-slate-500 dark:text-slate-400">{p.code}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'brand',
      header: 'Marca / Cat.',
      render: (p) => (
        <span className="text-xs text-slate-500 dark:text-slate-400">
          {p.brand || '—'}
          {p.category && <span className="ml-1 block text-[10px] uppercase text-slate-400 dark:text-slate-500">{p.category}</span>}
        </span>
      ),
    },
    {
      key: 'stock',
      header: 'Stock',
      render: (p) => {
        const low = p.currentStock <= p.minStock;
        const out = p.currentStock <= 0;
        return (
          <div className="flex items-center gap-2">
            <span
              className={cn(
                'min-w-[2.5rem] text-sm font-bold',
                out ? 'text-red-600' : low ? 'text-amber-600' : 'text-slate-900 dark:text-white'
              )}
            >
              {p.currentStock}
            </span>
            {out || low ? <AlertTriangle className="h-4 w-4 text-red-500" /> : null}
            <span className="text-xs text-slate-400 dark:text-slate-500">min. {p.minStock}</span>
          </div>
        );
      },
    },
    {
      key: 'purchase',
      header: 'P. compra',
      hideOnMobile: true,
      render: (p) => <span className="text-sm text-slate-600 dark:text-slate-300">{formatMoney(p.purchasePrice)}</span>,
    },
    {
      key: 'sale',
      header: 'P. venta',
      render: (p) => <span className="text-sm font-medium text-slate-900 dark:text-white">{formatMoney(p.salePrice)}</span>,
    },
    {
      key: 'value',
      header: 'Valor inv.',
      hideOnMobile: true,
      render: (p) => <span className="text-sm text-emerald-600 dark:text-emerald-400">{formatMoney(p.salePrice * p.currentStock)}</span>,
    },
    {
      key: 'state',
      header: 'Estado',
      hideOnMobile: true,
      render: (p) => (
        <span
          className={cn(
            'rounded-md px-2 py-0.5 text-[10px] font-semibold',
            p.active
              ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400'
              : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400'
          )}
        >
          {p.active ? 'Activo' : 'Inactivo'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      className: 'text-right',
      render: (p) => (
        <div className="flex justify-end gap-1.5">
          {canMove && (
            <button
              onClick={() => openMovement(p, 'IN')}
              title="Ingreso de stock"
              className="rounded-lg bg-emerald-50 p-1.5 text-emerald-600 transition hover:bg-emerald-100 dark:bg-emerald-500/20 dark:text-emerald-400"
            >
              <ArrowDownToLine size={14} />
            </button>
          )}
          {canMove && (
            <button
              onClick={() => openMovement(p, 'OUT')}
              title="Salida de stock"
              className="rounded-lg bg-amber-50 p-1.5 text-amber-600 transition hover:bg-amber-100 dark:bg-amber-500/20 dark:text-amber-400"
            >
              <ArrowUpToLine size={14} />
            </button>
          )}
          {canSeeHistory && (
            <button
              onClick={() => openHistory(p)}
              title="Historial de movimientos"
              className="rounded-lg bg-slate-100 p-1.5 text-slate-500 transition hover:bg-slate-200 hover:text-blue-600 dark:bg-slate-800 dark:text-slate-300"
            >
              <History size={14} />
            </button>
          )}
          {canEdit && (
            <button
              onClick={() => openEdit(p)}
              title="Editar producto"
              className="rounded-lg bg-slate-100 p-1.5 text-slate-500 transition hover:bg-slate-200 hover:text-blue-600 dark:bg-slate-800 dark:text-slate-300"
            >
              <Pencil size={14} />
            </button>
          )}
          {canDelete && (
            <button
              onClick={() => removeProduct(p)}
              title="Desactivar producto"
              className="rounded-lg bg-slate-100 p-1.5 text-slate-500 transition hover:bg-red-100 hover:text-red-600 dark:bg-slate-800 dark:text-slate-300"
            >
              <Trash2 size={14} />
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Inventario y Repuestos</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Control de stock de piezas, lubricantes y materiales.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canMove && (
            <label className="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-600 shadow-sm transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800/40">
              <input
                type="checkbox"
                checked={showInactive}
                onChange={(e) => setShowInactive(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 dark:border-slate-600"
              />
              Ver inactivos
            </label>
          )}
          {canCreate && (
            <button
              onClick={openCreate}
              className="inline-flex items-center justify-center rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-blue-500"
            >
              <Plus className="mr-2 h-4 w-4" /> Nuevo Producto
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
          <div>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400">Total Productos</p>
            <p className="text-2xl font-bold text-slate-900 dark:text-white">{stats.total}</p>
          </div>
          <div className="rounded-lg bg-blue-50 p-3 text-blue-600 dark:bg-blue-500/20 dark:text-blue-400">
            <Package size={24} />
          </div>
        </div>
        <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
          <div>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400">Bajo Stock</p>
            <p className="text-2xl font-bold text-amber-600">{stats.lowStock}</p>
          </div>
          <div className="rounded-lg bg-amber-50 p-3 text-amber-600 dark:bg-amber-500/20 dark:text-amber-400">
            <AlertTriangle size={24} />
          </div>
        </div>
        <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
          <div>
            <p className="text-sm font-medium text-slate-500 dark:text-slate-400">Valor Inventario</p>
            <p className="text-2xl font-bold text-emerald-600">{formatMoney(stats.value)}</p>
          </div>
          <div className="rounded-lg bg-emerald-50 p-3 text-emerald-600 dark:bg-emerald-500/20 dark:text-emerald-400">
            <ArrowUpToLine size={24} />
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
        <div className="flex flex-col gap-4 border-b border-slate-200 p-4 sm:flex-row sm:items-center dark:border-slate-700">
          <div className="relative w-full max-w-md">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
              <Search className="h-4 w-4 text-slate-400 dark:text-slate-500" />
            </div>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="block w-full rounded-lg border border-slate-300 py-2 pl-10 text-slate-900 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 dark:border-slate-600 dark:bg-slate-800 dark:text-white dark:placeholder:text-slate-400 sm:text-sm"
              placeholder="Buscar por código, nombre, marca o categoría..."
            />
          </div>
          <span className="text-sm text-slate-500 sm:ml-auto dark:text-slate-400">{filtered.length} productos</span>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-16 text-sm text-slate-400 dark:text-slate-500">
            <Spinner className="mr-2 h-5 w-5" /> Cargando inventario...
          </div>
        ) : (
          <ResponsiveTable
            columns={columns}
            rows={filtered}
            keyField={(p) => p.id}
            emptyMessage="No se encontraron productos."
            pageSize={10}
            itemLabel="productos"
          />
        )}
      </div>

      {/* Modal crear/editar producto */}
      {formOpen && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 p-4 backdrop-blur-sm sm:items-center"
          onClick={requestCloseForm}
        >
          <div
            className="max-h-[90vh] w-full max-w-lg animate-slide-up overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl dark:bg-slate-900"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-start justify-between">
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                {formOpen === 'edit' ? 'Editar Producto' : 'Nuevo Producto'}
              </h2>
              <button
                onClick={requestCloseForm}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:text-slate-500 dark:hover:bg-slate-800"
                aria-label="Cerrar"
              >
                <X size={18} />
              </button>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300 sm:col-span-2">
                Código (SKU)
                <input
                  type="text"
                  value={form.code}
                  onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
                  disabled={formOpen === 'edit' && !canEdit}
                  placeholder="Ej: ACE-5W30"
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              <label className="block text-xs font-medium text-slate-600 sm:col-span-2 dark:text-slate-300">
                Nombre
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="Ej: Aceite Sintético 5W-30"
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300">
                Marca
                <input
                  type="text"
                  value={form.brand}
                  onChange={(e) => setForm((f) => ({ ...f, brand: e.target.value }))}
                  placeholder="Ej: Castrol"
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300">
                Categoría
                <input
                  type="text"
                  value={form.category}
                  onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
                  placeholder="Ej: Aceites"
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              <label className="block text-xs font-medium text-slate-600 sm:col-span-2 dark:text-slate-300">
                Descripción
                <input
                  type="text"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300">
                Precio compra (S/)
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={form.purchasePrice}
                  onChange={(e) => setForm((f) => ({ ...f, purchasePrice: e.target.value }))}
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300">
                Precio venta (S/)
                <input
                  type="number"
                  min={0.01}
                  step="0.01"
                  value={form.salePrice}
                  onChange={(e) => setForm((f) => ({ ...f, salePrice: e.target.value }))}
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              {formOpen === 'create' && (
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-300">
                  Stock inicial
                  <input
                    type="number"
                    min={0}
                    value={form.currentStock}
                    onChange={(e) => setForm((f) => ({ ...f, currentStock: e.target.value }))}
                    className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                  />
                </label>
              )}
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-300">
                Stock mínimo
                <input
                  type="number"
                  min={0}
                  value={form.minStock}
                  onChange={(e) => setForm((f) => ({ ...f, minStock: e.target.value }))}
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
              {formOpen === 'edit' && (
                <label className="flex items-center gap-2 text-xs font-medium text-slate-600 sm:col-span-2 dark:text-slate-300">
                  <input
                    type="checkbox"
                    checked={form.active}
                    onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
                    className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 dark:border-slate-600"
                  />
                  Producto activo
                </label>
              )}
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button
                onClick={requestCloseForm}
                className="rounded-lg border border-slate-200 px-3.5 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
              >
                Cancelar
              </button>
              <button
                onClick={saveProduct}
                disabled={saving}
                className="flex items-center gap-1.5 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-semibold text-white transition hover:bg-blue-500 disabled:opacity-50"
              >
                {saving ? <Spinner /> : <Save size={14} />}
                {formOpen === 'edit' ? 'Guardar cambios' : 'Registrar producto'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal movimiento de stock */}
      {stockProduct && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 p-4 backdrop-blur-sm sm:items-center"
          onClick={requestCloseMovement}
        >
          <div
            className="w-full max-w-md animate-slide-up rounded-2xl bg-white p-6 shadow-2xl dark:bg-slate-900"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-start justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                  {stockType === 'IN' ? 'Ingreso de stock' : 'Salida de stock'}
                </h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  {stockProduct.code} · {stockProduct.name} · Actual: {stockProduct.currentStock}
                </p>
              </div>
              <button
                onClick={requestCloseMovement}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:text-slate-500 dark:hover:bg-slate-800"
                aria-label="Cerrar"
              >
                <X size={18} />
              </button>
            </div>
            <div className="flex items-center gap-2">
              <label className="block flex-1 text-xs font-medium text-slate-600 dark:text-slate-300">
                Tipo
                <select
                  value={stockType}
                  onChange={(e) => setStockType(e.target.value as 'IN' | 'OUT')}
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                >
                  <option value="IN">Entrada</option>
                  <option value="OUT">Salida</option>
                </select>
              </label>
              <label className="block w-32 text-xs font-medium text-slate-600 dark:text-slate-300">
                Cantidad
                <input
                  type="number"
                  min={1}
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
                />
              </label>
            </div>
            <label className="mt-3 block text-xs font-medium text-slate-600 dark:text-slate-300">
              Referencia (opcional)
              <input
                type="text"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                placeholder="Ej: Compra a proveedor, OT-000001..."
                className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-2 text-sm text-slate-900 focus:border-blue-500 focus:outline-none dark:border-slate-600 dark:bg-slate-800 dark:text-white"
              />
            </label>
            <div className="mt-5 flex justify-end gap-2">
              <button
                onClick={requestCloseMovement}
                className="rounded-lg border border-slate-200 px-3.5 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-50 dark:border-slate-600 dark:text-slate-300 dark:hover:bg-slate-800"
              >
                Cancelar
              </button>
              <button
                onClick={saveMovement}
                disabled={savingStock}
                className={cn(
                  'flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-sm font-semibold text-white transition disabled:opacity-50',
                  stockType === 'IN' ? 'bg-emerald-600 hover:bg-emerald-500' : 'bg-amber-600 hover:bg-amber-500'
                )}
              >
                {savingStock ? <Spinner /> : stockType === 'IN' ? <ArrowDownToLine size={14} /> : <ArrowUpToLine size={14} />}
                Registrar {stockType === 'IN' ? 'entrada' : 'salida'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal historial */}
      {historyProduct && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 p-4 backdrop-blur-sm sm:items-center"
          onClick={() => setHistoryProduct(null)}
        >
          <div
            className="max-h-[85vh] w-full max-w-lg animate-slide-up overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl dark:bg-slate-900"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-start justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">Movimientos</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  {historyProduct.code} · {historyProduct.name}
                </p>
              </div>
              <button
                onClick={() => setHistoryProduct(null)}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:text-slate-500 dark:hover:bg-slate-800"
                aria-label="Cerrar"
              >
                <X size={18} />
              </button>
            </div>
            {loadingHistory ? (
              <div className="flex items-center justify-center py-10 text-sm text-slate-400 dark:text-slate-500">
                <Spinner className="mr-2 h-5 w-5" /> Cargando historial...
              </div>
            ) : transactions.length === 0 ? (
              <p className="rounded-xl border border-dashed border-slate-200 p-4 text-center text-sm text-slate-400 dark:border-slate-700 dark:text-slate-500">
                Sin movimientos registrados para este producto.
              </p>
            ) : (
              <ul className="space-y-2">
                {transactions.map((t) => (
                  <li
                    key={t.id}
                    className="flex items-center justify-between rounded-xl border border-slate-200 p-3 text-sm dark:border-slate-700"
                  >
                    <div>
                      <span
                        className={cn(
                          'mr-2 inline-block rounded-md px-2 py-0.5 text-[10px] font-bold',
                          t.type === 'IN'
                            ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-400'
                            : 'bg-amber-50 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400'
                        )}
                      >
                        {t.type === 'IN' ? 'ENTRADA' : 'SALIDA'}
                      </span>
                      <span className="font-semibold text-slate-900 dark:text-white">x{t.quantity}</span>
                      <span className="ml-2 text-xs text-slate-500 dark:text-slate-400">@ {formatMoney(t.unitPrice)}</span>
                      {t.reference && <span className="ml-1 text-xs text-slate-400 dark:text-slate-500">· {t.reference}</span>}
                    </div>
                    <span className="shrink-0 text-xs text-slate-400 dark:text-slate-500">
                      {t.transactionDate ? new Date(t.transactionDate.replace('T', ' ')).toLocaleString('es-PE') : ''}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {/* Confirmación para no perder datos sin guardar */}
      <ConfirmDialog
        open={discard !== null}
        danger
        title={discard?.title ?? ''}
        message={discard?.message}
        confirmLabel={discard?.confirmLabel ?? 'Descartar'}
        cancelLabel="Seguir editando"
        onConfirm={() => {
          discard?.action();
          setDiscard(null);
        }}
        onCancel={() => setDiscard(null)}
      />
    </div>
  );
}