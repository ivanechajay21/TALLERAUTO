import { useEffect, useMemo, useState } from 'react';
import { Search, Activity, UserRound, Clock, FileText } from 'lucide-react';
import ResponsiveTable from '../components/ui/ResponsiveTable';
import type { ResponsiveColumn } from '../components/ui/ResponsiveTable';
import { cn } from '../lib/utils';
import { getAuditLogs, getAuditActions } from '../services/auditService';
import type { AuditLog, AuditActionOption } from '../services/auditService';

const ACTION_STYLES: Record<string, string> = {
  CREATE: 'bg-emerald-100 text-emerald-700 border-emerald-200',
  UPDATE: 'bg-blue-100 text-blue-700 border-blue-200',
  DELETE: 'bg-red-100 text-red-700 border-red-200',
  STATUS: 'bg-amber-100 text-amber-700 border-amber-200',
  PAYMENT: 'bg-violet-100 text-violet-700 border-violet-200',
  PERMISSIONS: 'bg-fuchsia-100 text-fuchsia-700 border-fuchsia-200',
  LOGIN: 'bg-teal-100 text-teal-700 border-teal-200',
  LOGOUT: 'bg-cyan-100 text-cyan-700 border-cyan-200',
};

const ACTION_ICON_BG: Record<string, string> = {
  CREATE: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400',
  UPDATE: 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  DELETE: 'bg-red-50 text-red-600 dark:bg-red-500/15 dark:text-red-400',
  STATUS: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-400',
  PAYMENT: 'bg-violet-50 text-violet-600 dark:bg-violet-500/15 dark:text-violet-400',
  PERMISSIONS: 'bg-fuchsia-50 text-fuchsia-600 dark:bg-fuchsia-500/15 dark:text-fuchsia-400',
  LOGIN: 'bg-teal-50 text-teal-600 dark:bg-teal-500/15 dark:text-teal-400',
  LOGOUT: 'bg-cyan-50 text-cyan-600 dark:bg-cyan-500/15 dark:text-cyan-400',
};

const ENTITY_LABELS: Record<string, string> = {
  USUARIO: 'Usuario',
  ROL: 'Rol',
  CLIENTE: 'Cliente',
  VEHICULO: 'Vehículo',
  CITA: 'Cita',
  RECEPCION: 'Recepción',
  ORDEN_TRABAJO: 'Orden de trabajo',
  ITEM_ORDEN: 'Item de orden',
  PRODUCTO: 'Producto',
  FACTURA: 'Factura',
  CONFIGURACION: 'Configuración',
};

const entityLabel = (entity?: string) => {
  if (!entity) return '—';
  if (ENTITY_LABELS[entity]) return ENTITY_LABELS[entity];
  if (entity.startsWith('CATALOGO_')) return `Catálogo (${entity.replace('CATALOGO_', '')})`;
  return entity;
};

function formatTimestamp(iso?: string): string {
  if (!iso) return '—';
  const date = new Date(iso);
  if (isNaN(date.getTime())) return iso;
  return `${date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' })} · ${date.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}`;
}

const DEFAULT_ACTIONS: AuditActionOption[] = [
  { action: 'LOGIN', label: 'Inicio de sesión' },
  { action: 'LOGOUT', label: 'Cierre de sesión' },
  { action: 'CREATE', label: 'Creación' },
  { action: 'UPDATE', label: 'Actualización' },
  { action: 'DELETE', label: 'Eliminación' },
  { action: 'STATUS', label: 'Cambio de estado' },
  { action: 'MOVEMENT', label: 'Movimiento de stock' },
  { action: 'PAYMENT', label: 'Pago' },
  { action: 'PERMISSIONS', label: 'Permisos' },
];

