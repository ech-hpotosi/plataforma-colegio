import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
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
import { listarGrupos } from '../../api/academico';
import { cambiarGrupo, matricular, type Matricula } from '../../api/estudiantes';
import { mensajeDeError } from '../academico/mensajes';
import SelectorAnio from '../academico/SelectorAnio';

interface Props {
  estudianteId: number;
  /** Matricula a la que se le cambia el grupo, o null para matricular en un anio. */
  matricula: Matricula | null;
  alCerrar: () => void;
  alGuardar: () => void;
}

/** Matricula al estudiante en un anio lectivo o le cambia el grupo. */
export default function DialogoMatricula({ estudianteId, matricula, alCerrar, alGuardar }: Props) {
  const [anioId, setAnioId] = useState<number | null>(matricula?.anioLectivoId ?? null);
  const [grupoId, setGrupoId] = useState<number>(matricula?.grupoId ?? 0);
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const grupos = useQuery({
    queryKey: ['grupos', anioId, null],
    queryFn: () => listarGrupos(anioId!, null),
    enabled: anioId !== null,
  });

  const guardar = async () => {
    if (anioId === null) return;
    setError(null);
    setGuardando(true);
    try {
      const grupo = grupoId || null;
      await (matricula ? cambiarGrupo(matricula.id, grupo) : matricular(estudianteId, anioId, grupo));
      alGuardar();
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>{matricula ? `Cambiar grupo (${matricula.anio})` : 'Matricular'}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Stack spacing={2} sx={{ pt: 1 }}>
          {!matricula && (
            <SelectorAnio
              valor={anioId}
              alCambiar={(id) => {
                setAnioId(id);
                setGrupoId(0);
              }}
            />
          )}
          <TextField select label="Grupo" value={grupoId} onChange={(e) => setGrupoId(Number(e.target.value))}>
            <MenuItem value={0}>Sin grupo por ahora</MenuItem>
            {grupos.data?.map((g) => (
              <MenuItem key={g.id} value={g.id}>
                {g.grado} {g.nombre} ({g.sede})
              </MenuItem>
            ))}
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" onClick={guardar} disabled={guardando || anioId === null}>
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
