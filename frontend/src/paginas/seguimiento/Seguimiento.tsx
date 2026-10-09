import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  CircularProgress,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { obtenerAvance, type AvanceClase } from '../../api/seguimiento';
import BarraAvance from '../../componentes/BarraAvance';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { mensajeDeError } from '../academico/mensajes';
import {
  LIMITE_DIAS_ASISTENCIA,
  asistenciaAtrasada,
  estadoAsistencia,
  fechaCorta,
  notasCompletas,
  textoNotas,
} from './avance';

type Mostrar = 'todas' | 'pendientes';

// El SIEE define tres periodos (art. 6)
const PERIODOS = [1, 2, 3];

/** Las clases mas atrasadas primero: menos notas y, a igual avance, mas dias sin asistencia. */
const ordenar = (a: AvanceClase, b: AvanceClase) =>
  Number(notasCompletas(a)) - Number(notasCompletas(b)) ||
  a.porcentajeNotas - b.porcentajeNotas ||
  b.diasSinAsistencia - a.diasSinAsistencia ||
  (a.docente ?? '').localeCompare(b.docente ?? '');

/**
 * Seguimiento del registro para directivos y secretaria: cuanto lleva cada clase de notas y asistencia,
 * para saber a quien recordarle antes del cierre del periodo.
 */
export default function Seguimiento() {
  const [periodo, setPeriodo] = useState<number | ''>('');
  const [sede, setSede] = useState('');
  const [docente, setDocente] = useState('');
  const [mostrar, setMostrar] = useState<Mostrar>('pendientes');
  const avance = useQuery({
    queryKey: ['seguimiento-avance', periodo],
    queryFn: () => obtenerAvance(periodo === '' ? undefined : periodo),
  });

  const clases = avance.data?.clases ?? [];
  const sedes = useMemo(() => [...new Set(clases.map((c) => c.sede))].sort(), [clases]);
  const docentes = useMemo(
    () => [...new Set(clases.map((c) => c.docente ?? 'Sin docente'))].sort((a, b) => a.localeCompare(b)),
    [clases],
  );
  const delFiltro = clases.filter(
    (c) => (sede === '' || c.sede === sede) && (docente === '' || (c.docente ?? 'Sin docente') === docente),
  );
  const visibles = delFiltro
    .filter((c) => mostrar === 'todas' || !notasCompletas(c) || asistenciaAtrasada(c))
    .sort(ordenar);
  const completas = delFiltro.filter(notasCompletas).length;
  const atrasadas = delFiltro.filter(asistenciaAtrasada).length;
  const periodos = [...new Set(clases.map((c) => c.periodoNumero))];
  const unPeriodo = periodos.length === 1 ? clases[0] : undefined;

  return (
    <>
      <Encabezado
        titulo="Seguimiento del registro"
        descripcion="Cuánto lleva cada clase de notas y asistencia en el periodo, para saber a quién recordarle antes del cierre."
      />
      <Stack spacing={2}>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
          <TextField
            select
            label="Periodo"
            value={periodo}
            onChange={(e) => setPeriodo(e.target.value === '' ? '' : Number(e.target.value))}
            slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="">En curso</MenuItem>
            {PERIODOS.map((n) => (
              <MenuItem key={n} value={n}>
                Periodo {n}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Sede"
            value={sede}
            onChange={(e) => setSede(e.target.value)}
            slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">Todas las sedes</MenuItem>
            {sedes.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Docente"
            value={docente}
            onChange={(e) => setDocente(e.target.value)}
            slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">Todos los docentes</MenuItem>
            {docentes.map((d) => (
              <MenuItem key={d} value={d}>
                {d}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Mostrar"
            value={mostrar}
            onChange={(e) => setMostrar(e.target.value as Mostrar)}
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="pendientes">Solo con pendientes</MenuItem>
            <MenuItem value="todas">Todas las clases</MenuItem>
          </TextField>
        </Stack>

        {avance.isPending && <CircularProgress />}
        {avance.isError && <Alert severity="error">{mensajeDeError(avance.error)}</Alert>}
        {avance.isSuccess && clases.length === 0 && (
          <Alert severity="info">No hay clases en un periodo que ya haya empezado.</Alert>
        )}
        {avance.isSuccess && clases.length > 0 && (
          <>
            <Typography variant="body2" color="text.secondary">
              {unPeriodo &&
                `Periodo ${unPeriodo.periodoNumero}, del ${fechaCorta(unPeriodo.inicioPeriodo)} al ${fechaCorta(unPeriodo.cierrePeriodo)}${unPeriodo.periodoCerrado ? ' (cerrado)' : ''}. `}
              Notas completas en {completas} de {delFiltro.length} clases. Asistencia atrasada en {atrasadas}{' '}
              {atrasadas === 1 ? 'clase' : 'clases'} (más de {LIMITE_DIAS_ASISTENCIA} días sin registrar).
            </Typography>
            {visibles.length === 0 ? (
              <Alert severity="success">Todas las clases del filtro tienen las notas completas y la asistencia al día.</Alert>
            ) : (
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Docente</TableCell>
                      <TableCell>Clase</TableCell>
                      <TableCell sx={{ minWidth: 260 }}>Notas</TableCell>
                      <TableCell>Asistencia</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {visibles.map((c) => {
                      const asistencia = estadoAsistencia(c);
                      return (
                        <TableRow key={`${c.cargaId}-${c.periodoId}`}>
                          <TableCell>{c.docente ?? 'Sin docente'}</TableCell>
                          <TableCell>
                            {c.grupo} - {c.asignatura}
                            <Typography variant="body2" color="text.secondary">
                              {c.sede}
                              {periodos.length > 1 && `, periodo ${c.periodoNumero}`}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            {!c.cualitativa && (
                              <BarraAvance porcentaje={c.porcentajeNotas} etiqueta={`Notas de ${c.grupo} - ${c.asignatura}`} />
                            )}
                            <Typography variant="body2" color="text.secondary">
                              {textoNotas(c)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Estado tono={asistencia.tono} texto={asistencia.texto} />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </>
        )}
      </Stack>
    </>
  );
}
