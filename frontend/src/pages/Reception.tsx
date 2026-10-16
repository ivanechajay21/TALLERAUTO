import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Car,
  ChevronLeft,
  ChevronRight,
  Check,
  Upload,
  Trash2,
  X,
  Camera,
  Fuel,
  FileText,
  Search,
  UserRound,
  ClipboardCheck,
  ClipboardList,
  Plus,
} from 'lucide-react';
import { cn } from '../lib/utils';
import ReceptionList from '../components/ReceptionList';
import { getAllCustomers } from '../services/customerService';
import { getVehiclesByCustomer } from '../services/vehicleService';
import type { Customer } from '../services/customerService';
import type { Vehicle } from '../services/vehicleService';
import { createReception, emitOrderFromReception } from '../services/receptionService';
import type { DamageRequest, Reception } from '../services/receptionService';
import VehicleInspector, { DAMAGE_TYPES, SEVERITIES, ZONE_MAP } from '../components/VehicleInspector';
import type { Side } from '../components/VehicleInspector';
import { getCurrentUser } from '../services/authService';
import { useToast } from '../components/ui/Toast';

const FUEL_LEVELS = ['Tanque lleno', '3/4', '1/2', '1/4', 'Reserva', 'Vacío'];

const ACCESSORIES = [
  'Gato',
  'Llanta de repuesto',
  'Tapa aros',
  'Cable de corriente',
  'Control remoto',
  'Documentos',
  'Triángulo',
  'Tapetes',
  'Herramientas',
  'Placa removida',
];

const PHOTO_CATEGORIES = [
  'Delantera',
  'Trasera',
  'Lateral izquierdo',
  'Lateral derecho',
  'Interior',
  'Motor',
  'Otros',
];

const STEPS = [
  { label: 'Cliente y vehículo', icon: UserRound },
  { label: 'Datos de recepción', icon: FileText },
  { label: 'Inspección visual', icon: ClipboardCheck },
  { label: 'Confirmar', icon: Check },
];

type DamageDraft = DamageRequest & { photo?: File };

const SEV_STYLE: Record<string, { dot: string; text: string }> = {
  LEVE: { dot: 'bg-emerald-500', text: 'text-emerald-600' },
  MODERADO: { dot: 'bg-amber-500', text: 'text-amber-600' },
  GRAVE: { dot: 'bg-red-500', text: 'text-red-600' },
};

