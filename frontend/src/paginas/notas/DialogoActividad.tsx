import { useState } from 'react';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import {
  DIMENSIONES,
  NOMBRE_DIMENSION,
  crearActividad,
  eliminarActividad,
  modificarActividad,
  type Actividad,
  type Dimension,
  type PlanillaNotas,
} from '../../api/notas';
import { mensajeDeError } from '../academico/mensajes';

interface Props {
  cargaId: number;
  periodoId: number;
  /** Actividad a editar, o null para crear una nueva. */
  actividad: Actividad | null;
  alCerrar: () => void;
  alGuardar: (planilla: PlanillaNotas) => void;
}

/** Crea, cambia o borra una actividad de la planilla. Borrarla borra tambien sus notas. */
export default function DialogoActividad({ cargaId, periodoId, actividad, alCerrar, alGuardar }: Props) {
  const [dimension, setDimension] = useState<Dimension>(actividad?.dimension ?? 'SABER');
  const [nombre, setNombre] = useState(actividad?.nombre ?? '');
  const [fecha, setFecha] = useState(actividad?.fecha ?? '');
  const [confirmarBorrado, setConfirmarBorrado] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  const ejecutar = async (accion: () => Promise<PlanillaNotas>) => {
    setError(null);
    setGuardando(true);
    try {
      alGuardar(await accion());
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setGuardando(false);
    }
  };

  const guardar = () => {
    if (!nombre.trim()) {
      setError('Escriba el nombre de la actividad');
      return;
    }
    const datos = { dimension, nombre: nombre.trim(), fecha: fecha || null };
    ejecutar(() => (actividad ? modificarActividad(actividad.id, datos) : crearActividad(cargaId, periodoId, datos)));
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>{actividad ? 'Editar actividad' : 'Nueva actividad'}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {confirmarBorrado ? (
          <Alert severity="warning">
            Se borrará la actividad «{actividad?.nombre}» con todas sus notas. Esta acción no se puede deshacer.
          </Alert>
        ) : (
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField select label="Dimensión" value={dimension} onChange={(e) => setDimension(e.target.value as Dimension)}>
              {DIMENSIONES.map((d) => (
                <MenuItem key={d} value={d}>
                  {NOMBRE_DIMENSION[d]}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              placeholder="Por ejemplo: Taller 1, Evaluación escrita"
              slotProps={{ htmlInput: { maxLength: 120 } }}
              autoFocus
            />
            <TextField
              label="Fecha (opcional)"
              type="date"
              value={fecha}
              onChange={(e) => setFecha(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        {actividad && !confirmarBorrado && (
          <Button color="error" onClick={() => setConfirmarBorrado(true)} sx={{ mr: 'auto' }}>
            Borrar
          </Button>
        )}
        <Button variant="outlined" onClick={confirmarBorrado ? () => setConfirmarBorrado(false) : alCerrar}>
          {confirmarBorrado ? 'Volver' : 'Cancelar'}
        </Button>
        {confirmarBorrado ? (
          <Button variant="contained" color="error" disabled={guardando} onClick={() => ejecutar(() => eliminarActividad(actividad!.id))}>
            Borrar actividad
          </Button>
        ) : (
          <Button variant="contained" disabled={guardando} onClick={guardar}>
            Guardar
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
