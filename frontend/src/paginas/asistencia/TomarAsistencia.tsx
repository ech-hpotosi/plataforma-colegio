import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  MenuItem,
  Paper,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import {
  guardarAsistenciaClase,
  hoyIso,
  listarCargasAsistencia,
  obtenerAsistenciaClase,
  type EstadoAsistencia,
  type EstudianteAsistencia,
} from '../../api/asistencia';
import { mensajeDeError } from '../academico/mensajes';

const OPCIONES: { valor: EstadoAsistencia; texto: string }[] = [
  { valor: 'ASISTIO', texto: 'Asistio' },
  { valor: 'FALTA', texto: 'Falta' },
  { valor: 'RETARDO', texto: 'Retardo' },
  { valor: 'PERMISO', texto: 'Permiso' },
];

/**
 * Toma de asistencia de una clase. Por defecto todos asistieron; el docente marca solo las novedades.
 * Una falta ya justificada se muestra como falta con la marca "Justificada" y se conserva al guardar.
 */
export default function TomarAsistencia() {
  const queryClient = useQueryClient();
  const [cargaId, setCargaId] = useState<number | ''>('');
  const [fecha, setFecha] = useState(hoyIso());
  const [horas, setHoras] = useState(1);
  const [filas, setFilas] = useState<EstudianteAsistencia[]>([]);
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);

  const cargas = useQuery({ queryKey: ['asistencia-cargas'], queryFn: listarCargasAsistencia });
  const clase = useQuery({
    queryKey: ['asistencia-clase', cargaId, fecha],
    queryFn: () => obtenerAsistenciaClase(Number(cargaId), fecha),
    enabled: cargaId !== '' && fecha !== '',
  });

  useEffect(() => {
    if (clase.data) {
      setFilas(clase.data.estudiantes);
      setHoras(clase.data.horas);
    }
  }, [clase.data]);

  // Si el docente tiene una sola clase, se selecciona sola
  useEffect(() => {
    if (cargas.data?.length === 1) setCargaId(cargas.data[0].cargaId);
  }, [cargas.data]);

  const cambiarEstado = (matriculaId: number, nuevo: EstadoAsistencia | null) => {
    if (!nuevo) return;
    setFilas((actuales) =>
      actuales.map((f) => {
        if (f.matriculaId !== matriculaId) return f;
        const sigueJustificada = f.estado === 'FALTA_JUSTIFICADA' && nuevo === 'FALTA';
        return { ...f, estado: sigueJustificada ? f.estado : nuevo };
      }),
    );
  };

  const cambiarObservacion = (matriculaId: number, observacion: string) =>
    setFilas((actuales) => actuales.map((f) => (f.matriculaId === matriculaId ? { ...f, observacion } : f)));

  const guardar = async () => {
    if (cargaId === '') return;
    setMensaje(null);
    setGuardando(true);
    try {
      const guardada = await guardarAsistenciaClase(
        cargaId,
        fecha,
        horas,
        filas.map((f) => ({
          matriculaId: f.matriculaId,
          estado: f.estado,
          observacion: f.estado === 'ASISTIO' ? null : f.observacion,
        })),
      );
      queryClient.setQueryData(['asistencia-clase', cargaId, fecha], guardada);
      setMensaje({ tipo: 'success', texto: 'Asistencia guardada' });
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  const faltas = filas.filter((f) => f.estado === 'FALTA' || f.estado === 'FALTA_JUSTIFICADA').length;
  const retardos = filas.filter((f) => f.estado === 'RETARDO').length;

  if (cargas.isPending) return <CircularProgress />;
  if (cargas.isError) return <Alert severity="error">{mensajeDeError(cargas.error)}</Alert>;
  if (cargas.data.length === 0) {
    return <Alert severity="info">No tiene grupos ni asignaturas asignadas en la carga academica.</Alert>;
  }

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          select
          label="Clase"
          value={cargaId}
          onChange={(e) => {
            setCargaId(Number(e.target.value));
            setMensaje(null);
          }}
          sx={{ minWidth: 280 }}
        >
          {cargas.data.map((c) => (
            <MenuItem key={c.cargaId} value={c.cargaId}>
              {c.grupo} - {c.asignatura} ({c.sede}, {c.anio})
            </MenuItem>
          ))}
        </TextField>
        <TextField
          label="Fecha"
          type="date"
          value={fecha}
          onChange={(e) => {
            setFecha(e.target.value);
            setMensaje(null);
          }}
          slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoyIso() } }}
        />
        <TextField
          label="Horas de clase"
          type="number"
          value={horas}
          onChange={(e) => setHoras(Math.max(1, Math.min(10, Number(e.target.value))))}
          slotProps={{ htmlInput: { min: 1, max: 10 } }}
          sx={{ width: 140 }}
        />
      </Stack>

      {mensaje && <Alert severity={mensaje.tipo}>{mensaje.texto}</Alert>}
      {cargaId === '' && <Alert severity="info">Seleccione la clase para ver la lista de estudiantes.</Alert>}
      {clase.isFetching && <CircularProgress />}
      {clase.isError && <Alert severity="error">{mensajeDeError(clase.error)}</Alert>}

      {clase.data && !clase.isError && (
        <>
          <Typography variant="body2" color="text.secondary">
            Periodo {clase.data.periodo}. {clase.data.registrada ? 'Asistencia ya registrada; puede corregirla.' : 'Aun no se ha tomado asistencia.'}{' '}
            Estudiantes: {filas.length}. Faltas: {faltas}. Retardos: {retardos}.
          </Typography>
          {filas.length === 0 && <Alert severity="info">El grupo no tiene estudiantes matriculados.</Alert>}
          {filas.map((f, i) => (
            <Paper key={f.matriculaId} variant="outlined" sx={{ p: 1.5 }}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} sx={{ alignItems: { md: 'center' } }}>
                <Box sx={{ flex: 1 }}>
                  <Typography>
                    {i + 1}. {f.apellidos} {f.nombres}
                  </Typography>
                  {f.estado === 'FALTA_JUSTIFICADA' && (
                    <Chip size="small" color="success" label={'Justificada: ' + (f.justificacion ?? '')} sx={{ mt: 0.5 }} />
                  )}
                </Box>
                <ToggleButtonGroup
                  size="small"
                  exclusive
                  value={f.estado === 'FALTA_JUSTIFICADA' ? 'FALTA' : f.estado}
                  onChange={(_, valor) => cambiarEstado(f.matriculaId, valor)}
                  aria-label={`Asistencia de ${f.nombres} ${f.apellidos}`}
                >
                  {OPCIONES.map((o) => (
                    <ToggleButton key={o.valor} value={o.valor} color={o.valor === 'ASISTIO' ? 'primary' : 'warning'}>
                      {o.texto}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
                {f.estado !== 'ASISTIO' && (
                  <TextField
                    size="small"
                    label="Observacion"
                    value={f.observacion ?? ''}
                    onChange={(e) => cambiarObservacion(f.matriculaId, e.target.value)}
                    slotProps={{ htmlInput: { maxLength: 300 } }}
                    sx={{ minWidth: 220 }}
                  />
                )}
              </Stack>
            </Paper>
          ))}
          {filas.length > 0 && (
            <Box>
              <Button variant="contained" onClick={guardar} disabled={guardando}>
                Guardar asistencia
              </Button>
            </Box>
          )}
        </>
      )}
    </Stack>
  );
}
