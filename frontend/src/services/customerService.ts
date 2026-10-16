import api from './api';

export interface Customer {
  id?: number;
  code?: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  phone: string;
  whatsapp?: string;
  email: string;
  address?: string;
  registrationDate?: string;
  active: boolean;
  observations?: string;
}

export const getAllCustomers = async () => {
  const response = await api.get('/clientes');
  return response.data;
};

export const getCustomerById = async (id: number) => {
  const response = await api.get(`/clientes/${id}`);
  return response.data;
};

export const createCustomer = async (customer: Customer) => {
  const response = await api.post('/clientes', customer);
  return response.data;
};

export const updateCustomer = async (id: number, customer: Customer) => {
  const response = await api.put(`/clientes/${id}`, customer);
  return response.data;
};

export const deleteCustomer = async (id: number) => {
  await api.delete(`/clientes/${id}`);
};
