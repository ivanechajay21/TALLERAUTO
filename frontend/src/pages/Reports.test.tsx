import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import Reports from './Reports';
import { getReporteCentro } from '../services/reportService';
import { ToastProvider } from '../components/ui/Toast';
import type { ReporteCentro } from '../services/reportService';

vi.mock('../services/reportService', () => ({ getReporteCentro: vi.fn() }));
vi.mock('../services/exportService', () => ({
  exportCentroPdf: vi.fn(),
  exportCentroExcel: vi.fn(),
}));
vi.mock('../services/authService', () => ({
  getCurrentUser: () => ({ name: 'Ana', lastName: 'Pérez' }),
}));
vi.mock('../context/SettingsContext', () => ({
  useShopSettings: () => ({ settings: null, loading: false, refresh: async () => {}, save: async () => ({}) }),
}));

function centro(tipo: string, filas: number): ReporteCentro {
  return {
    tipo: tipo as ReporteCentro['tipo'],
    titulo: `Reporte de ${tipo}`,
    columnas: ['Código', 'Cliente'],
    tipos: ['TEXT', 'TEXT'],
    filas: Array.from({ length: filas }, (_, i) => [`C${i + 1}`, `Cliente ${i + 1}`]),
    totales: [],
  };
}

function renderReports() {
  return render(
    <ToastProvider>
      <Reports />
    </ToastProvider>
  );
}

describe('Reports (Centro de Reportes)', () => {
  beforeEach(() => {
    (getReporteCentro as Mock).mockImplementation(async (tipo: string) =>
      centro(tipo, tipo === 'CLIENTES' ? 25 : 2)
    );
  });

  it('muestra las tarjetas de categoría y el reporte activo', async () => {
    renderReports();

    expect(screen.getByRole('heading', { name: 'Centro de Reportes' })).toBeInTheDocument();
    expect(await screen.findByText('Reporte de CLIENTES')).toBeInTheDocument();
    expect(await screen.findByText('C1')).toBeInTheDocument();
  });

  it('muestra el contador de registros del reporte', async () => {
    renderReports();

    expect(await screen.findByText(/25 de 25 registros/)).toBeInTheDocument();
  });

  it('filtra las filas con la búsqueda', async () => {
    renderReports();
    const user = userEvent.setup();
    await screen.findByText('C1');

    await user.type(screen.getByPlaceholderText('Buscar en el reporte…'), 'Cliente 25');

    expect(screen.getByText(/1 de 25 registros/)).toBeInTheDocument();
    expect(screen.getByText('C25')).toBeInTheDocument();
    expect(screen.queryByText('C1')).not.toBeInTheDocument();
  });

  it('pagina los resultados (10 por página)', async () => {
    renderReports();
    const user = userEvent.setup();
    await screen.findByText('C1');

    // Página 1: filas 1..10
    expect(screen.getByText('C1')).toBeInTheDocument();
    expect(screen.queryByText('C11')).not.toBeInTheDocument();

    // Ir a la página 2: filas 11..20
    const nav = screen.getByRole('navigation', { name: 'Paginación' });
    await user.click(within(nav).getByRole('button', { name: '2' }));

    expect(screen.getByText('C11')).toBeInTheDocument();
    expect(screen.queryByText('C1')).not.toBeInTheDocument();
  });
});