export default function Reception() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const user = getCurrentUser();

  const view: 'listado' | 'nueva' = location.pathname.endsWith('/nueva') ? 'nueva' : 'listado';

  const [step, setStep] = useState(0);

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [loadingCustomer, setLoadingCustomer] = useState(true);
  const [loadingVehicles, setLoadingVehicles] = useState(false);
  const [customerSearch, setCustomerSearch] = useState('');
  const [customerSearchOpen, setCustomerSearchOpen] = useState(false);

  const [form, setForm] = useState({
    mileage: '' as string,
    fuelLevel: '' as string,
    exteriorState: '',
    generalState: '',
    symptoms: '',
    observations: '',
  });
  const [accessories, setAccessories] = useState<string[]>([]);

  const [damages, setDamages] = useState<DamageDraft[]>([]);
  const [side, setSide] = useState<Side>('L');
  const [selectedZone, setSelectedZone] = useState<string | null>(null);
  const [dampType, setDampType] = useState<string>(DAMAGE_TYPES[0]);
  const [dampSeverity, setDampSeverity] = useState('MODERADO');
  const [dampDesc, setDampDesc] = useState('');
  const [dampObs, setDampObs] = useState('');
  const [dampPhoto, setDampPhoto] = useState<File | null>(null);
  const [dampPhotoPreview, setDampPhotoPreview] = useState<string | null>(null);
  const damagePhotoRef = useRef<HTMLInputElement | null>(null);

  const [photos, setPhotos] = useState<(File | null)[]>(Array(PHOTO_CATEGORIES.length).fill(null));
  const [photoPreviews, setPhotoPreviews] = useState<(string | null)[]>(Array(PHOTO_CATEGORIES.length).fill(null));
  const [saving, setSaving] = useState(false);
  const [created, setCreated] = useState<Reception | null>(null);
  const [orderNumber, setOrderNumber] = useState<string | null>(null);
  const [emitting, setEmitting] = useState(false);
  const fileRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    getAllCustomers()
      .then((data) => setCustomers(data))
      .catch(() => toast.error('Error', 'No se pudieron cargar los clientes.'))
      .finally(() => setLoadingCustomer(false));
  }, [toast]);

  const filteredCustomers = useMemo(() => {
    const q = customerSearch.toLowerCase().trim();
    if (!q) return customers.slice(0, 8);
    return customers.filter(
      (c) =>
        `${c.firstName} ${c.lastName}`.toLowerCase().includes(q) ||
        c.documentNumber.includes(q) ||
        (c.code || '').toLowerCase().includes(q)
    );
  }, [customers, customerSearch]);

  const selectCustomer = async (c: Customer) => {
    setCustomer(c);
    setVehicle(null);
    setCustomerSearchOpen(false);
    setLoadingVehicles(true);
    try {
      setVehicles(await getVehiclesByCustomer(c.id!));
    } catch {
      toast.error('Error', 'No se pudieron cargar los vehículos del cliente.');
    } finally {
      setLoadingVehicles(false);
    }
  };

  const removeDamage = (index: number) => setDamages((ds) => ds.filter((_, i) => i !== index));

  const resetDamageDraft = () => {
    setDampType(DAMAGE_TYPES[0]);
    setDampSeverity('MODERADO');
    setDampDesc('');
    setDampObs('');
    setDampPhoto(null);
    setDampPhotoPreview(null);
  };

  const handleSelectZone = (code: string) => {
    setSelectedZone((prev) => {
      if (prev !== code) resetDamageDraft();
      return code;
    });
  };

  const handleSideChange = (s: Side) => {
    setSide(s);
    setSelectedZone((prev) => {
      if (!prev) return prev;
      const z = ZONE_MAP.get(prev);
      if (z && z.side !== 'B' && z.side !== s) {
        resetDamageDraft();
        return null;
      }
      return prev;
    });
  };

  const handleDamagePhotoChange = (file: File | null) => {
    setDampPhoto(file);
    if (file) {
      if (dampPhotoPreview) URL.revokeObjectURL(dampPhotoPreview);
      setDampPhotoPreview(URL.createObjectURL(file));
    } else {
      setDampPhotoPreview(null);
    }
  };

  const zoneInfo = selectedZone ? ZONE_MAP.get(selectedZone) : null;

  const addDamage = () => {
    if (!zoneInfo) return;
    const d: DamageDraft = {
      zone: zoneInfo.code,
      zoneLabel: zoneInfo.label,
      damageType: dampType,
      severity: dampSeverity,
      description: dampDesc.trim() || undefined,
      observation: dampObs.trim() || undefined,
      ...(dampPhoto ? { photo: dampPhoto } : {}),
    };
    setDamages((ds) => [...ds, d]);
    resetDamageDraft();
  };

  const damageCounts = useMemo(() => {
    const m: Record<string, number> = {};
    damages.forEach((d) => {
      m[d.zone] = (m[d.zone] || 0) + 1;
    });
    return m;
  }, [damages]);

  const handlePhotoChange = (index: number, file: File | null) => {
    setPhotos((p) => p.map((f, i) => (i === index ? file : f)));
    if (file) {
      setPhotoPreviews((p) => p.map((pv, i) => (i === index ? URL.createObjectURL(file) : pv)));
    } else {
      setPhotoPreviews((p) => p.map((pv, i) => (i === index ? null : pv)));
    }
  };

  const canSubmit =
    !!customer && !!vehicle && form.mileage !== '' && damages.length >= 0;

  const handleSubmit = async () => {
    setSaving(true);
    const generalFiles = photos.filter((f): f is File => f !== null);
    let offset = generalFiles.length;
    const payloadDamages: DamageRequest[] = damages.map((d) => {
      let photoIndex: number | undefined;
      if (d.photo) {
        photoIndex = offset;
        offset += 1;
      }
      return {
        zone: d.zone,
        zoneLabel: d.zoneLabel,
        damageType: d.damageType,
        severity: d.severity,
        description: d.description,
        observation: d.observation,
        photoIndex,
      };
    });
    const damageFiles = damages.filter((d) => d.photo).map((d) => d.photo as File);
    const files = [...generalFiles, ...damageFiles];
    try {
      const reception = await createReception(
        {
          vehicleId: vehicle!.id!,
          mileage: Number(form.mileage) || 0,
          fuelLevel: form.fuelLevel || undefined,
          exteriorState: form.exteriorState || undefined,
          generalState: form.generalState || undefined,
          accessories,
          customerSymptoms: form.symptoms || undefined,
          observations: form.observations || undefined,
          damages: payloadDamages,
        },
        files
      );
      setCreated(reception);
      toast.success('Recepción guardada', `Recepcion guardada con código ${reception.code}.`);
    } catch (err: any) {
      toast.error('Error', err?.response?.data?.message || 'No se pudo guardar la recepción.');
    } finally {
      setSaving(false);
    }
  };

  const handleEmitOrder = async () => {
    if (!created) return;
    setEmitting(true);
    try {
      const result = await emitOrderFromReception(created.id);
      setOrderNumber(result.orderNumber);
      toast.success('Orden emitida', `Orden ${result.orderNumber} creada correctamente.`);
    } catch (err: any) {
      toast.error('Error', err?.response?.data?.message || 'No se pudo emitir la orden de trabajo.');
    } finally {
      setEmitting(false);
    }
  };

  const resetAll = () => {
    setStep(0);
    setCustomer(null);
    setVehicle(null);
    setVehicles([]);
    setForm({ mileage: '', fuelLevel: '', exteriorState: '', generalState: '', symptoms: '', observations: '' });
    setAccessories([]);
    setDamages([]);
    setSide('L');
    setSelectedZone(null);
    resetDamageDraft();
    setPhotos(Array(PHOTO_CATEGORIES.length).fill(null));
    setPhotoPreviews(Array(PHOTO_CATEGORIES.length).fill(null));
    setCreated(null);
    setOrderNumber(null);
  };

  const goToOrder = () => navigate('/ordenes-trabajo');

  const continueDisabled =
    (step === 0 && (!customer || !vehicle)) ||
    (step === 1 && form.mileage === '') ||
    (step === 2 && damages.length === 0);

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Recepción de vehículo</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Ficha digital de entrada, inspección visual y órdenes de trabajo.
          </p>
        </div>
      </div>

      {/* Submenu del módulo */}
      <div className="inline-flex rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-1 shadow-sm">
        <button
          type="button"
          onClick={() => navigate('/recepcion')}
          className={cn(
            'inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition',
            view === 'listado' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <ClipboardList size={16} /> Listado de recepciones
        </button>
        <button
          type="button"
          onClick={() => navigate('/recepcion/nueva')}
          className={cn(
            'inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition',
            view === 'nueva' ? 'bg-blue-600 text-white shadow-sm' : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
          )}
        >
          <Plus size={16} /> Nueva recepción
        </button>
      </div>

      {view === 'listado' ? (
        <ReceptionList />
      ) : (
      <div className="mx-auto max-w-5xl space-y-6">
      {/* Stepper */}
      <ol className="flex items-center gap-2 overflow-x-auto pb-1">
        {STEPS.map((s, i) => {
          const done = i < step;
          const active = i === step;
          return (
            <li key={s.label} className="flex shrink-0 items-center gap-2">
              <button
                onClick={() => i < step && setStep(i)}
                disabled={!done && !active}
                className={cn(
                  'flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition-colors',
                  active
                    ? 'bg-blue-600 text-white'
                    : done
                      ? 'bg-blue-50 text-blue-700 hover:bg-blue-100 dark:bg-blue-500/15 dark:text-blue-400 dark:hover:bg-blue-500/20'
                      : 'cursor-default bg-slate-100 dark:bg-slate-800 text-slate-400 dark:text-slate-500'
                )}
              >
                <s.icon size={16} />
                {s.label}
                {done && <Check size={14} />}
              </button>
              {i < STEPS.length - 1 && <span className="h-0.5 w-6 shrink-0 bg-slate-200 dark:bg-slate-700" />}
            </li>
          );
        })}
      </ol>

      <div className="rounded-3xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-5 shadow-sm sm:p-8">
        {/* PASO 0: Cliente y vehículo */}
        {created && (
          <div className="animate-slide-up text-center">
            <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-400">
              {orderNumber ? <Check size={32} /> : <Car size={30} />}
            </div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-white">
              {orderNumber ? '¡Orden de trabajo emitida!' : 'Recepción guardada'}
            </h2>
            <p className="mx-auto mt-2 max-w-md text-sm text-slate-500 dark:text-slate-400">
              {orderNumber ? (
                <>
                  La recepción <span className="font-semibold text-slate-700 dark:text-slate-200">{created.code}</span> generó la orden{' '}
                  <span className="font-semibold text-blue-600 dark:text-blue-400">{orderNumber}</span>. Ya puede iniciar el diagnóstico.
                </>
              ) : (
                <>
                  La recepción <span className="font-semibold text-slate-700 dark:text-slate-200">{created.code}</span> para el vehículo{' '}
                  <span className="font-semibold text-slate-700 dark:text-slate-200">{created.vehicle.plate}</span> se registró con{' '}
                  {created.damages.length} daños y {created.photos.length} fotografías.
                </>
              )}
            </p>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
              {!orderNumber && (
                <button
                  onClick={handleEmitOrder}
                  disabled={emitting}
                  className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-blue-600/25 transition hover:from-blue-500 hover:to-indigo-500 disabled:opacity-60"
                >
                  {emitting ? (
                    <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                  ) : (
                    <FileText size={16} />
                  )}
                  Emitir orden de trabajo
                </button>
              )}
              <button
                onClick={goToOrder}
                className="inline-flex items-center gap-2 rounded-xl bg-slate-100 dark:bg-slate-800 px-5 py-2.5 text-sm font-medium text-slate-700 dark:text-slate-200 transition hover:bg-slate-200 dark:hover:bg-slate-700"
              >
                Ver órdenes
              </button>
              <button
                onClick={resetAll}
                className="inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-medium text-slate-500 dark:text-slate-400 transition hover:bg-slate-100 dark:hover:bg-slate-800"
              >
                Nueva recepción
              </button>
            </div>
          </div>
        )}

        {!created && step === 0 && (
          <div>
            <h2 className="mb-1 text-lg font-bold text-slate-900 dark:text-white">Selecciona el cliente y vehículo</h2>
            <p className="mb-6 text-sm text-slate-500 dark:text-slate-400">
              Busca el cliente por nombre o documento. Luego elige uno de sus vehículos.
            </p>

            <div className="grid gap-6 lg:grid-cols-2">
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">Cliente</label>
                <div className="relative">
                  <Search size={17} className="pointer-events-none absolute inset-y-0 left-3 my-auto text-slate-400 dark:text-slate-500" />
                  <input
                    value={customerSearch}
                    onChange={(e) => {
                      setCustomerSearch(e.target.value);
                      setCustomerSearchOpen(true);
                    }}
                    onFocus={() => setCustomerSearchOpen(true)}
                    onBlur={() => setTimeout(() => setCustomerSearchOpen(false), 150)}
                    className="block w-full rounded-xl border border-slate-300 dark:border-slate-600 py-2.5 pl-10 pr-3 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                    placeholder="Buscar cliente..."
                  />
                  {customerSearchOpen && !loadingCustomer && (
                    <div className="absolute z-20 mt-2 max-h-64 w-full overflow-y-auto rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 shadow-xl">
                      {filteredCustomers.length === 0 && (
                        <p className="px-4 py-3 text-sm text-slate-400 dark:text-slate-500">Sin resultados.</p>
                      )}
                      {filteredCustomers.map((c) => (
                        <button
                          key={c.id}
                          onClick={() => selectCustomer(c)}
                          className="flex w-full items-center gap-3 px-4 py-2.5 text-left transition hover:bg-blue-50 dark:hover:bg-blue-500/10"
                        >
                          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400">
                            <UserRound size={16} />
                          </span>
                          <span className="min-w-0">
                            <span className="block truncate text-sm font-medium text-slate-900 dark:text-white">
                              {c.firstName} {c.lastName}
                            </span>
                            <span className="block text-xs text-slate-500 dark:text-slate-400">
                              {c.documentNumber} · {c.phone}
                            </span>
                          </span>
                          {c.code && (
                            <span className="ml-auto rounded-md bg-slate-100 dark:bg-slate-800 px-2 py-0.5 text-[11px] font-medium text-slate-500 dark:text-slate-400">
                              {c.code}
                            </span>
                          )}
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                {customer && (
                  <div className="mt-3 flex items-center gap-3 rounded-xl border border-blue-100 bg-blue-50/60 p-3 animate-fade-in dark:border-blue-500/30 dark:bg-blue-500/10">
                    <span className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white">
                      {customer.firstName.charAt(0)}
                      {customer.lastName.charAt(0)}
                    </span>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-slate-900 dark:text-white">
                        {customer.firstName} {customer.lastName}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        {customer.documentNumber} · {customer.phone}
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        setCustomer(null);
                        setCustomerSearch('');
                        setVehicle(null);
                      }}
                      className="ml-auto rounded-lg p-1.5 text-slate-400 dark:text-slate-500 hover:bg-white dark:hover:bg-slate-800 hover:text-slate-600 dark:hover:text-slate-300"
                      aria-label="Quitar cliente"
                    >
                      <X size={16} />
                    </button>
                  </div>
                )}
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">Vehículo</label>
                {!customer ? (
                  <div className="rounded-xl border border-dashed border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-800/50 p-6 text-center text-sm text-slate-400 dark:text-slate-500">
                    Primero selecciona un cliente para ver sus vehículos.
                  </div>
                ) : loadingVehicles ? (
                  <div className="grid gap-3">
                    {Array.from({ length: 2 }).map((_, i) => (
                      <div key={i} className="h-16 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800" />
                    ))}
                  </div>
                ) : vehicles.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-800/50 p-6 text-center text-sm text-slate-400 dark:text-slate-500">
                    Este cliente no tiene vehículos registrados.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {vehicles.map((v) => (
                      <button
                        key={v.id}
                        onClick={() => setVehicle(v)}
                        className={cn(
                          'flex w-full items-center gap-3 rounded-xl border p-3 text-left transition',
                          vehicle?.id === v.id
                            ? 'border-blue-500 bg-blue-50/60 ring-2 ring-blue-500/20 dark:bg-blue-500/10'
                            : 'border-slate-200 dark:border-slate-700 hover:border-slate-300 dark:hover:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800/40'
                        )}
                      >
                        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400">
                          <Car size={18} />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-sm font-semibold text-slate-900 dark:text-white">{v.plate}</span>
                          <span className="block text-xs text-slate-500 dark:text-slate-400">
                            {v.brand} {v.model} {v.year} · {v.color}
                          </span>
                        </span>
                        {vehicle?.id === v.id && (
                          <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-white">
                            <Check size={14} />
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* PASO 1: Datos de recepción */}
        {!created && step === 1 && (
          <div>
            <h2 className="mb-1 text-lg font-bold text-slate-900 dark:text-white">Datos de la recepción</h2>
            <p className="mb-6 text-sm text-slate-500 dark:text-slate-400">Registra las condiciones de entrada del vehículo.</p>

            <div className="grid gap-5 md:grid-cols-2">
              <div>
                <label htmlFor="mileage" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Kilometraje actual (km)
                </label>
                <input
                  id="mileage"
                  type="number"
                  min={0}
                  value={form.mileage}
                  onChange={(e) => setForm((f) => ({ ...f, mileage: e.target.value }))}
                  className="block w-full rounded-xl border border-slate-300 dark:border-slate-600 px-3 py-2.5 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                  placeholder="Ej. 45200"
                />
              </div>

              <div>
                <label htmlFor="fuel" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Nivel de combustible
                </label>
                <div className="relative">
                  <Fuel size={17} className="pointer-events-none absolute inset-y-0 left-3 my-auto text-slate-400 dark:text-slate-500" />
                  <select
                    id="fuel"
                    value={form.fuelLevel}
                    onChange={(e) => setForm((f) => ({ ...f, fuelLevel: e.target.value }))}
                    className="block w-full appearance-none rounded-xl border border-slate-300 dark:border-slate-600 py-2.5 pl-10 pr-8 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                  >
                    <option value="">Seleccionar...</option>
                    {FUEL_LEVELS.map((f) => (
                      <option key={f} value={f}>
                        {f}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label htmlFor="exterior" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Estado exterior
                </label>
                <input
                  id="exterior"
                  value={form.exteriorState}
                  onChange={(e) => setForm((f) => ({ ...f, exteriorState: e.target.value }))}
                  className="block w-full rounded-xl border border-slate-300 dark:border-slate-600 px-3 py-2.5 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                  placeholder="Ej. Lavado regular, sin pisos"
                />
              </div>

              <div>
                <label htmlFor="general" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Estado general
                </label>
                <input
                  id="general"
                  value={form.generalState}
                  onChange={(e) => setForm((f) => ({ ...f, generalState: e.target.value }))}
                  className="block w-full rounded-xl border border-slate-300 dark:border-slate-600 px-3 py-2.5 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                  placeholder="Ej. Operativo, sin ruidos"
                />
              </div>

              <div className="md:col-span-2">
                <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Accesorios y documentos
                </label>
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                  {ACCESSORIES.map((a) => {
                    const selected = accessories.includes(a);
                    return (
                      <button
                        key={a}
                        type="button"
                        onClick={() =>
                          setAccessories((acc) => (selected ? acc.filter((x) => x !== a) : [...acc, a]))
                        }
                        className={cn(
                          'flex items-center gap-2 rounded-lg border px-3 py-2 text-left text-sm transition',
                          selected
                            ? 'border-blue-500 bg-blue-50/70 font-medium text-blue-700 dark:bg-blue-500/15 dark:text-blue-400'
                            : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-600'
                        )}
                      >
                        <span
                          className={cn(
                            'flex h-4 w-4 shrink-0 items-center justify-center rounded border',
                            selected ? 'border-blue-600 bg-blue-600 text-white' : 'border-slate-300 dark:border-slate-600'
                          )}
                        >
                          {selected && <Check size={11} />}
                        </span>
                        {a}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="md:col-span-2">
                <label htmlFor="symptoms" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Síntomas reportados por el cliente
                </label>
                <textarea
                  id="symptoms"
                  rows={3}
                  value={form.symptoms}
                  onChange={(e) => setForm((f) => ({ ...f, symptoms: e.target.value }))}
                  className="block w-full rounded-xl border border-slate-300 dark:border-slate-600 px-3 py-2.5 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                  placeholder="Ej. Ruido al frenar, se enciende la luz del motor..."
                />
              </div>

              <div className="md:col-span-2">
                <label htmlFor="obs" className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-200">
                  Observaciones
                </label>
                <textarea
                  id="obs"
                  rows={2}
                  value={form.observations}
                  onChange={(e) => setForm((f) => ({ ...f, observations: e.target.value }))}
                  className="block w-full rounded-xl border border-slate-300 dark:border-slate-600 px-3 py-2.5 text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30"
                  placeholder="Notas adicionales..."
                />
              </div>
            </div>
          </div>
        )}

        {/* PASO 2: Inspección visual */}
        {!created && step === 2 && (
          <div>
            <h2 className="mb-1 text-lg font-bold text-slate-900 dark:text-white">Inspección visual del vehículo</h2>
            <p className="mb-6 text-sm text-slate-500 dark:text-slate-400">
              Selecciona una zona del esquema, registra el daño y adjunta una fotografía. Cambia de lado (izquierda o
              derecha) para inspeccionar las puertas y espejos del otro costado.
            </p>

            <div className="grid gap-6 lg:grid-cols-5">
              <div className="lg:col-span-3">
                <VehicleInspector
                  side={side}
                  onSideChange={handleSideChange}
                  counts={damageCounts}
                  selectedZone={selectedZone}
                  onSelectZone={handleSelectZone}
                />
              </div>

              <div className="space-y-4 lg:col-span-2">
                {selectedZone && zoneInfo ? (
                  <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50/60 dark:bg-slate-800/50 p-4 animate-fade-in">
                    <div className="mb-3 flex items-center gap-2">
                      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-400">
                        <ClipboardCheck size={16} />
                      </span>
                      <div>
                        <p className="text-sm font-semibold text-slate-900 dark:text-white">Registrar daño</p>
                        <p className="text-xs text-blue-600 dark:text-blue-400">{zoneInfo.label}</p>
                      </div>
                    </div>

                    <div>
                      <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Tipo de daño</label>
                      <div className="grid grid-cols-2 gap-1.5">
                        {DAMAGE_TYPES.map((t) => (
                          <button
                            key={t}
                            type="button"
                            onClick={() => setDampType(t)}
                            className={cn(
                              'rounded-lg border px-2 py-1.5 text-xs font-medium transition',
                              dampType === t
                                ? 'border-blue-500 bg-blue-600 text-white'
                                : 'border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-300 hover:border-blue-300'
                            )}
                          >
                            {t}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="mt-3">
                      <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Nivel / severidad</label>
                      <div className="grid grid-cols-3 gap-1.5">
                        {SEVERITIES.map((s) => (
                          <button
                            key={s.value}
                            type="button"
                            onClick={() => setDampSeverity(s.value)}
                            className={cn(
                              'rounded-lg border px-2 py-1.5 text-xs font-semibold transition',
                              dampSeverity === s.value ? s.active : s.idle
                            )}
                          >
                            {s.label}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="mt-3">
                      <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Descripción</label>
                      <textarea
                        rows={2}
                        value={dampDesc}
                        onChange={(e) => setDampDesc(e.target.value)}
                        placeholder="Detalle del daño (opcional)"
                        className="block w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-2.5 py-2 text-sm text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                      />
                    </div>

                    <div className="mt-3">
                      <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Observación</label>
                      <textarea
                        rows={2}
                        value={dampObs}
                        onChange={(e) => setDampObs(e.target.value)}
                        placeholder="Notas adicionales (opcional)"
                        className="block w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-900 px-2.5 py-2 text-sm text-slate-900 dark:text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                      />
                    </div>

                    <div className="mt-3">
                      <label className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">Fotografía del daño</label>
                      <input
                        ref={damagePhotoRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={(e) => handleDamagePhotoChange(e.target.files?.[0] || null)}
                      />
                      {dampPhotoPreview ? (
                        <div className="relative">
                          <button
                            type="button"
                            onClick={() => damagePhotoRef.current?.click()}
                            className="group block h-28 w-full overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700"
                          >
                            <img src={dampPhotoPreview} alt="Foto del daño" className="h-full w-full object-cover" />
                            <span className="absolute inset-0 flex items-center justify-center bg-slate-900/0 transition group-hover:bg-slate-900/30">
                              <Camera size={20} className="opacity-0 transition group-hover:opacity-100" style={{ color: '#fff' }} />
                            </span>
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDamagePhotoChange(null)}
                            className="absolute -right-1.5 -top-1.5 flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-white shadow transition hover:bg-red-500"
                            aria-label="Quitar foto del daño"
                          >
                            <X size={13} />
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => damagePhotoRef.current?.click()}
                          className="flex h-28 w-full flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-slate-300 dark:border-slate-600 text-slate-400 dark:text-slate-500 transition hover:border-blue-400 hover:text-blue-500"
                        >
                          <Camera size={18} />
                          <span className="text-xs font-medium">Subir fotografía</span>
                        </button>
                      )}
                    </div>

                    <button
                      onClick={addDamage}
                      className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-blue-600/20 transition hover:bg-blue-500"
                    >
                      Agregar daño
                    </button>
                  </div>
                ) : (
                  <div className="rounded-2xl border border-dashed border-slate-300 dark:border-slate-600 bg-slate-50 dark:bg-slate-800/50 p-6 text-center">
                    <span className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-blue-50 text-blue-500 dark:bg-blue-500/15 dark:text-blue-400">
                      <Car size={22} />
                    </span>
                    <p className="text-sm font-medium text-slate-700 dark:text-slate-200">Selecciona una zona del vehículo</p>
                    <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">
                      Haz clic sobre el esquema o en una de las fichas de zona para registrar un daño.
                    </p>
                  </div>
                )}

                {damages.length > 0 && (
                  <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900">
                    <p className="border-b border-slate-100 dark:border-slate-800 px-4 py-2.5 text-sm font-semibold text-slate-900 dark:text-white">
                      Daños registrados ({damages.length})
                    </p>
                    <ul className="max-h-72 space-y-1 overflow-y-auto p-3">
                      {damages.map((d, i) => {
                        const sev = SEV_STYLE[d.severity] || SEV_STYLE.MODERADO;
                        return (
                          <li key={i} className="flex items-start gap-2.5 rounded-lg p-2 text-sm transition hover:bg-slate-50 dark:hover:bg-slate-800/40">
                            <span className={cn('mt-1.5 h-2 w-2 shrink-0 rounded-full', sev.dot)} />
                            <span className="min-w-0 flex-1">
                              <span className="block font-medium text-slate-900 dark:text-white">
                                {d.zoneLabel} · {d.damageType}
                              </span>
                              <span className={cn('block text-[11px] font-semibold', sev.text)}>
                                Severidad {d.severity}
                                {d.photo && <span className="ml-1.5 font-normal text-slate-400 dark:text-slate-500">· con foto</span>}
                              </span>
                              {d.description && <span className="mt-0.5 block text-xs text-slate-500 dark:text-slate-400">{d.description}</span>}
                              {d.observation && (
                                <span className="mt-0.5 block truncate text-xs italic text-slate-400 dark:text-slate-500">
                                  Obs: {d.observation}
                                </span>
                              )}
                            </span>
                            <button
                              onClick={() => removeDamage(i)}
                              className="rounded-lg p-1.5 text-slate-400 dark:text-slate-500 transition hover:bg-red-50 hover:text-red-500"
                              aria-label="Quitar daño"
                            >
                              <Trash2 size={15} />
                            </button>
                          </li>
                        );
                      })}
                    </ul>
                  </div>
                )}
              </div>
            </div>

            <div className="mt-8">
              <p className="mb-3 text-sm font-semibold text-slate-900 dark:text-white">Fotografías generales del vehículo</p>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7">
                {PHOTO_CATEGORIES.map((cat, i) => (
                  <div key={cat} className="relative">
                    <input
                      ref={(el) => {
                        fileRefs.current[i] = el;
                      }}
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={(e) => handlePhotoChange(i, e.target.files?.[0] || null)}
                    />
                    {photoPreviews[i] ? (
                      <button
                        onClick={() => fileRefs.current[i]?.click()}
                        className="group relative block h-24 w-full overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700"
                      >
                        <img src={photoPreviews[i]!} alt={cat} className="h-full w-full object-cover" />
                        <span className="absolute inset-0 flex items-center justify-center bg-slate-900/0 transition group-hover:bg-slate-900/30">
                          <Camera size={20} className="opacity-0 transition group-hover:opacity-100" style={{ color: '#fff' }} />
                        </span>
                      </button>
                    ) : (
                      <button
                        onClick={() => fileRefs.current[i]?.click()}
                        className="flex h-24 w-full flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed border-slate-300 dark:border-slate-600 text-slate-400 dark:text-slate-500 transition hover:border-blue-400 hover:text-blue-500"
                      >
                        <Upload size={16} />
                        <span className="text-center text-[10px] font-medium leading-tight">{cat}</span>
                      </button>
                    )}
                    {photoPreviews[i] && (
                      <button
                        onClick={() => handlePhotoChange(i, null)}
                        className="absolute -right-1.5 -top-1.5 flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-white shadow transition hover:bg-red-500"
                        aria-label={`Quitar foto ${cat}`}
                      >
                        <X size={13} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <p className="mt-3 text-xs text-slate-400 dark:text-slate-500">
                Las fotos generales se archivarán en el expediente digital junto con las fotografías de cada daño.
              </p>
            </div>
          </div>
        )}

        {/* PASO 3: Confirmar */}
        {!created && step === 3 && (
          <div>
            <h2 className="mb-1 text-lg font-bold text-slate-900 dark:text-white">Confirmar recepción</h2>
            <p className="mb-6 text-sm text-slate-500 dark:text-slate-400">Revisa el resumen antes de guardar la ficha digital.</p>

            <div className="grid gap-5 md:grid-cols-2">
              <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50/60 dark:bg-slate-800/50 p-5">
                <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">Cliente y vehículo</p>
                <p className="text-sm font-semibold text-slate-900 dark:text-white">
                  {customer?.firstName} {customer?.lastName}
                </p>
                <p className="text-sm text-slate-500 dark:text-slate-400">{customer?.documentNumber} · {customer?.phone}</p>
                <div className="mt-3 flex items-center gap-2 text-sm text-slate-700 dark:text-slate-200">
                  <Car size={16} className="text-slate-400 dark:text-slate-500" />
                  {vehicle?.plate} — {vehicle?.brand} {vehicle?.model} {vehicle?.year} ({vehicle?.color})
                </div>
              </div>

              <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50/60 dark:bg-slate-800/50 p-5">
                <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">Datos ingresados</p>
                <div className="grid grid-cols-2 gap-2 text-sm">
                  <span className="text-slate-500 dark:text-slate-400">Kilometraje</span>
                  <span className="text-right font-medium text-slate-900 dark:text-white">{Number(form.mileage).toLocaleString('es-ES')} km</span>
                  <span className="text-slate-500 dark:text-slate-400">Combustible</span>
                  <span className="text-right font-medium text-slate-900 dark:text-white">{form.fuelLevel || '—'}</span>
                  <span className="text-slate-500 dark:text-slate-400">Accesorios</span>
                  <span className="text-right font-medium text-slate-900 dark:text-white">{accessories.length}</span>
                  <span className="text-slate-500 dark:text-slate-400">Daños</span>
                  <span className="text-right font-medium text-slate-900 dark:text-white">{damages.length}</span>
                  <span className="text-slate-500 dark:text-slate-400">Fotografías</span>
                  <span className="text-right font-medium text-slate-900 dark:text-white">
                    {photos.filter(Boolean).length + damages.filter((d) => d.photo).length}
                  </span>
                  <span className="text-slate-500 dark:text-slate-400">Recibido por</span>
                  <span className="text-right font-medium text-slate-900 dark:text-white">
                    {user?.name} {user?.lastName}
                  </span>
                </div>
              </div>

              {damages.length > 0 && (
                <div className="rounded-2xl border border-slate-200 dark:border-slate-700 bg-slate-50/60 dark:bg-slate-800/50 p-5 md:col-span-2">
                  <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400 dark:text-slate-500">Resumen de la inspección</p>
                  <ul className="grid gap-2 sm:grid-cols-2">
                    {damages.map((d, i) => (
                      <li key={i} className="flex items-center gap-2 text-sm">
                        <span className="h-2 w-2 shrink-0 rounded-full bg-red-500" />
                        <span className="text-slate-700 dark:text-slate-200">
                          <span className="font-medium">{d.zoneLabel}</span> — {d.damageType} ({d.severity})
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {!created && (
        <div className="flex items-center justify-between gap-3">
          <button
            onClick={() => setStep((s) => Math.max(0, s - 1))}
            disabled={step === 0}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-2.5 text-sm font-medium text-slate-600 dark:text-slate-300 transition hover:bg-slate-50 dark:hover:bg-slate-800/40 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <ChevronLeft size={16} /> Anterior
          </button>
          {step < 3 ? (
            <button
              onClick={() => setStep((s) => Math.min(3, s + 1))}
              disabled={continueDisabled}
              className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-blue-600/25 transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Siguiente <ChevronRight size={16} />
            </button>
          ) : (
            <button
              onClick={handleSubmit}
              disabled={saving || !canSubmit}
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-6 py-2.5 text-sm font-semibold text-white shadow-lg shadow-emerald-600/25 transition hover:from-emerald-500 hover:to-teal-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saving ? (
                <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              ) : (
                <Check size={16} />
              )}
              Guardar recepción
            </button>
          )}
        </div>
      )}
      </div>
      )}
    </div>
  );
}
