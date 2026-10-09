import { llamarApi } from './cliente';

export type EstadoAnio = 'PLANEACION' | 'MATRICULA' | 'EN_CURSO' | 'CERRADO';

export const ESTADOS_ANIO: EstadoAnio[] = ['PLANEACION', 'MATRICULA', 'EN_CURSO', 'CERRADO'];

export const NOMBRE_ESTADO_ANIO: Record<EstadoAnio, string> = {
  PLANEACION: 'Planeación',
  MATRICULA: 'Matrícula',
  EN_CURSO: 'En curso',
  CERRADO: 'Cerrado',
};

export type Nivel = 'PREESCOLAR' | 'BASICA_PRIMARIA' | 'BASICA_SECUNDARIA' | 'MEDIA';

export const NOMBRE_NIVEL: Record<Nivel, string> = {
  PREESCOLAR: 'Preescolar',
  BASICA_PRIMARIA: 'Básica primaria',
  BASICA_SECUNDARIA: 'Básica secundaria',
  MEDIA: 'Media',
};

export type Jornada = 'MANANA' | 'TARDE' | 'UNICA';

export const NOMBRE_JORNADA: Record<Jornada, string> = {
  MANANA: 'Mañana',
  TARDE: 'Tarde',
  UNICA: 'Única',
};

export interface Sede {
  id: number;
  codigoDane: string | null;
  nombre: string;
  direccion: string | null;
  principal: boolean;
}

export interface DatosSede {
  codigoDane: string;
  nombre: string;
  direccion: string;
  principal: boolean;
}

export interface Periodo {
  id: number;
  numero: number;
  fechaInicio: string;
  fechaFin: string;
  porcentaje: number;
  cerrado: boolean;
}

export interface AnioLectivo {
  id: number;
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoAnio;
  periodos: Periodo[];
}

export interface DatosAnio {
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  periodos: { fechaInicio: string; fechaFin: string; porcentaje: number }[];
}

export interface Grado {
  id: number;
  nombre: string;
  nivel: Nivel;
  orden: number;
  evaluacionCualitativa: boolean;
}

export interface Asignatura {
  id: number;
  areaId: number;
  nombre: string;
}

export interface Area {
  id: number;
  nombre: string;
  asignaturas: Asignatura[];
}

export interface ItemPlan {
  id: number;
  asignaturaId: number;
  asignatura: string;
  area: string;
  intensidadHoraria: number;
}

export interface Docente {
  id: number;
  nombres: string;
  apellidos: string;
  numeroDocumento: string;
  especialidad: string | null;
  escalafon: string | null;
  sedeIds: number[];
}

export interface Grupo {
  id: number;
  anioLectivoId: number;
  sedeId: number;
  sede: string;
  gradoId: number;
  grado: string;
  nombre: string;
  jornada: Jornada;
  cupo: number;
  directorId: number | null;
  director: string | null;
}

export interface DatosGrupo {
  anioLectivoId: number;
  sedeId: number;
  gradoId: number;
  nombre: string;
  jornada: Jornada;
  cupo: number;
  directorId: number | null;
}

export interface ItemCarga {
  asignaturaId: number;
  asignatura: string;
  area: string;
  intensidadHoraria: number;
  docenteId: number | null;
  docente: string | null;
}

const json = (metodo: string, cuerpo: unknown): RequestInit => ({ method: metodo, body: JSON.stringify(cuerpo) });

export const listarSedes = () => llamarApi<Sede[]>('/sedes');
export const crearSede = (datos: DatosSede) => llamarApi<Sede>('/sedes', json('POST', datos));
export const actualizarSede = (id: number, datos: DatosSede) => llamarApi<Sede>(`/sedes/${id}`, json('PUT', datos));

export const listarAnios = () => llamarApi<AnioLectivo[]>('/anios');
export const crearAnio = (datos: DatosAnio) => llamarApi<AnioLectivo>('/anios', json('POST', datos));
export const actualizarAnio = (id: number, datos: DatosAnio) =>
  llamarApi<AnioLectivo>(`/anios/${id}`, json('PUT', datos));
export const cambiarEstadoAnio = (id: number, estado: EstadoAnio) =>
  llamarApi<AnioLectivo>(`/anios/${id}/estado`, json('PUT', { estado }));

export const listarGrados = () => llamarApi<Grado[]>('/grados');
export const actualizarGrado = (id: number, evaluacionCualitativa: boolean) =>
  llamarApi<Grado>(`/grados/${id}`, json('PUT', { evaluacionCualitativa }));

export const listarAreas = () => llamarApi<Area[]>('/areas');
export const crearArea = (nombre: string) => llamarApi<Area>('/areas', json('POST', { nombre }));
export const actualizarArea = (id: number, nombre: string) => llamarApi<Area>(`/areas/${id}`, json('PUT', { nombre }));
export const crearAsignatura = (areaId: number, nombre: string) =>
  llamarApi<Asignatura>('/asignaturas', json('POST', { areaId, nombre }));
export const actualizarAsignatura = (id: number, areaId: number, nombre: string) =>
  llamarApi<Asignatura>(`/asignaturas/${id}`, json('PUT', { areaId, nombre }));

export const listarPlan = (anioId: number, gradoId: number) =>
  llamarApi<ItemPlan[]>(`/plan-estudio?anioId=${anioId}&gradoId=${gradoId}`);
export const guardarPlan = (
  anioId: number,
  gradoId: number,
  asignaturas: { asignaturaId: number; intensidadHoraria: number }[],
) => llamarApi<ItemPlan[]>(`/plan-estudio?anioId=${anioId}&gradoId=${gradoId}`, json('PUT', { asignaturas }));

export const listarDocentes = () => llamarApi<Docente[]>('/docentes');
export const actualizarDocente = (
  id: number,
  datos: { especialidad: string; escalafon: string; sedeIds: number[] },
) => llamarApi<Docente>(`/docentes/${id}`, json('PUT', datos));

export const listarGrupos = (anioId: number, sedeId: number | null) =>
  llamarApi<Grupo[]>(`/grupos?anioId=${anioId}` + (sedeId ? `&sedeId=${sedeId}` : ''));
export const crearGrupo = (datos: DatosGrupo) => llamarApi<Grupo>('/grupos', json('POST', datos));
export const actualizarGrupo = (id: number, datos: DatosGrupo) =>
  llamarApi<Grupo>(`/grupos/${id}`, json('PUT', datos));
export const listarCarga = (grupoId: number) => llamarApi<ItemCarga[]>(`/grupos/${grupoId}/carga`);
export const guardarCarga = (grupoId: number, asignaciones: { asignaturaId: number; docenteId: number }[]) =>
  llamarApi<ItemCarga[]>(`/grupos/${grupoId}/carga`, json('PUT', { asignaciones }));

/** Anio que se muestra por defecto: el que esta en curso, o el mas reciente. */
export function anioPorDefecto(anios: AnioLectivo[]): AnioLectivo | undefined {
  return anios.find((a) => a.estado === 'EN_CURSO') ?? anios[0];
}
