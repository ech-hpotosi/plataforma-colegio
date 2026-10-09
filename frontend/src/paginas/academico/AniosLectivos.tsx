import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid2 as Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  actualizarAnio,
  cambiarEstadoAnio,
  crearAnio,
  ESTADOS_ANIO,
  listarAnios,
  NOMBRE_ESTADO_ANIO,
  type AnioLectivo,
} from '../../api/academico';
import { mensajeDeError } from './mensajes';
import Estado from '../../componentes/Estado';

const fecha = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingrese la fecha');

// Las reglas de fechas y porcentajes las valida el backend; aqui solo se revisa que esten completos
const esquema = z.object({
  anio: z.coerce.number<string>().int().min(2020, 'Año inválido').max(2100, 'Año inválido'),
  fechaInicio: fecha,
  fechaFin: fecha,
  periodos: z
    .array(
      z.object({
        fechaInicio: fecha,
        fechaFin: fecha,
        porcentaje: z.coerce.number<string>().gt(0, 'Mayor que cero').max(100, 'Máximo 100'),
      }),
    )
    .min(1, 'Ingrese al menos un periodo')
    .max(6, 'Máximo 6 periodos'),
});

type Entrada = z.input<typeof esquema>;
type Datos = z.output<typeof esquema>;

/** Formatea una fecha ISO (aaaa-mm-dd) como dd/mm/aaaa. */
function fechaCorta(iso: string) {
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}

