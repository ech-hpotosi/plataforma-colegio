import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import {
  Alert,
  Button,
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
import { listarGruposAsistencia } from '../../api/asistencia';
import { NOMBRE_DESEMPENO, descargarBoletines, obtenerConsolidadoNotas } from '../../api/notas';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';

/**
 * Notas de un grupo por asignatura, de un periodo o acumuladas del anio.
 * Resalta las notas en desempeno Bajo y cuenta cuantas asignaturas lleva perdidas cada estudiante.
 */
export default function ConsolidadoNotas() {
  const [parametros] = useSearchParams();
  const [grupoId, setGrupoId] = useState<number | ''>(parametros.get('grupo') ? Number(parametros.get('grupo')) : '');
  // '' es el acumulado del anio
  const [periodoId, setPeriodoId] = useState<number | ''>('');
  const [descargando, setDescargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const grupos = useQuery({ queryKey: ['asistencia-grupos'], queryFn: listarGruposAsistencia });
  const consolidado = useQuery({
    queryKey: ['notas-consolidado', grupoId, periodoId],
    queryFn: () => obtenerConsolidadoNotas(Number(grupoId), periodoId === '' ? null : periodoId),
    enabled: grupoId !== '',
  });

  useEffect(() => {
    if (grupos.data?.length === 1) setGrupoId(grupos.data[0].grupoId);
  }, [grupos.data]);

  if (grupos.isPending) return <CircularProgress />;
  if (grupos.isError) return <Alert severity="error">{mensajeDeError(grupos.error)}</Alert>;
  if (grupos.data.length === 0) {
    return <Alert severity="info">No tiene grupos a cargo. El consolidado lo ve el director de cada grupo.</Alert>;
  }

  const datos = consolidado.data;

  const boletines = async (matriculaId?: number) => {
    if (grupoId === '' || periodoId === '') return;
    setDescargando(true);
    setError(null);
    try {
      await descargarBoletines(grupoId, periodoId, matriculaId);
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setDescargando(false);
    }
  };
  const conBajo = datos?.estudiantes.filter((e) => e.asignaturasEnBajo > 0).length ?? 0;

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          select
          label="Grupo"
          value={grupoId}
          onChange={(e) => {
            setGrupoId(Number(e.target.value));
            setPeriodoId('');
          }}
          sx={{ minWidth: 280 }}
        >
          {grupos.data.map((g) => (
            <MenuItem key={g.grupoId} value={g.grupoId}>
              {g.grupo} ({g.sede}, {g.anio})
            </MenuItem>
          ))}
        </TextField>
        {datos && (
          <TextField
            select
            label="Periodo"
            value={periodoId}
            onChange={(e) => setPeriodoId(e.target.value === '' ? '' : Number(e.target.value))}
            slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="">Año completo</MenuItem>
            {datos.periodos.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                Periodo {p.numero} ({p.porcentaje} %)
              </MenuItem>
            ))}
          </TextField>
        )}
        {datos && periodoId !== '' && datos.estudiantes.length > 0 && (
          <Button
            variant="contained"
            onClick={() => boletines()}
            disabled={descargando}
            sx={{ ml: { sm: 'auto' }, alignSelf: { sm: 'center' } }}
          >
            Descargar boletines (PDF)
          </Button>
        )}
      </Stack>
      {error && <Alert severity="error">{error}</Alert>}

      {consolidado.isFetching && !datos && <CircularProgress />}
      {consolidado.isError && <Alert severity="error">{mensajeDeError(consolidado.error)}</Alert>}
      {datos && (
        <>
          <Typography variant="body2" color="text.secondary">
            {periodoId === ''
              ? 'Nota acumulada del año según el porcentaje de cada periodo.'
              : 'Nota del periodo según los pesos de Saber, Hacer y Ser.'}{' '}
            En rojo las notas con desempeño Bajo (menos de {datos.configuracion.notaAprobatoria.toFixed(1)}).{' '}
            {conBajo > 0
              ? `${conBajo} ${conBajo === 1 ? 'estudiante tiene' : 'estudiantes tienen'} alguna asignatura en Bajo.`
              : 'Ningún estudiante tiene asignaturas en Bajo.'}
          </Typography>
          {datos.estudiantes.length === 0 ? (
            <Alert severity="info">El grupo no tiene estudiantes matriculados.</Alert>
          ) : datos.asignaturas.length === 0 ? (
            <Alert severity="info">El grupo no tiene carga académica asignada.</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table size="small" sx={{ '& td, & th': { whiteSpace: 'nowrap' } }}>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ position: 'sticky', left: 0, zIndex: 3, bgcolor: COLORES.encabezado }}>Estudiante</TableCell>
                    {datos.asignaturas.map((a) => (
                      <TableCell key={a.cargaId} align="center" title={a.docente}>
                        {a.nombre}
                      </TableCell>
                    ))}
                    <TableCell align="center">En Bajo</TableCell>
                    {periodoId !== '' && <TableCell align="center">Boletín</TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {datos.estudiantes.map((e, i) => (
                    <TableRow key={e.matriculaId}>
                      <TableCell sx={{ position: 'sticky', left: 0, zIndex: 1, bgcolor: '#fff' }}>
                        {i + 1}. {e.apellidos} {e.nombres}
                      </TableCell>
                      {e.notas.map((n) => (
                        <TableCell
                          key={n.cargaId}
                          align="center"
                          title={
                            n.desempeno
                              ? `${NOMBRE_DESEMPENO[n.desempeno]}${n.completa ? '' : '. Nota parcial'}`
                              : 'Sin notas'
                          }
                          sx={{
                            color: n.desempeno === 'BAJO' ? 'error.main' : undefined,
                            fontWeight: n.desempeno === 'BAJO' ? 700 : undefined,
                          }}
                        >
                          {n.nota === null ? '-' : `${n.nota.toFixed(1)}${n.completa ? '' : '*'}`}
                        </TableCell>
                      ))}
                      <TableCell
                        align="center"
                        sx={{ color: e.asignaturasEnBajo > 0 ? 'error.main' : 'text.secondary', fontWeight: 700 }}
                      >
                        {e.asignaturasEnBajo}
                      </TableCell>
                      {periodoId !== '' && (
                        <TableCell align="center" sx={{ py: 0 }}>
                          <Button
                            size="small"
                            onClick={() => boletines(e.matriculaId)}
                            disabled={descargando}
                            aria-label={`Boletín de ${e.nombres} ${e.apellidos}`}
                          >
                            PDF
                          </Button>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <Typography variant="caption" color="text.secondary">
            * Nota parcial: falta alguna dimensión{periodoId === '' ? ' o algún periodo' : ''}. Pase el mouse sobre la nota
            para ver el desempeño; el nombre del docente aparece sobre la asignatura.
          </Typography>
        </>
      )}
    </Stack>
  );
}
