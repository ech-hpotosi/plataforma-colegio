import type { Rol } from '../tipos';
import { ErrorApi, llamarApi } from './cliente';

export interface UsuarioActual {
  id: number;
  nombreUsuario: string;
  nombreCompleto: string;
  roles: Rol[];
}

export function iniciarSesion(nombreUsuario: string, contrasena: string): Promise<UsuarioActual> {
  return llamarApi<UsuarioActual>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ nombreUsuario, contrasena }),
  });
}

export function cerrarSesion(): Promise<void> {
  return llamarApi<void>('/auth/logout', { method: 'POST' });
}

/** Devuelve el usuario con sesion abierta, o null si no hay sesion. */
export async function consultarUsuarioActual(): Promise<UsuarioActual | null> {
  try {
    return await llamarApi<UsuarioActual>('/yo');
  } catch (error) {
    if (error instanceof ErrorApi && error.estado === 401) {
      return null;
    }
    throw error;
  }
}
