import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import Vehicles from './Vehicles';
import { getAllVehicles } from '../services/vehicleService';
import { getAllCustomers } from '../services/customerService';
import { ToastProvider } from '../components/ui/Toast';

vi.mock('../services/vehicleService', () => ({
  getAllVehicles: vi.fn(),
  createVehicle: vi.fn(),
  updateVehicle: vi.fn(),
  getVehicleHistory: vi.fn(),
  deleteVehicle: vi.fn(),
}));
vi.mock('../services/customerService', () => ({ getAllCustomers: vi.fn() }));

const mockVehicles = Array.from({ length: 12 }, (_, i) => ({
  id: i + 1,
  code: `V${i + 1}`,
  plate: `ABC-${String(i + 1).padStart(3, '0')}`,
  brand: 'Toyota',
  model: `Corolla ${i + 1}`,
  year: 2020,
  color: 'Rojo',
  customer: { id: 1, firstName: 'Ana', lastName: 'Pérez', documentNumber: '12345678' },
  currentMileage: 1000 + i,
  fuelType: 'Gasolina',
  active: true,
}));

function renderVehicles() {
  return render(
    <MemoryRouter>
      <ToastProvider>
        <Vehicles />
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('Vehicles (vehículos)', () => {
  beforeEach(() => {
    (getAllVehicles as Mock).mockResolvedValue(mockVehicles);
    (getAllCustomers as Mock).mockResolvedValue([]);
  });

  it('muestra los vehículos y el contador', async () => {
    renderVehicles();

    expect(await screen.findByText('ABC-001')).toBeInTheDocument();
    expect(screen.getByText('12 vehículos')).toBeInTheDocument();
  });

  it('filtra por placa con la búsqueda', async () => {
    renderVehicles();
    const user = userEvent.setup();
    await screen.findByText('ABC-001');

    await user.type(screen.getByPlaceholderText('Buscar por placa, marca o cliente...'), 'ABC-002');

    expect(screen.getByText('1 vehículos')).toBeInTheDocument();
    expect(screen.getByText('ABC-002')).toBeInTheDocument();
    expect(screen.queryByText('ABC-003')).not.toBeInTheDocument();
  });

  it('pagina los vehículos (10 por página)', async () => {
    renderVehicles();
    const user = userEvent.setup();
    await screen.findByText('ABC-001');

    // Página 1: 001..010
    expect(screen.getByText('ABC-010')).toBeInTheDocument();
    expect(screen.queryByText('ABC-011')).not.toBeInTheDocument();

    // Página 2: 011..012
    const nav = screen.getByRole('navigation', { name: 'Paginación' });
    await user.click(within(nav).getByRole('button', { name: '2' }));

    expect(screen.getByText('ABC-011')).toBeInTheDocument();
    expect(screen.queryByText('ABC-001')).not.toBeInTheDocument();
  });

  it('abre el modal de registro de vehículo', async () => {
    renderVehicles();
    const user = userEvent.setup();
    await screen.findByText('ABC-001');

    await user.click(screen.getByRole('button', { name: /Registrar Vehículo/i }));

    expect(screen.getByRole('heading', { name: 'Registrar vehículo' })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('ABC-123')).toBeInTheDocument();
  });
});
