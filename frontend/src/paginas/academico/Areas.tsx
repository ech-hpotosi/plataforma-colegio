import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import {
  actualizarArea,
  actualizarAsignatura,
  crearArea,
  crearAsignatura,
  listarAreas,
  type Area,
  type Asignatura,
} from '../../api/academico';
import { mensajeDeError } from './mensajes';

type Edicion =
  | { tipo: 'area'; area: Area | null }
  | { tipo: 'asignatura'; asignatura: Asignatura | null; areaId: number };

/** Areas obligatorias y fundamentales con sus asignaturas. */
export default function Areas() {
  const queryClient = useQueryClient();
  const [edicion, setEdicion] = useState<Edicion | null>(null);
  const consulta = useQuery({ queryKey: ['areas'], queryFn: listarAreas });

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button variant="contained" onClick={() => setEdicion({ tipo: 'area', area: null })}>
          Nueva área
        </Button>
      </Box>
      {consulta.isError && <Alert severity="error">{consulta.error.message}</Alert>}
      {consulta.data?.length === 0 && <Typography>No hay áreas registradas</Typography>}
      <Stack spacing={2}>
        {consulta.data?.map((area) => (
          <Card key={area.id} variant="outlined">
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Typography variant="subtitle1" sx={{ flexGrow: 1, fontWeight: 600 }}>
                  {area.nombre}
                </Typography>
                <Button size="small" onClick={() => setEdicion({ tipo: 'area', area })}>
                  Editar
                </Button>
                <Button
                  size="small"
                  onClick={() => setEdicion({ tipo: 'asignatura', asignatura: null, areaId: area.id })}
                >
                  Agregar asignatura
                </Button>
              </Box>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {area.asignaturas.map((asignatura) => (
                  <Chip
                    key={asignatura.id}
                    label={asignatura.nombre}
                    onClick={() => setEdicion({ tipo: 'asignatura', asignatura, areaId: area.id })}
                  />
                ))}
                {area.asignaturas.length === 0 && (
                  <Typography variant="body2" color="text.secondary">
                    Sin asignaturas
                  </Typography>
                )}
              </Box>
            </CardContent>
          </Card>
        ))}
      </Stack>
      {edicion && (
        <DialogoNombre
          edicion={edicion}
          areas={consulta.data ?? []}
          alCerrar={() => setEdicion(null)}
          alGuardar={() => {
            setEdicion(null);
            queryClient.invalidateQueries({ queryKey: ['areas'] });
          }}
        />
      )}
    </>
  );
}

function DialogoNombre({
  edicion,
  areas,
  alCerrar,
  alGuardar,
}: {
  edicion: Edicion;
  areas: Area[];
  alCerrar: () => void;
  alGuardar: () => void;
}) {
  const esArea = edicion.tipo === 'area';
  const actual = esArea ? edicion.area : edicion.asignatura;
  const [nombre, setNombre] = useState(actual?.nombre ?? '');
  const [areaId, setAreaId] = useState(esArea ? 0 : edicion.areaId);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  const guardar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!nombre.trim()) {
      setError('Ingrese el nombre');
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      if (esArea) {
        await (edicion.area ? actualizarArea(edicion.area.id, nombre) : crearArea(nombre));
      } else {
        await (edicion.asignatura
          ? actualizarAsignatura(edicion.asignatura.id, areaId, nombre)
          : crearAsignatura(areaId, nombre));
      }
      alGuardar();
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setGuardando(false);
    }
  };

  const titulo = (actual ? 'Editar ' : 'Nueva ') + (esArea ? 'area' : 'asignatura');
  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <form onSubmit={guardar} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField label="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} autoFocus />
            {!esArea && (
              <TextField select label="Área" value={areaId} onChange={(e) => setAreaId(Number(e.target.value))}>
                {areas.map((area) => (
                  <MenuItem key={area.id} value={area.id}>
                    {area.nombre}
                  </MenuItem>
                ))}
              </TextField>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={guardando}>
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
