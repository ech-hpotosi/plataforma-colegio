import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
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
import { listarGruposAsistencia, obtenerResumenGrupo } from '../../api/asistencia';
import { useSesion } from '../../sesion/useSesion';
import { mensajeDeError } from '../academico/mensajes';
import DialogoNovedades from './DialogoNovedades';

/**
 * Consolidado del anio por grupo: horas sin justificar por asignatura y porcentaje frente a las horas del anio.
 * En rojo los estudiantes que pasan el maximo del SIEE.
 */
export default function ConsolidadoAsistencia() {
  const { tieneAlgunRol } = useSesion();
  const [grupoId, setGrupoId] = useState<number | ''>('');
  const [estudiante, setEstudiante] = useState<{ matriculaId: number; nombre: string } | null>(null);
  const grupos = useQuery({ queryKey: ['asistencia-grupos'], queryFn: listarGruposAsistencia });
  const resumen = useQuery({
    queryKey: ['asistencia-resumen', grupoId],
    queryFn: () => obtenerResumenGrupo(Number(grupoId)),
    enabled: grupoId !== '',
  });
  const puedeJustificar = tieneAlgunRol(['ADMINISTRADOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE']);

  useEffect(() => {
    if (grupos.data?.length === 1) setGrupoId(grupos.data[0].grupoId);
  }, [grupos.data]);

  if (grupos.isPending) return <CircularProgress />;
  if (grupos.isError) return <Alert severity="error">{mensajeDeError(grupos.error)}</Alert>;
  if (grupos.data.length === 0) {
    return <Alert severity="info">No tiene grupos a cargo. El consolidado lo ve el director de cada grupo.</Alert>;
  }

  return (
    <Stack spacing={2}>
      <TextField
        select
        label="Grupo"
        value={grupoId}
        onChange={(e) => setGrupoId(Number(e.target.value))}
        sx={{ maxWidth: 360 }}
      >
        {grupos.data.map((g) => (
          <MenuItem key={g.grupoId} value={g.grupoId}>
            {g.grupo} ({g.sede}, {g.anio})
          </MenuItem>
        ))}
      </TextField>

      {resumen.isFetching && <CircularProgress />}
      {resumen.isError && <Alert severity="error">{mensajeDeError(resumen.error)}</Alert>}
      {resumen.data && (
        <>
          <Typography variant="body2" color="text.secondary">
            Horas de inasistencia sin justificar en el anio y su porcentaje frente a las horas de la asignatura
            (intensidad semanal por {resumen.data.semanasLectivas} semanas). Segun el SIEE se pierde la asignatura
            al pasar el {resumen.data.porcentajeMaximo}%.
          </Typography>
          {resumen.data.estudiantes.length === 0 ? (
            <Alert severity="info">El grupo no tiene estudiantes matriculados.</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Estudiante</TableCell>
                    {resumen.data.asignaturas.map((a) => (
                      <TableCell key={a.asignaturaId} align="center">
                        {a.nombre}
                        <Typography variant="caption" display="block" color="text.secondary">
                          {a.horasAnuales} h/anio
                        </Typography>
                      </TableCell>
                    ))}
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {resumen.data.estudiantes.map((e) => (
                    <TableRow key={e.matriculaId}>
                      <TableCell sx={{ color: e.superaLimite ? 'error.main' : undefined }}>
                        {e.apellidos} {e.nombres}
                      </TableCell>
                      {resumen.data.asignaturas.map((a) => {
                        const conteo = e.asignaturas.find((x) => x.asignaturaId === a.asignaturaId);
                        return (
                          <TableCell
                            key={a.asignaturaId}
                            align="center"
                            sx={{ color: conteo?.superaLimite ? 'error.main' : undefined, fontWeight: conteo?.superaLimite ? 700 : undefined }}
                            title={
                              conteo
                                ? `Justificadas: ${conteo.horasJustificadas} h. Retardos: ${conteo.horasRetardo} h.`
                                : undefined
                            }
                          >
                            {conteo && conteo.horasSinJustificar > 0
                              ? `${conteo.horasSinJustificar} h (${conteo.porcentaje.toFixed(1)}%)`
                              : '-'}
                          </TableCell>
                        );
                      })}
                      <TableCell align="right">
                        <Button
                          size="small"
                          onClick={() => setEstudiante({ matriculaId: e.matriculaId, nombre: `${e.nombres} ${e.apellidos}` })}
                        >
                          Faltas
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </>
      )}
      {estudiante && grupoId !== '' && (
        <DialogoNovedades
          matriculaId={estudiante.matriculaId}
          nombre={estudiante.nombre}
          grupoId={grupoId}
          puedeJustificar={puedeJustificar}
          alCerrar={() => setEstudiante(null)}
        />
      )}
    </Stack>
  );
}
