import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { justificarFaltas, listarNovedades, NOMBRE_ESTADO_ASISTENCIA, type EstadoAsistencia } from '../../api/asistencia';
import { mensajeDeError } from '../academico/mensajes';
import Estado, { type Tono } from '../../componentes/Estado';

const TONO_ESTADO: Record<EstadoAsistencia, Tono> = {
  ASISTIO: 'neutro',
  FALTA: 'negativo',
  FALTA_JUSTIFICADA: 'positivo',
  RETARDO: 'alerta',
  PERMISO: 'informativo',
};

interface Props {
  matriculaId: number;
  nombre: string;
  grupoId: number;
  puedeJustificar: boolean;
  alCerrar: () => void;
}

/** Faltas, permisos y retardos de un estudiante, y justificacion de las faltas de un dia. */
export default function DialogoNovedades({ matriculaId, nombre, grupoId, puedeJustificar, alCerrar }: Props) {
  const queryClient = useQueryClient();
  const [fecha, setFecha] = useState('');
  const [justificacion, setJustificacion] = useState('');
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);
  const novedades = useQuery({
    queryKey: ['asistencia-novedades', matriculaId],
    queryFn: () => listarNovedades(matriculaId),
  });

  const fechasConFalta = [...new Set((novedades.data ?? []).filter((n) => n.estado === 'FALTA').map((n) => n.fecha))];
  const limiteDe = (f: string) => novedades.data?.find((n) => n.fecha === f)?.fechaLimite;

  const justificar = async () => {
    setMensaje(null);
    setGuardando(true);
    try {
      const actualizadas = await justificarFaltas(matriculaId, fecha, justificacion);
      queryClient.setQueryData(['asistencia-novedades', matriculaId], actualizadas);
      await queryClient.invalidateQueries({ queryKey: ['asistencia-resumen', grupoId] });
      setMensaje({ tipo: 'success', texto: `Faltas del ${fecha} justificadas` });
      setFecha('');
      setJustificacion('');
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="md" fullWidth>
      <DialogTitle>Asistencia de {nombre}</DialogTitle>
      <DialogContent>
        {mensaje && (
          <Alert severity={mensaje.tipo} sx={{ mb: 2 }}>
            {mensaje.texto}
          </Alert>
        )}
        {novedades.isPending && <CircularProgress />}
        {novedades.isError && <Alert severity="error">{mensajeDeError(novedades.error)}</Alert>}
        {novedades.data?.length === 0 && <Alert severity="info">No tiene faltas, permisos ni retardos.</Alert>}

        {puedeJustificar && fechasConFalta.length > 0 && (
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ my: 1 }}>
            <TextField
              select
              label="Día de la falta"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              sx={{ minWidth: 220 }}
            >
              {fechasConFalta.map((f) => (
                <MenuItem key={f} value={f}>
                  {f} (plazo hasta {limiteDe(f)})
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Justificación y soporte presentado"
              value={justificacion}
              onChange={(e) => setJustificacion(e.target.value)}
              slotProps={{ htmlInput: { maxLength: 500 } }}
              sx={{ flex: 1 }}
            />
            <Button
              variant="contained"
              onClick={justificar}
              disabled={guardando || !fecha || !justificacion.trim()}
            >
              Justificar
            </Button>
          </Stack>
        )}

        {novedades.data && novedades.data.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Fecha</TableCell>
                <TableCell>Asignatura</TableCell>
                <TableCell align="center">Horas</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell>Detalle</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {novedades.data.map((n) => (
                <TableRow key={n.detalleId}>
                  <TableCell>{n.fecha}</TableCell>
                  <TableCell>{n.asignatura}</TableCell>
                  <TableCell align="center">{n.horas}</TableCell>
                  <TableCell>
                    <Estado tono={TONO_ESTADO[n.estado]} texto={NOMBRE_ESTADO_ASISTENCIA[n.estado]} />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{n.justificacion ?? n.observacion ?? ''}</Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}
