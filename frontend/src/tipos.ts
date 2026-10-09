export type Rol =
  | 'ADMINISTRADOR'
  | 'RECTOR'
  | 'COORDINADOR_ACADEMICO'
  | 'SECRETARIA'
  | 'DOCENTE'
  | 'ESTUDIANTE'
  | 'ACUDIENTE';

export const ROLES: Rol[] = [
  'ADMINISTRADOR',
  'RECTOR',
  'COORDINADOR_ACADEMICO',
  'SECRETARIA',
  'DOCENTE',
  'ESTUDIANTE',
  'ACUDIENTE',
];

export const NOMBRE_ROL: Record<Rol, string> = {
  ADMINISTRADOR: 'Administrador',
  RECTOR: 'Rector',
  COORDINADOR_ACADEMICO: 'Coordinador académico',
  SECRETARIA: 'Secretaria',
  DOCENTE: 'Docente',
  ESTUDIANTE: 'Estudiante',
  ACUDIENTE: 'Acudiente',
};

export type TipoDocumento = 'RC' | 'TI' | 'CC' | 'CE' | 'PPT';

export const NOMBRE_TIPO_DOCUMENTO: Record<TipoDocumento, string> = {
  RC: 'Registro civil',
  TI: 'Tarjeta de identidad',
  CC: 'Cédula de ciudadanía',
  CE: 'Cédula de extranjería',
  PPT: 'Permiso por protección temporal',
};

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}
