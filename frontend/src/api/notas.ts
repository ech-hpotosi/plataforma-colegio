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
  /** Nota maxima que deja una recuperacion. */
  topeRecuperacion: number;
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
  /** Nota calculada del periodo, antes de la recuperacion. */
  notaPeriodo: number | null;
  recuperacion: number | null;
  observacionRecuperacion: string | null;
  notaDefinitiva: number | null;
  /** Desempeno de la nota definitiva. */
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
  /** Las recuperaciones se registran aunque el periodo este cerrado, mientras el anio siga abierto. */
  admiteRecuperacion: boolean;
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

export interface RecuperacionFinal {
  cargaId: number;
  grupo: string;
  asignatura: string;
  editable: boolean;
  configuracion: ConfiguracionEvaluacion;
  estudiantes: {
    matriculaId: number;
    nombres: string;
    apellidos: string;
    notaAnio: number | null;
    completa: boolean;
    recuperacion: number | null;
    observacion: string | null;
    notaDefinitiva: number | null;
    desempeno: Desempeno | null;
  }[];
}

export type ItemRecuperacion = { matriculaId: number; nota: number | null; observacion: string | null };

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

/** Convierte lo que escribe el docente (acepta coma) en nota; undefined si no es valida, null si esta vacia. */
export function leerNota(texto: string, config: ConfiguracionEvaluacion): number | null | undefined {
  const limpio = texto.trim().replace(',', '.');
  if (limpio === '') return null;
  if (!/^\d+(\.\d)?$/.test(limpio)) return undefined;
  const valor = Number(limpio);
  return valor >= config.notaMinima && valor <= config.notaMaxima ? valor : undefined;
}

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

export const guardarRecuperaciones = (cargaId: number, periodoId: number, recuperaciones: ItemRecuperacion[]) =>
  llamarApi<PlanillaNotas>(
    `/notas/cargas/${cargaId}/periodos/${periodoId}/recuperaciones`,
    json('PUT', { recuperaciones }),
  );
export const obtenerRecuperacionFinal = (cargaId: number) =>
  llamarApi<RecuperacionFinal>(`/notas/cargas/${cargaId}/recuperacion-final`);
export const guardarRecuperacionFinal = (cargaId: number, recuperaciones: ItemRecuperacion[]) =>
  llamarApi<RecuperacionFinal>(`/notas/cargas/${cargaId}/recuperacion-final`, json('PUT', { recuperaciones }));

export const obtenerConsolidadoNotas = (grupoId: number, periodoId: number | null) =>
  llamarApi<ConsolidadoNotas>(
    `/notas/grupos/${grupoId}/consolidado${periodoId === null ? '' : `?periodoId=${periodoId}`}`,
  );
