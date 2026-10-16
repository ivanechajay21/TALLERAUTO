import { useEffect, useMemo, useState } from 'react';
import {
  Users,
  CarFront,
  CalendarCheck,
  Clock4,
  Wrench,
  PackageCheck,
  CheckCircle2,
  Banknote,
  Boxes,
  AlertTriangle,
  PackageX,
  TrendingUp,
  Loader2,
} from 'lucide-react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { getDashboardSummary, getDashboardLine, getDashboardDonut, getDashboardBars } from '../services/reportService';
import type {
  DashboardPeriod,
  DashboardSummary,
  DashboardLineRow,
  DashboardBars,
} from '../services/reportService';
import { cn } from '../lib/utils';
import { useTheme } from '../context/ThemeContext';

const DEFAULT_SUMMARY: DashboardSummary = {
  clientesRegistrados: 0,
  vehiculosRegistrados: 0,
  citasDelDia: 0,
  ordenesPendientes: 0,
  ordenesEnProceso: 0,
  vehiculosListos: 0,
  reparacionesFinalizadas: 0,
  ingresos: 0,
  valorInventario: 0,
  stockBajo: 0,
  productosAgotados: 0,
};

const DEFAULT_BARS: DashboardBars = {
  servicios: [],
  repuestos: [],
  reparacionesPorPeriodo: [],
  trabajosPorMecanico: [],
};

const PERIODS: { key: DashboardPeriod; label: string }[] = [
  { key: 'HOY', label: 'Hoy' },
  { key: '7', label: '7 días' },
  { key: '30', label: '30 días' },
  { key: 'YEAR', label: 'Este año' },
];

const DONUT_COLORS = ['#f59e0b', '#3b82f6', '#f97316', '#06b6d4', '#10b981'];

const currency = (value: number) =>
  value.toLocaleString('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 });

interface ChartTooltipProps {
  active?: boolean;
  payload?: Array<{
    name?: string | number;
    value?: number | string;
    color?: string;
    fill?: string;
    dataKey?: string | number;
  }>;
  label?: string | number;
  format?: 'currency' | 'number';
}

function ChartTooltip({ active, payload, label, format }: ChartTooltipProps) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs shadow-xl dark:border-slate-700 dark:bg-slate-900">
      <p className="mb-1 font-semibold text-slate-700 dark:text-slate-200">{label}</p>
      {payload.map((entry) => (
        <p key={String(entry.dataKey)} className="flex items-center gap-2 text-slate-500 dark:text-slate-400">
          <span className="h-2 w-2 rounded-full" style={{ background: entry.color || entry.fill || '#3b82f6' }} />
          <span className="capitalize">{entry.name}:</span>
          <span className="font-semibold text-slate-900 dark:text-white">
            {format === 'currency' ? currency(Number(entry.value)) : String(entry.value)}
          </span>
        </p>
      ))}
    </div>
  );
}

const axisTick = { fill: '#94a3b8', fontSize: 11 };

