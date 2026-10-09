import { llamarApi } from './cliente';

export type Dimension = 'SABER' | 'HACER' | 'SER';
export type Desempeno = 'BAJO' | 'BASICO' | 'ALTO' | 'SUPERIOR';

export const DIMENSIONES: Dimension[] = ['SABER', 'HACER', 'SER'];
export const NOMBRE_DIMENSION: Record<Dimension, string> = { SABER: 'Saber', HACER: 'Hacer', SER: 'Ser' };
export const NOMBRE_DESEMPENO: Record<Desempeno, string> = {
  BAJO: 'Bajo',
  BASICO: 'Básico',
  ALTO: 'Alto',
  SUPERIOR: 'Superior',
};

export interface ConfiguracionEvaluacion {
  anioLectivoId: number;
  anio: number;
  notaMinima: number;
  notaMaxima: number;
  notaAprobatoria: number;
  limiteAlto: number;
  limiteSuperior: number;
  pesoSaber: number;
  pesoHacer: number;
  pesoSer: number;
  editable: boolean;
}

export type DatosConfiguracion = Omit<ConfiguracionEvaluacion, 'anioLectivoId' | 'anio' | 'editable'>;

export interface PeriodoNotas {
  id: number;
  numero: number;
  fechaInicio: string;
  fechaFin: string;
  porcentaje: number;
  cerrado: boolean;
}

export interface CargaNotas {
  cargaId: number;
  grupoId: number;
  anio: number;
  sede: string;
  grupo: string;
  asignatura: string;
  docente: string;
  cualitativa: boolean;
  periodos: PeriodoNotas[];
}

export interface Actividad {
  id: number;
  dimension: Dimension;
  nombre: string;
  fecha: string | null;
  /** 1 es normal, 2 vale el doble, hasta 5. */
  peso: number;
}

export interface FilaPlanilla {
  matriculaId: number;
  nombres: string;
  apellidos: string;
  /** Nota por id de actividad. */
  notas: Record<string, number>;
  saber: number | null;
  hacer: number | null;
  ser: number | null;
  notaPeriodo: number | null;
  desempeno: Desempeno | null;
  completa: boolean;
}

export interface PlanillaNotas {
  cargaId: number;
  grupo: string;
  asignatura: string;
  periodoId: number;
  periodo: number;
  editable: boolean;
  configuracion: ConfiguracionEvaluacion;
  actividades: Actividad[];
  estudiantes: FilaPlanilla[];
}

export interface DatosActividad {
  dimension: Dimension;
  nombre: string;
  fecha: string | null;
  peso: number;
}

export interface NotaConsolidado {
  cargaId: number;
  nota: number | null;
  desempeno: Desempeno | null;
  completa: boolean;
}

export interface ConsolidadoNotas {
  grupoId: number;
  grupo: string;
  periodoId: number | null;
  periodos: PeriodoNotas[];
  configuracion: ConfiguracionEvaluacion;
  asignaturas: { cargaId: number; nombre: string; docente: string }[];
  estudiantes: {
    matriculaId: number;
    nombres: string;
    apellidos: string;
    notas: NotaConsolidado[];
    asignaturasEnBajo: number;
  }[];
}

const json = (metodo: string, cuerpo: unknown): RequestInit => ({ method: metodo, body: JSON.stringify(cuerpo) });

/** Periodo que contiene la fecha de hoy; si no hay, el ultimo que ya empezo o el primero. */
export function periodoActual(periodos: PeriodoNotas[], hoy: string): PeriodoNotas | undefined {
  return (
    periodos.find((p) => p.fechaInicio <= hoy && hoy <= p.fechaFin) ??
    [...periodos].reverse().find((p) => p.fechaInicio <= hoy) ??
    periodos[0]
  );
}

export const obtenerConfiguracion = (anioId: number) =>
  llamarApi<ConfiguracionEvaluacion>(`/notas/configuracion/${anioId}`);
export const guardarConfiguracion = (anioId: number, datos: DatosConfiguracion) =>
  llamarApi<ConfiguracionEvaluacion>(`/notas/configuracion/${anioId}`, json('PUT', datos));

export const listarCargasNotas = () => llamarApi<CargaNotas[]>('/notas/cargas');
export const obtenerPlanilla = (cargaId: number, periodoId: number) =>
  llamarApi<PlanillaNotas>(`/notas/cargas/${cargaId}/periodos/${periodoId}`);
export const guardarNotas = (
  cargaId: number,
  periodoId: number,
  notas: { actividadId: number; matriculaId: number; valor: number | null }[],
) => llamarApi<PlanillaNotas>(`/notas/cargas/${cargaId}/periodos/${periodoId}`, json('PUT', { notas }));
export const crearActividad = (cargaId: number, periodoId: number, datos: DatosActividad) =>
  llamarApi<PlanillaNotas>(`/notas/cargas/${cargaId}/periodos/${periodoId}/actividades`, json('POST', datos));
export const modificarActividad = (id: number, datos: DatosActividad) =>
  llamarApi<PlanillaNotas>(`/notas/actividades/${id}`, json('PUT', datos));
export const eliminarActividad = (id: number) =>
  llamarApi<PlanillaNotas>(`/notas/actividades/${id}`, { method: 'DELETE' });

export const obtenerConsolidadoNotas = (grupoId: number, periodoId: number | null) =>
  llamarApi<ConsolidadoNotas>(
    `/notas/grupos/${grupoId}/consolidado${periodoId === null ? '' : `?periodoId=${periodoId}`}`,
  );
