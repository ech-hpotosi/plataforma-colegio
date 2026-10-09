import { ErrorApi } from '../../api/cliente';

/** Texto para mostrar al usuario cuando falla una llamada a la API. */
export function mensajeDeError(e: unknown): string {
  return e instanceof ErrorApi ? e.message : 'No hay conexión con el servidor';
}
