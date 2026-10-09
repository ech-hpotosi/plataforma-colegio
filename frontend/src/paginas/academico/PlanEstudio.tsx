import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  MenuItem,
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
import { guardarPlan, listarAreas, listarGrados, listarPlan } from '../../api/academico';
import { mensajeDeError } from './mensajes';
import SelectorAnio from './SelectorAnio';

/**
 * Plan de estudios de un grado en un anio: se marcan las asignaturas que se dictan
 * y su intensidad horaria semanal.
 */
export default function PlanEstudio() {
  const queryClient = useQueryClient();
  const [anioId, setAnioId] = useState<number | null>(null);
  const [gradoId, setGradoId] = useState<number | null>(null);
  // Intensidad por asignatura; las asignaturas que no estan en el mapa no hacen parte del plan
  const [intensidades, setIntensidades] = useState<Record<number, string>>({});
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);

  const grados = useQuery({ queryKey: ['grados'], queryFn: listarGrados });
  const areas = useQuery({ queryKey: ['areas'], queryFn: listarAreas });
  const plan = useQuery({
    queryKey: ['plan', anioId, gradoId],
    queryFn: () => listarPlan(anioId!, gradoId!),
    enabled: anioId !== null && gradoId !== null,
  });

  useEffect(() => {
    if (plan.data) {
      setIntensidades(Object.fromEntries(plan.data.map((p) => [p.asignaturaId, String(p.intensidadHoraria)])));
    }
  }, [plan.data]);

  const total = Object.values(intensidades).reduce((suma, h) => suma + (Number(h) || 0), 0);

  const guardar = async () => {
    if (anioId === null || gradoId === null) return;
    setMensaje(null);
    setGuardando(true);
    try {
      const asignaturas = Object.entries(intensidades).map(([id, h]) => ({
        asignaturaId: Number(id),
        intensidadHoraria: Number(h),
      }));
      const guardado = await guardarPlan(anioId, gradoId, asignaturas);
      queryClient.setQueryData(['plan', anioId, gradoId], guardado);
      setMensaje({ tipo: 'success', texto: 'Plan de estudios guardado' });
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <>
      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
        <SelectorAnio
          valor={anioId}
          alCambiar={(id) => {
            setAnioId(id);
            setMensaje(null);
          }}
        />
        <TextField
          select
          size="small"
          label="Grado"
          value={gradoId ?? ''}
          onChange={(e) => {
            setGradoId(Number(e.target.value));
            setMensaje(null);
          }}
          sx={{ minWidth: 180 }}
        >
          {grados.data?.map((g) => (
            <MenuItem key={g.id} value={g.id}>
              {g.nombre}
            </MenuItem>
          ))}
        </TextField>
        <Box sx={{ flexGrow: 1 }} />
        {plan.data && (
          <Button variant="contained" onClick={guardar} disabled={guardando}>
            Guardar plan
          </Button>
        )}
      </Box>
      {mensaje && (
        <Alert severity={mensaje.tipo} sx={{ mb: 2 }}>
          {mensaje.texto}
        </Alert>
      )}
      {gradoId === null && <Typography>Seleccione el año y el grado para ver su plan de estudios.</Typography>}
      {plan.data && (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox" />
                <TableCell>Asignatura</TableCell>
                <TableCell>Área</TableCell>
                <TableCell>Horas semanales</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {areas.data?.flatMap((area) =>
                area.asignaturas.map((asignatura) => {
                  const incluida = asignatura.id in intensidades;
                  return (
                    <TableRow key={asignatura.id}>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={incluida}
                          slotProps={{ input: { 'aria-label': `Incluir ${asignatura.nombre}` } }}
                          onChange={(e) =>
                            setIntensidades((actual) => {
                              const copia = { ...actual };
                              if (e.target.checked) copia[asignatura.id] = '1';
                              else delete copia[asignatura.id];
                              return copia;
                            })
                          }
                        />
                      </TableCell>
                      <TableCell>{asignatura.nombre}</TableCell>
                      <TableCell>{area.nombre}</TableCell>
                      <TableCell>
                        {incluida && (
                          <TextField
                            size="small"
                            type="number"
                            value={intensidades[asignatura.id]}
                            onChange={(e) =>
                              setIntensidades((actual) => ({ ...actual, [asignatura.id]: e.target.value }))
                            }
                            slotProps={{ htmlInput: { min: 1, max: 40, 'aria-label': `Horas de ${asignatura.nombre}` } }}
                            sx={{ width: 90 }}
                          />
                        )}
                      </TableCell>
                    </TableRow>
                  );
                }),
              )}
              <TableRow>
                <TableCell colSpan={3} align="right">
                  Total horas semanales
                </TableCell>
                <TableCell>{total}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}