export default function Audit() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [actions, setActions] = useState<AuditActionOption[]>(DEFAULT_ACTIONS);
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAuditActions()
      .then((opts) => {
        const loaded = opts.map((o) => ({ action: o.action, label: o.label || o.action }));
        if (loaded.length > 0) setActions(loaded);
      })
      .catch(() => {
        setActions(DEFAULT_ACTIONS);
      });
  }, []);

  useEffect(() => {
    let cancelled = false;
    const timer = setTimeout(() => {
      setLoading(true);
      setError(null);
      getAuditLogs(actionFilter === 'ALL' ? undefined : actionFilter, search.trim() || undefined)
        .then((data) => {
          if (!cancelled) setLogs(data);
        })
        .catch(() => {
          if (!cancelled) setError('No se pudieron cargar los registros de auditoría.');
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, 300);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [search, actionFilter]);

  const filtered = useMemo(() => logs, [logs]);

  const columns: ResponsiveColumn<AuditLog>[] = [
    {
      key: 'timestamp',
      header: 'Fecha / Hora',
      render: (log) => (
        <div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
          <Clock size={14} className="text-slate-400 dark:text-slate-500" />
          {formatTimestamp(log.timestamp)}
        </div>
      ),
    },
    {
      key: 'action',
      header: 'Acción',
      render: (log) => (
        <span className={cn('rounded-md border px-2.5 py-1 text-[10px] font-bold', ACTION_STYLES[log.action] || 'border-slate-200 bg-slate-100 text-slate-700')}>
          {log.actionLabel || log.action}
        </span>
      ),
    },
    {
      key: 'entity',
      header: 'Entidad / Registro',
      render: (log) => (
        <div className="flex items-center gap-2 text-sm font-medium text-slate-900 dark:text-white">
          <Activity size={16} className="text-blue-500" />
          {entityLabel(log.entity)} <span className="font-normal text-slate-400 dark:text-slate-500">(#{log.entityId ?? '—'})</span>
        </div>
      ),
    },
    {
      key: 'username',
      header: 'Usuario',
      render: (log) => (
        <div className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
          <span className={cn('flex h-6 w-6 items-center justify-center rounded-full', ACTION_ICON_BG[log.action] || 'bg-slate-200 text-slate-600')}>
            <UserRound size={12} />
          </span>
          @{log.username}
        </div>
      ),
    },
    {
      key: 'details',
      header: 'Detalles',
      render: (log) => (
        <div className="flex items-start gap-2 text-sm text-slate-500 dark:text-slate-400">
          <FileText size={14} className="mt-0.5 shrink-0 text-slate-400 dark:text-slate-500" />
          {log.details || '—'}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Registro de Auditoría</h1>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Historial de acciones y cambios en el sistema por usuario.</p>
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
        <div className="flex flex-col gap-4 border-b border-slate-200 bg-slate-50/60 p-4 sm:flex-row sm:items-center dark:border-slate-700 dark:bg-slate-800/50">
          <div className="relative w-full max-w-md">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
              <Search className="h-4 w-4 text-slate-400 dark:text-slate-500" />
            </div>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="block w-full rounded-lg border border-slate-300 bg-white py-2 pl-10 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-600 focus:ring-2 focus:ring-blue-600/20 dark:border-slate-600 dark:bg-slate-900 dark:text-white dark:placeholder:text-slate-500"
              placeholder="Buscar por usuario, entidad o detalle..."
            />
          </div>
          <select
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            className="w-full rounded-lg border border-slate-300 bg-white py-2 text-sm text-slate-700 focus:border-blue-600 sm:w-auto dark:border-slate-600 dark:bg-slate-900 dark:text-slate-200"
          >
            <option value="ALL">Cualquier Acción</option>
            {actions.map((option) => (
              <option key={option.action} value={option.action}>
                {option.label}
              </option>
            ))}
          </select>
          {loading && (
            <span className="flex items-center gap-2 text-xs font-medium text-slate-400 dark:text-slate-500">
              <span className="h-3 w-3 animate-spin rounded-full border-2 border-slate-300 border-t-blue-600 dark:border-slate-600" />
              Cargando...
            </span>
          )}
        </div>

        {error ? (
          <div className="p-8 text-center text-sm text-red-600">{error}</div>
        ) : (
          <ResponsiveTable
            columns={columns}
            rows={filtered}
            keyField={(log) => log.id}
            emptyMessage={loading ? 'Cargando registros...' : 'No se encontraron registros de auditoría.'}
            pageSize={10}
            itemLabel="registros"
          />
        )}
      </div>
    </div>
  );
}