export default function AniosLectivos() {
  const queryClient = useQueryClient();
  const [editando, setEditando] = useState<AnioLectivo | 'nuevo' | null>(null);
  const [error, setError] = useState<string | null>(null);
  const consulta = useQuery({ queryKey: ['anios'], queryFn: listarAnios });

  const avanzar = useMutation({
    mutationFn: (anio: AnioLectivo) => cambiarEstadoAnio(anio.id, ESTADOS_ANIO[ESTADOS_ANIO.indexOf(anio.estado) + 1]),
    onMutate: () => setError(null),
    onError: (e) => setError(mensajeDeError(e)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['anios'] }),
  });

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Button variant="contained" onClick={() => setEditando('nuevo')}>
          Nuevo año lectivo
        </Button>
      </Box>
      {consulta.isError && <Alert severity="error">{consulta.error.message}</Alert>}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Año</TableCell>
              <TableCell>Fechas</TableCell>
              <TableCell>Periodos</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {consulta.data?.map((anio) => {
              const siguiente = ESTADOS_ANIO[ESTADOS_ANIO.indexOf(anio.estado) + 1];
              return (
                <TableRow key={anio.id} hover>
                  <TableCell>{anio.anio}</TableCell>
                  <TableCell>
                    {fechaCorta(anio.fechaInicio)} a {fechaCorta(anio.fechaFin)}
                  </TableCell>
                  <TableCell>
                    {anio.periodos.map((p) => (
                      <Typography key={p.id} variant="body2">
                        P{p.numero}: {fechaCorta(p.fechaInicio)} a {fechaCorta(p.fechaFin)} ({p.porcentaje}%)
                      </Typography>
                    ))}
                  </TableCell>
                  <TableCell>
                    <Estado
                      tono={anio.estado === 'EN_CURSO' ? 'positivo' : anio.estado === 'CERRADO' ? 'neutro' : 'informativo'}
                      texto={NOMBRE_ESTADO_ANIO[anio.estado]}
                    />
                  </TableCell>
                  <TableCell align="right">
                    {anio.estado !== 'CERRADO' && (
                      <Button size="small" onClick={() => setEditando(anio)}>
                        Editar
                      </Button>
                    )}
                    {siguiente && (
                      <Button
                        size="small"
                        disabled={avanzar.isPending}
                        onClick={() => {
                          if (window.confirm(`Pasar el año ${anio.anio} a ${NOMBRE_ESTADO_ANIO[siguiente]}? No se puede devolver.`)) {
                            avanzar.mutate(anio);
                          }
                        }}
                      >
                        Pasar a {NOMBRE_ESTADO_ANIO[siguiente]}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              );
            })}
            {consulta.data?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  No hay años lectivos registrados
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {editando !== null && (
        <DialogoAnio
          anio={editando === 'nuevo' ? null : editando}
          alCerrar={() => setEditando(null)}
          alGuardar={() => {
            setEditando(null);
            queryClient.invalidateQueries({ queryKey: ['anios'] });
          }}
        />
      )}
    </>
  );
}

function valoresIniciales(anio: AnioLectivo | null): Entrada {
  if (anio) {
    return {
      anio: String(anio.anio),
      fechaInicio: anio.fechaInicio,
      fechaFin: anio.fechaFin,
      periodos: anio.periodos.map((p) => ({
        fechaInicio: p.fechaInicio,
        fechaFin: p.fechaFin,
        porcentaje: String(p.porcentaje),
      })),
    };
  }
  // Por defecto el SIEE tiene tres periodos; los porcentajes se ajustan segun el colegio
  const siguiente = new Date().getFullYear() + 1;
  return {
    anio: String(siguiente),
    fechaInicio: '',
    fechaFin: '',
    periodos: [
      { fechaInicio: '', fechaFin: '', porcentaje: '30' },
      { fechaInicio: '', fechaFin: '', porcentaje: '30' },
      { fechaInicio: '', fechaFin: '', porcentaje: '40' },
    ],
  };
}

function DialogoAnio({
  anio,
  alCerrar,
  alGuardar,
}: {
  anio: AnioLectivo | null;
  alCerrar: () => void;
  alGuardar: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<Entrada, unknown, Datos>({ resolver: zodResolver(esquema), defaultValues: valoresIniciales(anio) });
  const { fields, append, remove } = useFieldArray({ control, name: 'periodos' });
  const suma = watch('periodos').reduce((total, p) => total + (Number(p.porcentaje) || 0), 0);

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      await (anio ? actualizarAnio(anio.id, datos) : crearAnio(datos));
      alGuardar();
    } catch (e) {
      setError(mensajeDeError(e));
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="md" fullWidth>
      <DialogTitle>{anio ? `Editar año ${anio.anio}` : 'Nuevo año lectivo'}</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label="Año"
                type="number"
                fullWidth
                disabled={anio !== null}
                {...register('anio')}
                error={!!errors.anio}
                helperText={errors.anio?.message}
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 4 }}>
              <TextField
                label="Inicio"
                type="date"
                fullWidth
                slotProps={{ inputLabel: { shrink: true } }}
                {...register('fechaInicio')}
                error={!!errors.fechaInicio}
                helperText={errors.fechaInicio?.message}
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 4 }}>
              <TextField
                label="Fin"
                type="date"
                fullWidth
                slotProps={{ inputLabel: { shrink: true } }}
                {...register('fechaFin')}
                error={!!errors.fechaFin}
                helperText={errors.fechaFin?.message}
              />
            </Grid>
            {fields.map((campo, i) => (
              <Grid container size={12} spacing={2} key={campo.id} sx={{ alignItems: 'center' }}>
                <Grid size={{ xs: 12, sm: 2 }}>
                  <Typography>Periodo {i + 1}</Typography>
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField
                    label="Inicio"
                    type="date"
                    fullWidth
                    slotProps={{ inputLabel: { shrink: true } }}
                    {...register(`periodos.${i}.fechaInicio`)}
                    error={!!errors.periodos?.[i]?.fechaInicio}
                  />
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField
                    label="Fin"
                    type="date"
                    fullWidth
                    slotProps={{ inputLabel: { shrink: true } }}
                    {...register(`periodos.${i}.fechaFin`)}
                    error={!!errors.periodos?.[i]?.fechaFin}
                  />
                </Grid>
                <Grid size={{ xs: 8, sm: 2 }}>
                  <TextField
                    label="Porcentaje"
                    type="number"
                    fullWidth
                    {...register(`periodos.${i}.porcentaje`)}
                    error={!!errors.periodos?.[i]?.porcentaje}
                    helperText={errors.periodos?.[i]?.porcentaje?.message}
                  />
                </Grid>
                <Grid size={{ xs: 4, sm: 2 }}>
                  {fields.length > 1 && i === fields.length - 1 && (
                    <Button color="error" onClick={() => remove(i)}>
                      Quitar
                    </Button>
                  )}
                </Grid>
              </Grid>
            ))}
            <Grid size={12} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              {fields.length < 6 && (
                <Button onClick={() => append({ fechaInicio: '', fechaFin: '', porcentaje: '' })}>
                  Agregar periodo
                </Button>
              )}
              <Typography color={suma === 100 ? 'text.secondary' : 'error'}>
                Suma de porcentajes: {suma}%
              </Typography>
            </Grid>
          </Grid>
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
