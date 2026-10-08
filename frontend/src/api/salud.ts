import { llamarApi } from './cliente';

export interface Salud {
  estado: string;
  aplicacion: string;
  fecha: string;
}

export function consultarSalud(): Promise<Salud> {
  return llamarApi<Salud>('/salud');
}
