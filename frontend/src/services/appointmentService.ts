import api from './api';

export interface Appointment {
  id?: number;
  appointmentDate: string;
  appointmentTime: string;
  requestedService: string;
  estimatedDurationMinutes?: number;
  priority?: string;
  status: string;
  observations?: string;
  customer?: {
    id: number;
    firstName: string;
    lastName: string;
    documentNumber: string;
  };
  vehicle?: {
    id: number;
    plate: string;
    brand: string;
    model: string;
  };
}

export const getAllAppointments = async (): Promise<Appointment[]> => {
  const response = await api.get('/citas');
  return response.data;
};

export const getAppointmentsByDate = async (date: string): Promise<Appointment[]> => {
  const response = await api.get(`/citas/fecha/${date}`);
  return response.data;
};

export const createAppointment = async (
  customerId: number,
  vehicleId: number,
  appointment: Partial<Appointment>
) => {
  const response = await api.post(`/citas/cliente/${customerId}/vehiculo/${vehicleId}`, appointment);
  return response.data;
};

export const updateAppointmentStatus = async (id: number, status: string) => {
  const response = await api.patch(`/citas/${id}/estado`, null, { params: { status } });
  return response.data;
};