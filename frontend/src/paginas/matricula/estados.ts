import type { Tono } from '../../componentes/Estado';
import type { EstadoDocumento, EstadoSolicitud } from '../../api/matricula';

export const TONO_SOLICITUD: Record<EstadoSolicitud, Tono> = {
  BORRADOR: 'neutro',
  ENVIADA: 'informativo',
  CON_OBSERVACIONES: 'alerta',
  APROBADA: 'positivo',
  RECHAZADA: 'negativo',
};

export const TONO_DOCUMENTO: Record<EstadoDocumento, Tono> = {
  PENDIENTE: 'informativo',
  APROBADO: 'positivo',
  RECHAZADO: 'negativo',
};

const FECHA_HORA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });

/** Fecha y hora cortas, a partir del formato ISO local que envia el servidor. */
export function fechaHora(iso: string | null): string {
  return iso ? FECHA_HORA.format(new Date(iso)) : '';
}

/** Fecha ISO (aaaa-mm-dd) como dd/mm/aaaa. */
export function fechaCorta(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
}
