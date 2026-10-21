import { descargarArchivo, llamarApi } from './cliente';
import type { NovedadAsistencia } from './asistencia';
import type { ConfiguracionEvaluacion, Desempeno, PeriodoNotas } from './notas';

export interface MatriculaFamilia {
  id: number;
  anio: number;
  grupo: string | null;
  sede: string | null;
}

export interface EstudianteFamilia {
  id: number;
  nombres: string;
  apellidos: string;
  /** Nulo cuando quien consulta es el propio estudiante. */
  parentesco: string | null;
  matriculas: MatriculaFamilia[];
}

export interface NotaPeriodoFamilia {
  periodoId: number;
  nota: number | null;
  desempeno: Desempeno | null;
  completa: boolean;
  /** Informe descriptivo del periodo (solo Transicion y periodos cerrados). */
  informe: string | null;
}

export interface AsignaturaFamilia {
  nombre: string;
  docente: string | null;
  periodos: NotaPeriodoFamilia[];
  definitiva: number | null;
  desempeno: Desempeno | null;
  completa: boolean;
  faltasJustificadas: number;
  faltasSinJustificar: number;
  retardos: number;
}

export interface ResumenFamilia {
  estudianteId: number;
  estudiante: string;
  matriculaId: number;
  anio: number;
  grupo: string | null;
  sede: string | null;
  director: string | null;
  escala: ConfiguracionEvaluacion | null;
  periodos: PeriodoNotas[];
  asignaturas: AsignaturaFamilia[];
  novedades: NovedadAsistencia[];
  boletinDisponible: boolean;
  /** Grado evaluado con informes descriptivos en lugar de notas (Transicion). */
  cualitativo: boolean;
}

export const listarEstudiantesFamilia = () => llamarApi<EstudianteFamilia[]>('/familia/estudiantes');

export const obtenerResumenFamilia = (estudianteId: number, matriculaId: number | null) =>
  llamarApi<ResumenFamilia>(
    `/familia/estudiantes/${estudianteId}/resumen` + (matriculaId ? `?matriculaId=${matriculaId}` : ''),
  );

export const descargarBoletinFamilia = (estudianteId: number, matriculaId: number, periodoId: number) =>
  descargarArchivo(
    `/familia/estudiantes/${estudianteId}/boletin?matriculaId=${matriculaId}&periodoId=${periodoId}`,
    'boletin.pdf',
  );
