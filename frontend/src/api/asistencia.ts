import { llamarApi } from './cliente';

export type EstadoAsistencia = 'ASISTIO' | 'FALTA' | 'FALTA_JUSTIFICADA' | 'RETARDO' | 'PERMISO';

export const NOMBRE_ESTADO_ASISTENCIA: Record<EstadoAsistencia, string> = {
  ASISTIO: 'Asistio',
  FALTA: 'Falta',
  FALTA_JUSTIFICADA: 'Falta justificada',
  RETARDO: 'Retardo',
  PERMISO: 'Permiso',
};

export interface CargaDocente {
  cargaId: number;
  grupoId: number;
  anio: number;
  sede: string;
  grupo: string;
  asignatura: string;
  docente: string;
}

export interface EstudianteAsistencia {
  matriculaId: number;
  nombres: string;
  apellidos: string;
  estado: EstadoAsistencia;
  observacion: string | null;
  justificacion: string | null;
}

export interface AsistenciaClase {
  cargaId: number;
  grupo: string;
  asignatura: string;
  fecha: string;
  periodo: number;
  horas: number;
  registrada: boolean;
  estudiantes: EstudianteAsistencia[];
}

export interface ItemAsistencia {
  matriculaId: number;
  estado: EstadoAsistencia;
  observacion: string | null;
}

export interface GrupoAsistencia {
  grupoId: number;
  anio: number;
  sede: string;
  grupo: string;
}

export interface InasistenciaAsignatura {
  asignaturaId: number;
  horasSinJustificar: number;
  horasJustificadas: number;
  horasRetardo: number;
  porcentaje: number;
  superaLimite: boolean;
}

export interface ResumenGrupo {
  grupoId: number;
  grupo: string;
  semanasLectivas: number;
  porcentajeMaximo: number;
  asignaturas: { asignaturaId: number; nombre: string; horasAnuales: number }[];
  estudiantes: {
    matriculaId: number;
    nombres: string;
    apellidos: string;
    asignaturas: InasistenciaAsignatura[];
    superaLimite: boolean;
  }[];
}

export interface NovedadAsistencia {
  detalleId: number;
  fecha: string;
  asignatura: string;
  horas: number;
  estado: EstadoAsistencia;
  observacion: string | null;
  justificacion: string | null;
  fechaLimite: string;
}

const json = (metodo: string, cuerpo: unknown): RequestInit => ({ method: metodo, body: JSON.stringify(cuerpo) });

/** Fecha de hoy en la hora local del equipo (toISOString usaria la hora UTC y en la noche daria el dia siguiente). */
export function hoyIso(): string {
  const hoy = new Date();
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${hoy.getFullYear()}-${dos(hoy.getMonth() + 1)}-${dos(hoy.getDate())}`;
}

export const listarCargasAsistencia = () => llamarApi<CargaDocente[]>('/asistencia/cargas');
export const obtenerAsistenciaClase = (cargaId: number, fecha: string) =>
  llamarApi<AsistenciaClase>(`/asistencia/cargas/${cargaId}?fecha=${fecha}`);
export const guardarAsistenciaClase = (cargaId: number, fecha: string, horas: number, estudiantes: ItemAsistencia[]) =>
  llamarApi<AsistenciaClase>(`/asistencia/cargas/${cargaId}?fecha=${fecha}`, json('PUT', { horas, estudiantes }));

export const listarGruposAsistencia = () => llamarApi<GrupoAsistencia[]>('/asistencia/grupos');
export const obtenerResumenGrupo = (grupoId: number) =>
  llamarApi<ResumenGrupo>(`/asistencia/grupos/${grupoId}/resumen`);
export const listarNovedades = (matriculaId: number) =>
  llamarApi<NovedadAsistencia[]>(`/asistencia/matriculas/${matriculaId}/novedades`);
export const justificarFaltas = (matriculaId: number, fecha: string, justificacion: string) =>
  llamarApi<NovedadAsistencia[]>(
    `/asistencia/matriculas/${matriculaId}/justificacion`,
    json('POST', { fecha, justificacion }),
  );
