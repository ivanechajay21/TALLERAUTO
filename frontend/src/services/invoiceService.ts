import api from './api';

export interface Invoice {
  id: number;
  invoiceNumber: string;
  issueDate: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  status: 'PENDING' | 'PAID' | 'CANCELLED';
  paymentMethod?: string;
  customer?: string;
  documentNumber?: string;
  vehicle?: string;
  workOrder?: { id: number; orderNumber: string };
}

export const getAllInvoices = async (): Promise<Invoice[]> => {
  const response = await api.get('/facturas');
  return response.data;
};

export const getInvoiceById = async (id: number): Promise<Invoice> => {
  const response = await api.get(`/facturas/${id}`);
  return response.data;
};

export const generateInvoice = async (workOrderId: number): Promise<Invoice> => {
  const response = await api.post(`/facturas/orden-trabajo/${workOrderId}`);
  return response.data;
};

export const payInvoice = async (id: number, paymentMethod: string): Promise<Invoice> => {
  const response = await api.patch(`/facturas/${id}/pagar`, null, { params: { paymentMethod } });
  return response.data;
};