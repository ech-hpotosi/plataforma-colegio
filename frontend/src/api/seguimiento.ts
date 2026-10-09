import { llamarApi } from './cliente';
import type { Dimension } from './notas';

export interface AvanceClase {
  cargaId: number;
  anio: number;
  sede: string;
  grupo: string;
  asignatura: string;
  docente: string | null;
  periodoId: number;
  periodoNumero: number;
  inicioPeriodo: string;
  cierrePeriodo: string;
  periodoCerrado: boolean;
  estudiantes: number;
  cualitativa: boolean;
  actividades: number;
  /** Dimensiones que pesan en la nota y todavia no tienen actividades. */
  dimensionesSinActividad: Dimension[];
  notasRegistradas: number;
  notasEsperadas: number;
  porcentajeNotas: number;
  diasConAsistencia: number;
  ultimaAsistencia: string | null;
  diasSinAsistencia: number;
}

export interface AvanceRegistro {
  hoy: string;
  clases: AvanceClase[];
}

/** periodo es el numero (1, 2, 3); sin el, el periodo en curso de cada clase. */
export const obtenerAvance = (periodo?: number) =>
  llamarApi<AvanceRegistro>(periodo ? `/seguimiento/avance?periodo=${periodo}` : '/seguimiento/avance');
