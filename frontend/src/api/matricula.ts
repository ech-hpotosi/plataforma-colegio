import type { Pagina, TipoDocumento } from '../tipos';
import { llamarApi } from './cliente';
import type { Genero, Parentesco } from './estudiantes';

export type TipoSolicitud = 'NUEVO' | 'ANTIGUO';
export type EstadoSolicitud = 'BORRADOR' | 'ENVIADA' | 'CON_OBSERVACIONES' | 'APROBADA' | 'RECHAZADA';
export type EstadoDocumento = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';

export const NOMBRE_TIPO_SOLICITUD: Record<TipoSolicitud, string> = {
  NUEVO: 'Nuevo en el colegio',
  ANTIGUO: 'Ya estudiaba en el colegio',
};

export const NOMBRE_ESTADO_SOLICITUD: Record<EstadoSolicitud, string> = {
  BORRADOR: 'Sin enviar',
  ENVIADA: 'En revisión',
  CON_OBSERVACIONES: 'Con observaciones',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
};

export const NOMBRE_ESTADO_DOCUMENTO: Record<EstadoDocumento, string> = {
  PENDIENTE: 'Por revisar',
  APROBADO: 'Aprobado',
  RECHAZADO: 'Rechazado',
};

export interface Opcion {
  id: number;
  nombre: string;
}

export interface OfertaMatricula {
  anioLectivoId: number;
  anio: number;
  sedes: Opcion[];
  grados: Opcion[];
}

export interface TipoDocumentoMatricula {
  id: number;
  nombre: string;
  descripcion: string | null;
  obligatorio: boolean;
  activo: boolean;
  orden: number;
}

export type DatosTipoDocumento = Omit<TipoDocumentoMatricula, 'id'>;

export interface DocumentoSolicitud {
  tipoDocumentoId: number;
  nombre: string;
  descripcion: string | null;
  obligatorio: boolean;
  /** Nulo mientras no se haya subido el archivo. */
  id: number | null;
  nombreArchivo: string | null;
  tipoMime: string | null;
  tamanoBytes: number | null;
  fechaCarga: string | null;
  estado: EstadoDocumento | null;
  observacion: string | null;
}

export interface RevisionSolicitud {
  fecha: string;
  usuario: string;
  estadoAnterior: EstadoSolicitud;
  estadoNuevo: EstadoSolicitud;
  comentario: string | null;
}

export interface DatosSolicitud {
  anioLectivoId: number;
  sedeId: number;
  gradoId: number;
  tipo: TipoSolicitud;
  parentesco: Parentesco;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  genero: Genero;
  direccion: string;
  telefono: string;
  correo: string;
  eps: string;
  grupoSanguineo: string;
  condicionDiscapacidad: string;
  condicionesEspeciales: string;
  institucionAnterior: string;
}

export interface Solicitud {
  id: number;
  anioLectivoId: number;
  anio: number;
  sedeId: number;
  sede: string;
  gradoId: number;
  grado: string;
  tipo: TipoSolicitud;
  parentesco: Parentesco;
  estado: EstadoSolicitud;
  editable: boolean;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  genero: Genero;
  direccion: string | null;
  telefono: string | null;
  correo: string | null;
  eps: string | null;
  grupoSanguineo: string | null;
  condicionDiscapacidad: string | null;
  condicionesEspeciales: string | null;
  institucionAnterior: string | null;
  autorizaDatos: boolean;
  fechaCreacion: string;
  fechaEnvio: string | null;
  observacion: string | null;
  solicitante: {
    nombre: string;
    tipoDocumento: TipoDocumento;
    numeroDocumento: string;
    telefono: string | null;
    correo: string | null;
  };
  estudianteId: number | null;
  grupo: string | null;
  documentos: DocumentoSolicitud[];
  revisiones: RevisionSolicitud[];
}

export interface SolicitudResumen {
  id: number;
  anio: number;
  estudiante: string;
  numeroDocumento: string;
  sede: string;
  grado: string;
  tipo: TipoSolicitud;
  estado: EstadoSolicitud;
  fechaEnvio: string | null;
  solicitante: string;
  documentosPorRevisar: number;
}

const json = (metodo: string, cuerpo: unknown): RequestInit => ({ method: metodo, body: JSON.stringify(cuerpo) });
const BASE = '/solicitudes-matricula';

export const consultarOferta = () => llamarApi<OfertaMatricula[]>('/matricula-en-linea/oferta');
export const listarTiposDocumento = (soloActivos = true) =>
  llamarApi<TipoDocumentoMatricula[]>(`/tipos-documento-matricula?soloActivos=${soloActivos}`);
export const crearTipoDocumento = (datos: DatosTipoDocumento) =>
  llamarApi<TipoDocumentoMatricula>('/tipos-documento-matricula', json('POST', datos));
export const actualizarTipoDocumento = (id: number, datos: DatosTipoDocumento) =>
  llamarApi<TipoDocumentoMatricula>(`/tipos-documento-matricula/${id}`, json('PUT', datos));

export const listarMisSolicitudes = () => llamarApi<SolicitudResumen[]>(`${BASE}/mias`);
export const obtenerSolicitud = (id: number) => llamarApi<Solicitud>(`${BASE}/${id}`);
export const crearSolicitud = (datos: DatosSolicitud) => llamarApi<Solicitud>(BASE, json('POST', datos));
export const actualizarSolicitud = (id: number, datos: DatosSolicitud) =>
  llamarApi<Solicitud>(`${BASE}/${id}`, json('PUT', datos));
export const enviarSolicitud = (id: number) =>
  llamarApi<Solicitud>(`${BASE}/${id}/enviar`, json('POST', { autorizaDatos: true }));

export function subirDocumento(id: number, tipoId: number, archivo: File): Promise<Solicitud> {
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  return llamarApi<Solicitud>(`${BASE}/${id}/documentos/${tipoId}`, { method: 'POST', body: formulario });
}

export const quitarDocumento = (id: number, tipoId: number) =>
  llamarApi<Solicitud>(`${BASE}/${id}/documentos/${tipoId}`, { method: 'DELETE' });

/** Ruta para ver el archivo en otra pestana; la cookie de sesion viaja sola porque es el mismo sitio. */
export const urlArchivo = (id: number, documentoId: number) => `/api${BASE}/${id}/archivos/${documentoId}`;

export function buscarSolicitudes(
  anioId: number,
  estado: EstadoSolicitud | '',
  buscar: string,
  pagina: number,
  tamano: number,
): Promise<Pagina<SolicitudResumen>> {
  const parametros = new URLSearchParams({
    anioId: String(anioId),
    buscar,
    pagina: String(pagina),
    tamano: String(tamano),
  });
  if (estado) parametros.set('estado', estado);
  return llamarApi<Pagina<SolicitudResumen>>(`${BASE}?${parametros}`);
}

export const revisarDocumento = (id: number, documentoId: number, estado: EstadoDocumento, observacion: string) =>
  llamarApi<Solicitud>(`${BASE}/${id}/archivos/${documentoId}/revision`, json('PUT', { estado, observacion }));
export const devolverSolicitud = (id: number, comentario: string) =>
  llamarApi<Solicitud>(`${BASE}/${id}/devolver`, json('POST', { comentario }));
export const rechazarSolicitud = (id: number, comentario: string) =>
  llamarApi<Solicitud>(`${BASE}/${id}/rechazar`, json('POST', { comentario }));
export const aprobarSolicitud = (id: number, grupoId: number | null) =>
  llamarApi<Solicitud>(`${BASE}/${id}/aprobar`, json('POST', { grupoId }));

/** Tamano legible de un archivo. */
export function tamanoLegible(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
}
