import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormGroup,
  FormLabel,
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
import { actualizarDocente, listarDocentes, listarSedes, type Docente, type Sede } from '../../api/academico';
import { mensajeDeError } from './mensajes';

/** Docentes (usuarios activos con rol Docente) con su especialidad, escalafon y sedes donde trabajan. */
export default function Docentes() {
  const queryClient = useQueryClient();
  const [editando, setEditando] = useState<Docente | null>(null);
  const docentes = useQuery({ queryKey: ['docentes'], queryFn: listarDocentes });
  const sedes = useQuery({ queryKey: ['sedes'], queryFn: listarSedes });
  const nombreSede = (id: number) => sedes.data?.find((s) => s.id === id)?.nombre ?? '';

  return (
    <>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Aqui aparecen los usuarios activos con rol Docente. Para agregar un docente, cree su usuario en Usuarios. Un
        docente sin sedes se puede asignar en cualquier sede.
      </Typography>
      {docentes.isError && <Alert severity="error">{docentes.error.message}</Alert>}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Documento</TableCell>
              <TableCell>Especialidad</TableCell>
              <TableCell>Escalafon</TableCell>
              <TableCell>Sedes</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {docentes.data?.map((docente) => (
              <TableRow key={docente.id} hover>
                <TableCell>
                  {docente.apellidos} {docente.nombres}
                </TableCell>
                <TableCell>{docente.numeroDocumento}</TableCell>
                <TableCell>{docente.especialidad}</TableCell>
                <TableCell>{docente.escalafon}</TableCell>
                <TableCell>{docente.sedeIds.map(nombreSede).join(', ') || 'Todas'}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setEditando(docente)}>
                    Editar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {docentes.data?.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No hay usuarios con rol Docente
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {editando && (
        <DialogoDocente
          docente={editando}
          sedes={sedes.data ?? []}
          alCerrar={() => setEditando(null)}
          alGuardar={() => {
            setEditando(null);
            queryClient.invalidateQueries({ queryKey: ['docentes'] });
          }}
        />
      )}
    </>
  );
}

function DialogoDocente({
  docente,
  sedes,
  alCerrar,
  alGuardar,
}: {
  docente: Docente;
  sedes: Sede[];
  alCerrar: () => void;
  alGuardar: () => void;
}) {
  const [especialidad, setEspecialidad] = useState(docente.especialidad ?? '');
  const [escalafon, setEscalafon] = useState(docente.escalafon ?? '');
  const [sedeIds, setSedeIds] = useState<number[]>(docente.sedeIds);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  const guardar = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setGuardando(true);
    try {
      await actualizarDocente(docente.id, { especialidad, escalafon, sedeIds });
      alGuardar();
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>
        {docente.nombres} {docente.apellidos}
      </DialogTitle>
      <form onSubmit={guardar} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              label="Especialidad"
              value={especialidad}
              onChange={(e) => setEspecialidad(e.target.value)}
              slotProps={{ htmlInput: { maxLength: 150 } }}
            />
            <TextField
              label="Escalafon"
              value={escalafon}
              onChange={(e) => setEscalafon(e.target.value)}
              slotProps={{ htmlInput: { maxLength: 30 } }}
            />
            <div>
              <FormLabel component="legend">Sedes donde trabaja</FormLabel>
              <FormGroup row>
                {sedes.map((sede) => (
                  <FormControlLabel
                    key={sede.id}
                    label={sede.nombre}
                    control={
                      <Checkbox
                        checked={sedeIds.includes(sede.id)}
                        onChange={(e) =>
                          setSedeIds(e.target.checked ? [...sedeIds, sede.id] : sedeIds.filter((id) => id !== sede.id))
                        }
                      />
                    }
                  />
                ))}
              </FormGroup>
            </div>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={guardando}>
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
