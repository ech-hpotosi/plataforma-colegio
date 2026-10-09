import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
} from '@mui/material';
import { actualizarSede, crearSede, listarSedes, type Sede } from '../../api/academico';
import { mensajeDeError } from './mensajes';

const esquema = z.object({
  nombre: z.string().trim().min(1, 'Ingrese el nombre de la sede').max(120),
  codigoDane: z.string().trim().regex(/^[0-9]{0,20}$/, 'El código DANE solo lleva números'),
  direccion: z.string().trim().max(200),
  principal: z.boolean(),
});

type Datos = z.infer<typeof esquema>;

export default function Sedes() {
  const queryClient = useQueryClient();
  const [editando, setEditando] = useState<Sede | 'nueva' | null>(null);
  const consulta = useQuery({ queryKey: ['sedes'], queryFn: listarSedes });

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button variant="contained" onClick={() => setEditando('nueva')}>
          Nueva sede
        </Button>
      </Box>
      {consulta.isError && <Alert severity="error">{consulta.error.message}</Alert>}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Código DANE</TableCell>
              <TableCell>Dirección</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {consulta.data?.map((sede) => (
              <TableRow key={sede.id} hover>
                <TableCell>
                  {sede.nombre} {sede.principal && <Chip size="small" color="primary" label="Principal" />}
                </TableCell>
                <TableCell>{sede.codigoDane}</TableCell>
                <TableCell>{sede.direccion}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setEditando(sede)}>
                    Editar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {consulta.data?.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  No hay sedes registradas
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {editando !== null && (
        <DialogoSede
          sede={editando === 'nueva' ? null : editando}
          alCerrar={() => setEditando(null)}
          alGuardar={() => {
            setEditando(null);
            queryClient.invalidateQueries({ queryKey: ['sedes'] });
          }}
        />
      )}
    </>
  );
}

function DialogoSede({ sede, alCerrar, alGuardar }: { sede: Sede | null; alCerrar: () => void; alGuardar: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      nombre: sede?.nombre ?? '',
      codigoDane: sede?.codigoDane ?? '',
      direccion: sede?.direccion ?? '',
      principal: sede?.principal ?? false,
    },
  });

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      await (sede ? actualizarSede(sede.id, datos) : crearSede(datos));
      alGuardar();
    } catch (e) {
      setError(mensajeDeError(e));
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{sede ? `Editar ${sede.nombre}` : 'Nueva sede'}</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Stack spacing={2} sx={{ pt: 1 }}>
            <TextField
              label="Nombre"
              {...register('nombre')}
              error={!!errors.nombre}
              helperText={errors.nombre?.message}
            />
            <TextField
              label="Código DANE"
              {...register('codigoDane')}
              error={!!errors.codigoDane}
              helperText={errors.codigoDane?.message}
            />
            <TextField
              label="Dirección"
              {...register('direccion')}
              error={!!errors.direccion}
              helperText={errors.direccion?.message}
            />
            <FormControlLabel
              control={<Checkbox defaultChecked={sede?.principal ?? false} {...register('principal')} />}
              label="Sede principal"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
