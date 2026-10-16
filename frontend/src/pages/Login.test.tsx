import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';
import Login from './Login';
import { login, getCurrentUser } from '../services/authService';
import { ToastProvider } from '../components/ui/Toast';

vi.mock('../services/authService', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
}));

vi.mock('../components/ThemeToggle', () => ({ default: () => null }));

function renderLogin() {
  return render(
    <MemoryRouter>
      <ToastProvider>
        <Login />
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('Login', () => {
  beforeEach(() => {
    (login as Mock).mockReset();
    (getCurrentUser as Mock).mockReturnValue({
      name: 'Ana',
      lastName: 'Pérez',
      username: 'ana',
      roleName: 'ROLE_ADMIN',
      token: 'token-de-prueba',
    });
  });

  it('renderiza el formulario de acceso', () => {
    renderLogin();

    expect(screen.getByRole('heading', { name: 'Iniciar Sesión' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Usuario o Correo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Contraseña/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Entrar al Sistema/i })).toBeInTheDocument();
  });

  it('muestra un error cuando las credenciales son incorrectas (401)', async () => {
    (login as Mock).mockRejectedValue({ response: { status: 401 } });
    renderLogin();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/Usuario o Correo/i), 'admin');
    await user.type(screen.getByLabelText(/^Contraseña/i), 'incorrecta');
    await user.click(screen.getByRole('button', { name: /Entrar al Sistema/i }));

    expect(await screen.findByText('Usuario o contraseña incorrectos.')).toBeInTheDocument();
  });

  it('tras un login correcto muestra la animación de ingreso', async () => {
    (login as Mock).mockResolvedValue(undefined);
    renderLogin();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/Usuario o Correo/i), 'admin');
    await user.type(screen.getByLabelText(/^Contraseña/i), 'admin123');
    await user.click(screen.getByRole('button', { name: /Entrar al Sistema/i }));

    expect(await screen.findByText('Iniciando sesión')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('admin', 'admin123', true);
  });
});
