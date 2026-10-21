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

/** Cada usuario cambia su propia contrasena confirmando la actual. */
export function cambiarMiContrasena(actual: string, nueva: string): Promise<void> {
  return llamarApi<void>('/yo/contrasena', { method: 'PUT', body: JSON.stringify({ actual, nueva }) });
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

export interface DatosRegistro {
  tipoDocumento: string;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  contrasena: string;
}

/** Crea la cuenta de acudiente y deja la sesion iniciada. */
export function registrarAcudiente(datos: DatosRegistro): Promise<UsuarioActual> {
  return llamarApi<UsuarioActual>('/auth/registro', { method: 'POST', body: JSON.stringify(datos) });
}
