import type { Pagina, TipoDocumento } from '../tipos';
import { llamarApi } from './cliente';

export type EstadoEstudiante = 'ASPIRANTE' | 'ACTIVO' | 'RETIRADO' | 'GRADUADO';

export const NOMBRE_ESTADO_ESTUDIANTE: Record<EstadoEstudiante, string> = {
  ASPIRANTE: 'Aspirante',
  ACTIVO: 'Activo',
  RETIRADO: 'Retirado',
  GRADUADO: 'Graduado',
};

export type Genero = 'FEMENINO' | 'MASCULINO';

export const NOMBRE_GENERO: Record<Genero, string> = { FEMENINO: 'Femenino', MASCULINO: 'Masculino' };

export type Parentesco = 'MADRE' | 'PADRE' | 'ABUELO' | 'TIO' | 'HERMANO' | 'TUTOR' | 'OTRO';

export const NOMBRE_PARENTESCO: Record<Parentesco, string> = {
  MADRE: 'Madre',
  PADRE: 'Padre',
  ABUELO: 'Abuelo(a)',
  TIO: 'Tio(a)',
  HERMANO: 'Hermano(a)',
  TUTOR: 'Tutor legal',
  OTRO: 'Otro',
};

export interface EstudianteResumen {
  id: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  codigo: string | null;
  estado: EstadoEstudiante;
}

export interface AcudienteVinculado {
  id: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string | null;
  correo: string | null;
  ocupacion: string | null;
  parentesco: Parentesco;
  principal: boolean;
}

export interface Estudiante {
  id: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string | null;
  correo: string | null;
  codigo: string | null;
  fechaNacimiento: string;
  genero: Genero;
  direccion: string | null;
  eps: string | null;
  grupoSanguineo: string | null;
  condicionDiscapacidad: string | null;
  tienePiar: boolean;
  condicionesEspeciales: string | null;
  estado: EstadoEstudiante;
  acudientes: AcudienteVinculado[];
}

export interface DatosEstudiante {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  codigo: string;
  fechaNacimiento: string;
  genero: Genero;
  direccion: string;
  eps: string;
  grupoSanguineo: string;
  condicionDiscapacidad: string;
  tienePiar: boolean;
  condicionesEspeciales: string;
}

export interface DatosAcudiente {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string;
  correo: string;
  ocupacion: string;
  parentesco: Parentesco;
  principal: boolean;
}

export interface PersonaEncontrada {
  id: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  telefono: string | null;
  correo: string | null;
  ocupacion: string | null;
}

export interface Matricula {
  id: number;
  estudianteId: number;
  anioLectivoId: number;
  anio: number;
  grupoId: number | null;
  grupo: string | null;
  sede: string | null;
  fechaMatricula: string;
  estado: 'ACTIVA' | 'RETIRADA';
  fechaRetiro: string | null;
  motivoRetiro: string | null;
}

export interface EstudianteDeGrupo {
  matriculaId: number;
  estudianteId: number;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
}

const json = (metodo: string, cuerpo: unknown): RequestInit => ({ method: metodo, body: JSON.stringify(cuerpo) });

export function buscarEstudiantes(
  buscar: string,
  estado: EstadoEstudiante | '',
  pagina: number,
  tamano: number,
): Promise<Pagina<EstudianteResumen>> {
  const parametros = new URLSearchParams({ buscar, pagina: String(pagina), tamano: String(tamano) });
  if (estado) parametros.set('estado', estado);
  return llamarApi<Pagina<EstudianteResumen>>('/estudiantes?' + parametros.toString());
}

export const obtenerEstudiante = (id: number) => llamarApi<Estudiante>(`/estudiantes/${id}`);
export const crearEstudiante = (datos: DatosEstudiante) =>
  llamarApi<Estudiante>('/estudiantes', json('POST', datos));
export const actualizarEstudiante = (id: number, datos: DatosEstudiante) =>
  llamarApi<Estudiante>(`/estudiantes/${id}`, json('PUT', datos));

export const vincularAcudiente = (estudianteId: number, datos: DatosAcudiente) =>
  llamarApi<Estudiante>(`/estudiantes/${estudianteId}/acudientes`, json('POST', datos));
export const actualizarAcudiente = (estudianteId: number, acudienteId: number, datos: DatosAcudiente) =>
  llamarApi<Estudiante>(`/estudiantes/${estudianteId}/acudientes/${acudienteId}`, json('PUT', datos));
export const desvincularAcudiente = (estudianteId: number, acudienteId: number) =>
  llamarApi<Estudiante>(`/estudiantes/${estudianteId}/acudientes/${acudienteId}`, { method: 'DELETE' });
export const buscarPersonaPorDocumento = (tipo: TipoDocumento, numero: string) =>
  llamarApi<PersonaEncontrada>(`/personas/por-documento?tipo=${tipo}&numero=${encodeURIComponent(numero)}`);

export const listarMatriculas = (estudianteId: number) =>
  llamarApi<Matricula[]>(`/estudiantes/${estudianteId}/matriculas`);
export const matricular = (estudianteId: number, anioLectivoId: number, grupoId: number | null) =>
  llamarApi<Matricula>('/matriculas', json('POST', { estudianteId, anioLectivoId, grupoId }));
export const cambiarGrupo = (matriculaId: number, grupoId: number | null) =>
  llamarApi<Matricula>(`/matriculas/${matriculaId}/grupo`, json('PUT', { grupoId }));
export const retirarMatricula = (matriculaId: number, motivo: string) =>
  llamarApi<Matricula>(`/matriculas/${matriculaId}/retirar`, json('POST', { motivo }));
export const listarEstudiantesDeGrupo = (grupoId: number) =>
  llamarApi<EstudianteDeGrupo[]>(`/grupos/${grupoId}/estudiantes`);
