import Estado, { type Tono } from '../../componentes/Estado';
import { NOMBRE_DESEMPENO, type Desempeno } from '../../api/notas';

const TONO: Record<Desempeno, Tono> = { BAJO: 'negativo', BASICO: 'neutro', ALTO: 'informativo', SUPERIOR: 'positivo' };

/** Desempeno de la escala nacional con su punto de color. */
export default function EstadoDesempeno({ desempeno }: { desempeno: Desempeno | null }) {
  if (!desempeno) return <>-</>;
  return <Estado tono={TONO[desempeno]} texto={NOMBRE_DESEMPENO[desempeno]} />;
}
