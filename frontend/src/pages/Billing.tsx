import { useEffect, useMemo, useState } from 'react';
import { FileText, Search, CreditCard, Printer, Download, CheckCircle } from 'lucide-react';
import { getAllInvoices, payInvoice } from '../services/invoiceService';
import type { Invoice } from '../services/invoiceService';
import { cn } from '../lib/utils';
import { useToast } from '../components/ui/Toast';
import { usePermissions } from '../context/PermissionContext';
import usePagination from '../hooks/usePagination';
import Pagination from '../components/ui/Pagination';

const fmt = (n: number) =>
  (n || 0).toLocaleString('es-ES', { style: 'currency', currency: 'PEN' });

const PAYMENT_METHODS = [
  { value: 'CASH', label: 'Efectivo' },
  { value: 'CARD', label: 'Tarjeta' },
  { value: 'TRANSFER', label: 'Transferencia' },
];

export default function Billing() {
  const toast = useToast();
  const { hasPermission } = usePermissions();
  const canApprove = hasPermission('FACTURAS:APROBAR');
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [paying, setPaying] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('CASH');

  const fetchInvoices = async () => {
    setLoading(true);
    try {
      const data = await getAllInvoices();
      setInvoices(data);
      setSelectedId((cur) => cur ?? data[0]?.id ?? null);
    } catch {
      toast.error('Error', 'No se pudieron cargar las facturas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim();
    if (!q) return invoices;
    return invoices.filter(
      (inv) =>
        inv.invoiceNumber.toLowerCase().includes(q) ||
        (inv.workOrder?.orderNumber || '').toLowerCase().includes(q) ||
        (inv.customer || '').toLowerCase().includes(q)
    );
  }, [invoices, search]);

  const pag = usePagination(filtered, 10);

  const selected = invoices.find((inv) => inv.id === selectedId) || null;

  const handlePay = async () => {
    if (!selected) return;
    setPaying(true);
    try {
      const updated = await payInvoice(selected.id, paymentMethod);
      setInvoices((prev) => prev.map((inv) => (inv.id === selected.id ? updated : inv)));
      toast.success('Pago registrado', `${updated.invoiceNumber} quedó pagada.`);
    } catch (err: any) {
      toast.error('Error', err?.response?.data?.message || 'No se pudo registrar el pago.');
    } finally {
      setPaying(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Facturación y Cobros</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Gestión de facturas y boletas de pago por servicios.</p>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
        <div className="space-y-4 md:col-span-1">
          <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-900">
            <div className="relative mb-4">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                <Search className="h-4 w-4 text-slate-400 dark:text-slate-500" />
              </div>
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="block w-full rounded-lg border border-slate-300 py-2 pl-10 text-sm focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 dark:border-slate-600"
                placeholder="Buscar comprobante..."
              />
            </div>

            {loading ? (
              <div className="space-y-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-20 animate-pulse rounded-lg bg-slate-100 dark:bg-slate-800" />
                ))}
              </div>
            ) : (
              <div className="space-y-2">
                {pag.pageItems.map((inv) => (
                  <button
                    key={inv.id}
                    onClick={() => setSelectedId(inv.id)}
                    className={cn(
                      'w-full rounded-lg border p-3 text-left transition',
                      selectedId === inv.id
                        ? 'border-blue-500 bg-blue-50/60 ring-2 ring-blue-500/20'
                        : 'border-slate-200 hover:border-blue-300 hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800/40'
                    )}
                  >
                    <div className="mb-1 flex items-start justify-between gap-2">
                      <span className="text-sm font-bold text-slate-900 dark:text-white">{inv.invoiceNumber}</span>
                      <span
                        className={cn(
                          'rounded px-2 py-0.5 text-[10px] font-bold',
                          inv.status === 'PAID' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
                        )}
                      >
                        {inv.status === 'PAID' ? 'PAGADA' : 'PENDIENTE'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400">{inv.customer || '—'}</p>
                    <div className="mt-2 flex items-center justify-between">
                      <span className="text-xs text-slate-400 dark:text-slate-500">
                        {inv.issueDate ? new Date(inv.issueDate).toLocaleDateString('es-ES') : ''}
                      </span>
                      <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">{fmt(inv.totalAmount)}</span>
                    </div>
                  </button>
                ))}
                {filtered.length === 0 && (
                  <p className="py-6 text-center text-sm text-slate-400 dark:text-slate-500">Sin facturas.</p>
                )}
              </div>
            )}

            <Pagination
              page={pag.page}
              pageCount={pag.pageCount}
              onChange={pag.setPage}
              total={pag.total}
              from={pag.from}
              to={pag.to}
              itemLabel="registros"
            />
          </div>
        </div>

        <div className="md:col-span-2">
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-800/50">
              <div className="flex items-center gap-2">
                <FileText className="text-slate-400 dark:text-slate-500" />
                <h3 className="font-semibold text-slate-800 dark:text-slate-100">Detalle del comprobante</h3>
              </div>
              <div className="flex gap-2">
                <button className="rounded-lg p-2 text-slate-600 transition hover:bg-slate-200 dark:text-slate-300 dark:hover:bg-slate-700" title="Imprimir">
                  <Printer size={18} />
                </button>
                <button className="rounded-lg p-2 text-slate-600 transition hover:bg-slate-200 dark:text-slate-300 dark:hover:bg-slate-700" title="Descargar">
                  <Download size={18} />
                </button>
              </div>
            </div>

            {!selected ? (
              <div className="p-12 text-center text-sm text-slate-400 dark:text-slate-500">
                {loading ? 'Cargando...' : 'Genera una factura desde una orden finalizada.'}
              </div>
            ) : (
              <div className="p-6 sm:p-8">
                <div className="mb-6 flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 pb-6 dark:border-slate-700">
                  <div>
                    <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">FACTURA</h2>
                    <p className="mt-1 font-mono text-slate-500 dark:text-slate-400">{selected.invoiceNumber}</p>
                  </div>
                  <div className="text-right">
                    <div className="mb-2 inline-flex items-center gap-2">
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-500/10 text-blue-600">
                        <CheckCircle size={16} />
                      </div>
                      <span className="font-bold text-slate-700 dark:text-slate-200">Taller Mecánico SIGTM</span>
                    </div>
                    <p className="text-sm text-slate-500 dark:text-slate-400">RUC: 20123456789</p>
                    <p className="text-sm text-slate-500 dark:text-slate-400">Av. Principal 123, Ciudad</p>
                  </div>
                </div>

                <div className="mb-8 grid grid-cols-2 gap-8">
                  <div>
                    <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">Facturar a</h4>
                    <p className="font-semibold text-slate-800 dark:text-slate-100">{selected.customer || '—'}</p>
                    <p className="text-sm text-slate-600 dark:text-slate-300">DNI: {selected.documentNumber || '—'}</p>
                  </div>
                  <div>
                    <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">Referencia</h4>
                    <p className="text-sm text-slate-600 dark:text-slate-300">
                      <span className="font-semibold text-slate-700 dark:text-slate-200">Orden:</span> {selected.workOrder?.orderNumber || '—'}
                    </p>
                    <p className="text-sm text-slate-600 dark:text-slate-300">
                      <span className="font-semibold text-slate-700 dark:text-slate-200">Vehículo:</span> {selected.vehicle || '—'}
                    </p>
                    <p className="text-sm text-slate-600 dark:text-slate-300">
                      <span className="font-semibold text-slate-700 dark:text-slate-200">Emisión:</span>{' '}
                      {selected.issueDate ? new Date(selected.issueDate).toLocaleDateString('es-ES') : '—'}
                    </p>
                  </div>
                </div>

                <div className="flex justify-end">
                  <div className="w-64 space-y-3">
                    <div className="flex justify-between text-sm text-slate-600 dark:text-slate-300">
                      <span>Subtotal:</span>
                      <span>{fmt(selected.subtotal)}</span>
                    </div>
                    <div className="flex justify-between border-b border-slate-200 pb-3 text-sm text-slate-600 dark:border-slate-700 dark:text-slate-300">
                      <span>IGV (18%):</span>
                      <span>{fmt(selected.taxAmount)}</span>
                    </div>
                    <div className="flex justify-between pt-1 text-lg font-bold text-slate-900 dark:text-white">
                      <span>Total:</span>
                      <span>{fmt(selected.totalAmount)}</span>
                    </div>
                    <div className="flex justify-between text-xs text-slate-500 dark:text-slate-400">
                      <span>Estado:</span>
                      <span className={cn('font-bold', selected.status === 'PAID' ? 'text-emerald-600' : 'text-amber-600')}>
                        {selected.status === 'PAID' ? 'PAGADA' : 'PENDIENTE'}
                      </span>
                    </div>
                  </div>
                </div>

                {selected.status === 'PENDING' && canApprove && (
                  <div className="mt-8 flex flex-wrap items-center justify-end gap-3 border-t border-slate-200 pt-6 dark:border-slate-700">
                    <select
                      value={paymentMethod}
                      onChange={(e) => setPaymentMethod(e.target.value)}
                      className="rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-700 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-200"
                    >
                      {PAYMENT_METHODS.map((m) => (
                        <option key={m.value} value={m.value}>
                          {m.label}
                        </option>
                      ))}
                    </select>
                    <button
                      onClick={handlePay}
                      disabled={paying}
                      className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-6 py-2.5 font-medium text-white transition hover:bg-emerald-500 disabled:opacity-50"
                    >
                      <CreditCard size={18} />
                      {paying ? 'Registrando...' : 'Registrar Pago'}
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
