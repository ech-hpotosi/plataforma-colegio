import type { Pagina, Rol, TipoDocumento } from '../tipos';
import { llamarApi } from './cliente';

export interface Usuario {
  id: number;
  nombreUsuario: string;
  activo: boolean;
  bloqueado: boolean;
  ultimoAcceso: string | null;
  roles: Rol[];
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string | null;
  correo: string | null;
}

export interface NuevoUsuario {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  nombreUsuario: string;
  contrasena: string;
  roles: Rol[];
}

export interface CambiosUsuario {
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  activo: boolean;
  roles: Rol[];
}

export function buscarUsuarios(buscar: string, pagina: number, tamano: number): Promise<Pagina<Usuario>> {
  const parametros = new URLSearchParams({ buscar, pagina: String(pagina), tamano: String(tamano) });
  return llamarApi<Pagina<Usuario>>('/usuarios?' + parametros.toString());
}

export function crearUsuario(datos: NuevoUsuario): Promise<Usuario> {
  return llamarApi<Usuario>('/usuarios', { method: 'POST', body: JSON.stringify(datos) });
}

export function actualizarUsuario(id: number, datos: CambiosUsuario): Promise<Usuario> {
  return llamarApi<Usuario>(`/usuarios/${id}`, { method: 'PUT', body: JSON.stringify(datos) });
}

export function cambiarContrasena(id: number, contrasena: string): Promise<void> {
  return llamarApi<void>(`/usuarios/${id}/contrasena`, { method: 'PUT', body: JSON.stringify({ contrasena }) });
}

export function desbloquearUsuario(id: number): Promise<Usuario> {
  return llamarApi<Usuario>(`/usuarios/${id}/desbloquear`, { method: 'POST' });
}
