import api from './api';

export interface UserInfo {
  id: number;
  name: string;
  lastName: string;
  username: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  active: boolean;
  lastLogin?: string;
  roleName?: string;
  createdAt?: string;
}

export interface MechanicInfo {
  id: number;
  name: string;
  lastName: string;
  username: string;
  phone?: string;
}

export const getMechanics = async (): Promise<MechanicInfo[]> => {
  const response = await api.get('/usuarios/mecanicos');
  return response.data;
};

export const getAllUsers = async (): Promise<UserInfo[]> => {
  const response = await api.get('/usuarios');
  return response.data;
};

export const getUserById = async (id: number): Promise<UserInfo> => {
  const response = await api.get(`/usuarios/${id}`);
  return response.data;
};

export interface UpdateUserPayload {
  name?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  roleName?: string;
  newPassword?: string;
}

export interface CreateUserPayload {
  name: string;
  lastName: string;
  username: string;
  email: string;
  phone?: string;
  roleName: string;
  password: string;
}

export const createUser = async (payload: CreateUserPayload): Promise<UserInfo> => {
  const response = await api.post('/usuarios', payload);
  return response.data;
};

export const updateUser = async (id: number, payload: UpdateUserPayload): Promise<UserInfo> => {
  const response = await api.put(`/usuarios/${id}`, payload);
  return response.data;
};

export const setUserActive = async (id: number, active: boolean): Promise<UserInfo> => {
  const response = await api.patch(`/usuarios/${id}/estado`, null, { params: { active } });
  return response.data;
};