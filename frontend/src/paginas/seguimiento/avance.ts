import { NOMBRE_DIMENSION } from '../../api/notas';
import type { AvanceClase } from '../../api/seguimiento';
import type { Tono } from '../../componentes/Estado';

/** Dias sin registrar asistencia a partir de los cuales la clase se marca como atrasada. */
export const LIMITE_DIAS_ASISTENCIA = 7;

const FECHA_CORTA = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', month: 'short' });

/** Fecha ISO (aaaa-mm-dd) en la hora local, sin que el cambio a UTC la corra un dia. */
export function fechaCorta(iso: string) {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FECHA_CORTA.format(new Date(anio, mes - 1, dia));
}

export const notasCompletas = (c: AvanceClase) => c.cualitativa || c.porcentajeNotas === 100;
export const asistenciaAtrasada = (c: AvanceClase) => c.diasSinAsistencia > LIMITE_DIAS_ASISTENCIA;

/** Texto corto del avance de notas: cuantas van y que actividades faltan por crear. */
export function textoNotas(c: AvanceClase) {
  if (c.cualitativa) return 'Evaluación cualitativa, sin planilla numérica.';
  const faltan = c.dimensionesSinActividad.map((d) => NOMBRE_DIMENSION[d]);
  const sinActividades = faltan.length > 0 ? ` Falta crear actividades de ${unir(faltan)}.` : '';
  if (c.actividades === 0) return `Sin actividades todavía.${sinActividades}`;
  return `${c.notasRegistradas} de ${c.notasEsperadas} notas.${sinActividades}`;
}

export function estadoAsistencia(c: AvanceClase): { tono: Tono; texto: string } {
  if (c.diasConAsistencia === 0) {
    return asistenciaAtrasada(c)
      ? { tono: 'negativo', texto: `Sin registros en ${c.diasSinAsistencia} días` }
      : { tono: 'neutro', texto: 'Sin registros aún' };
  }
  if (asistenciaAtrasada(c)) {
    return { tono: 'alerta', texto: `Última hace ${c.diasSinAsistencia} días` };
  }
  return { tono: 'positivo', texto: `Al día, última el ${fechaCorta(c.ultimaAsistencia!)}` };
}

/** "Saber", "Saber y Ser", "Saber, Hacer y Ser". */
function unir(partes: string[]) {
  return partes.length <= 1 ? partes.join('') : `${partes.slice(0, -1).join(', ')} y ${partes[partes.length - 1]}`;
}