export default function Dashboard() {
  const [periodo, setPeriodo] = useState<DashboardPeriod>('30');
  const [summary, setSummary] = useState<DashboardSummary>(DEFAULT_SUMMARY);
  const [lineData, setLineData] = useState<DashboardLineRow[]>([]);
  const [donutData, setDonutData] = useState<{ name: string; value: number }[]>([]);
  const [bars, setBars] = useState<DashboardBars>(DEFAULT_BARS);
  const [loading, setLoading] = useState(true);
  const { theme } = useTheme();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      getDashboardSummary(periodo),
      getDashboardLine(periodo),
      getDashboardDonut(),
      getDashboardBars(periodo),
    ])
      .then(([s, line, donut, b]) => {
        if (cancelled) return;
        setSummary(s);
        setLineData(line);
        setDonutData(donut);
        setBars(b);
      })
      .catch(() => {
        /* sin permisos o sin conexión */
        if (!cancelled) setDonutData([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [periodo]);

  const gridColor = theme === 'dark' ? '#1e293b' : '#e2e8f0';

  const kpis = useMemo(
    () => [
      { key: 'clientesRegistrados', label: 'Clientes registrados', value: String(summary.clientesRegistrados), icon: Users, color: 'text-emerald-500', bg: 'bg-emerald-50 dark:bg-emerald-500/10' },
      { key: 'vehiculosRegistrados', label: 'Vehículos registrados', value: String(summary.vehiculosRegistrados), icon: CarFront, color: 'text-sky-500', bg: 'bg-sky-50 dark:bg-sky-500/10' },
      { key: 'citasDelDia', label: 'Citas del día', value: String(summary.citasDelDia), icon: CalendarCheck, color: 'text-indigo-500', bg: 'bg-indigo-50 dark:bg-indigo-500/10' },
      { key: 'ordenesPendientes', label: 'Órdenes pendientes', value: String(summary.ordenesPendientes), icon: Clock4, color: 'text-amber-500', bg: 'bg-amber-50 dark:bg-amber-500/10' },
      { key: 'ordenesEnProceso', label: 'Órdenes en proceso', value: String(summary.ordenesEnProceso), icon: Wrench, color: 'text-orange-500', bg: 'bg-orange-50 dark:bg-orange-500/10' },
      { key: 'vehiculosListos', label: 'Vehículos listos', value: String(summary.vehiculosListos), icon: PackageCheck, color: 'text-cyan-600', bg: 'bg-cyan-50 dark:bg-cyan-500/10' },
      { key: 'reparacionesFinalizadas', label: 'Reparaciones finalizadas', value: String(summary.reparacionesFinalizadas), icon: CheckCircle2, color: 'text-green-500', bg: 'bg-green-50 dark:bg-green-500/10' },
      { key: 'ingresos', label: 'Ingresos (período)', value: currency(summary.ingresos), icon: Banknote, color: 'text-blue-500', bg: 'bg-blue-50 dark:bg-blue-500/10' },
      { key: 'valorInventario', label: 'Valor de inventario', value: currency(summary.valorInventario), icon: Boxes, color: 'text-violet-500', bg: 'bg-violet-50 dark:bg-violet-500/10' },
      { key: 'stockBajo', label: 'Stock bajo', value: String(summary.stockBajo), icon: AlertTriangle, color: 'text-amber-600', bg: 'bg-amber-50 dark:bg-amber-500/10' },
      { key: 'productosAgotados', label: 'Productos agotados', value: String(summary.productosAgotados), icon: PackageX, color: 'text-red-500', bg: 'bg-red-50 dark:bg-red-500/10' },
    ] as const,
    [summary]
  );

  const panel = 'rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900';

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Resumen operativo</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Indicadores clave y tendencias del taller.</p>
        </div>
        <div className="flex items-center gap-1 rounded-xl border border-slate-200 bg-white p-1 shadow-sm dark:border-slate-700 dark:bg-slate-900">
          {PERIODS.map((p) => (
            <button
              key={p.key}
              onClick={() => setPeriodo(p.key)}
              className={cn(
                'rounded-lg px-3 py-1.5 text-xs font-semibold transition-all',
                periodo === p.key
                  ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow'
                  : 'text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-200'
              )}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-5 2xl:grid-cols-6">
        {kpis.map((kpi) => (
          <div
            key={kpi.key}
            className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900"
          >
            <div className={cn('mb-3 flex h-10 w-10 items-center justify-center rounded-xl', kpi.bg)}>
              <kpi.icon className={cn('h-5 w-5', kpi.color)} aria-hidden="true" />
            </div>
            <p className="truncate text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
              {kpi.value}
            </p>
            <p className="mt-0.5 truncate text-xs font-medium text-slate-500 dark:text-slate-400">{kpi.label}</p>
          </div>
        ))}
      </div>

      {loading && lineData.length === 0 ? (
        <div className={cn(panel, 'flex flex-col items-center justify-center gap-3 py-24')}>
          <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
          <p className="text-sm text-slate-500 dark:text-slate-400">Cargando indicadores...</p>
        </div>
      ) : (
        <>
          {/* Línea: tendencias del período */}
          <div className={panel}>
            <div className="mb-5 flex items-center gap-2">
              <TrendingUp size={18} className="text-blue-500" />
              <div>
                <h3 className="text-base font-semibold text-slate-900 dark:text-white">Tendencias del período</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Citas, reparaciones, vehículos atendidos e ingresos
                </p>
              </div>
            </div>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={lineData} margin={{ top: 5, right: 15, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={gridColor} />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={axisTick} />
                  <YAxis yAxisId="left" axisLine={false} tickLine={false} tick={axisTick} width={34} />
                  <YAxis yAxisId="right" orientation="right" axisLine={false} tickLine={false} tick={axisTick} width={48} tickFormatter={(v) => (v >= 1000 ? `${Math.round(v / 1000)}k` : String(v))} />
                  <Tooltip content={<ChartTooltip />} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Line yAxisId="left" type="monotone" dataKey="citas" name="Citas" stroke="#6366f1" strokeWidth={2.5} dot={false} activeDot={{ r: 5 }} />
                  <Line yAxisId="left" type="monotone" dataKey="reparaciones" name="Reparaciones" stroke="#3b82f6" strokeWidth={2.5} dot={false} activeDot={{ r: 5 }} />
                  <Line yAxisId="left" type="monotone" dataKey="vehiculos" name="Vehículos" stroke="#06b6d4" strokeWidth={2.5} dot={false} activeDot={{ r: 5 }} />
                  <Line yAxisId="right" type="monotone" dataKey="ingresos" name="Ingresos" stroke="#10b981" strokeWidth={2.5} dot={false} activeDot={{ r: 5 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Donut + servicios/repuestos */}
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
            <div className={panel}>
              <h3 className="mb-5 text-base font-semibold text-slate-900 dark:text-white">Estado de órdenes</h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={donutData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={95}
                      paddingAngle={3}
                      strokeWidth={0}
                    >
                      {donutData.map((_, i) => (
                        <Cell key={i} fill={DONUT_COLORS[i % DONUT_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<ChartTooltip />} />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className={panel}>
              <h3 className="mb-5 flex items-center gap-2 text-base font-semibold text-slate-900 dark:text-white">
                <Wrench size={16} className="text-blue-500" /> Servicios más solicitados
              </h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={bars.servicios} layout="vertical" margin={{ top: 5, right: 25, bottom: 5, left: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={gridColor} />
                    <XAxis type="number" axisLine={false} tickLine={false} tick={axisTick} />
                    <YAxis type="category" dataKey="name" axisLine={false} tickLine={false} tick={axisTick} width={130} />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(59,130,246,0.08)' }} />
                    <Bar dataKey="value" name="Cantidad" fill="#3b82f6" radius={[0, 6, 6, 0]} barSize={16} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className={panel}>
              <h3 className="mb-5 flex items-center gap-2 text-base font-semibold text-slate-900 dark:text-white">
                <Boxes size={16} className="text-violet-500" /> Repuestos más utilizados
              </h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={bars.repuestos} layout="vertical" margin={{ top: 5, right: 25, bottom: 5, left: 10 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={gridColor} />
                    <XAxis type="number" axisLine={false} tickLine={false} tick={axisTick} />
                    <YAxis type="category" dataKey="name" axisLine={false} tickLine={false} tick={axisTick} width={130} />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(139,92,246,0.08)' }} />
                    <Bar dataKey="value" name="Cantidad" fill="#8b5cf6" radius={[0, 6, 6, 0]} barSize={16} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Barras: reparaciones por período + trabajo por mecánico */}
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <div className={panel}>
              <h3 className="mb-5 flex items-center gap-2 text-base font-semibold text-slate-900 dark:text-white">
                <TrendingUp size={16} className="text-emerald-500" /> Reparaciones por período
              </h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={bars.reparacionesPorPeriodo} margin={{ top: 5, right: 15, bottom: 5, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={gridColor} />
                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={axisTick} />
                    <YAxis axisLine={false} tickLine={false} tick={axisTick} width={30} allowDecimals={false} />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(16,185,129,0.08)' }} />
                    <Bar dataKey="value" name="Reparaciones" fill="#10b981" radius={[6, 6, 0, 0]} barSize={26} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className={panel}>
              <h3 className="mb-5 flex items-center gap-2 text-base font-semibold text-slate-900 dark:text-white">
                <Wrench size={16} className="text-orange-500" /> Trabajos por mecánico
              </h3>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={bars.trabajosPorMecanico} margin={{ top: 5, right: 15, bottom: 5, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={gridColor} />
                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={axisTick} />
                    <YAxis axisLine={false} tickLine={false} tick={axisTick} width={30} allowDecimals={false} />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(249,115,22,0.08)' }} />
                    <Bar dataKey="value" name="Trabajos" fill="#f97316" radius={[6, 6, 0, 0]} barSize={26} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}