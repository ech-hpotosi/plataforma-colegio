import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  InputBase,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { guardarRecuperacionFinal, leerNota, obtenerRecuperacionFinal } from '../../api/notas';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';
import EstadoDesempeno from './Desempeno';

const formato = (valor: number | null) => (valor === null ? '' : valor.toFixed(1));

/**
 * Recuperacion final del anio de una clase (SIEE art. 11): para quien termina el anio en Bajo,
 * con las recuperaciones de cada periodo ya aplicadas.
 */
export default function RecuperacionFinal({ cargaId }: { cargaId: number }) {
  const queryClient = useQueryClient();
  const [cambios, setCambios] = useState<Record<number, string>>({});
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);
  const consulta = useQuery({
    queryKey: ['notas-recuperacion-final', cargaId],
    queryFn: () => obtenerRecuperacionFinal(cargaId),
  });

  if (consulta.isPending) return <CircularProgress />;
  if (consulta.isError) return <Alert severity="error">{mensajeDeError(consulta.error)}</Alert>;
  const datos = consulta.data;
  const config = datos.configuracion;
  const enBajo = (f: (typeof datos.estudiantes)[number]) =>
    f.recuperacion !== null || (f.notaAnio !== null && f.notaAnio < config.notaAprobatoria);
  const filas = datos.estudiantes.filter(enBajo);

  const guardar = async () => {
    const items = Object.entries(cambios).map(([k, texto]) => ({
      matriculaId: Number(k),
      nota: leerNota(texto, config),
      observacion: null,
    }));
    if (items.some((i) => i.nota === undefined)) {
      setMensaje({ tipo: 'error', texto: `Use valores de ${formato(config.notaMinima)} a ${formato(config.notaMaxima)} con una decimal.` });
      return;
    }
    setGuardando(true);
    setMensaje(null);
    try {
      const nueva = await guardarRecuperacionFinal(
        cargaId,
        items.map((i) => ({ ...i, nota: i.nota ?? null })),
      );
      queryClient.setQueryData(['notas-recuperacion-final', cargaId], nueva);
      setCambios({});
      setMensaje({ tipo: 'success', texto: 'Recuperaciones guardadas' });
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Typography variant="body2" color="text.secondary">
        Estudiantes que terminan el año en Bajo en esta asignatura, con las recuperaciones de cada periodo ya aplicadas.
        La definitiva es la mayor entre la nota del año y la recuperación, sin pasar de {formato(config.topeRecuperacion)}.
      </Typography>
      {mensaje && <Alert severity={mensaje.tipo}>{mensaje.texto}</Alert>}
      {filas.length === 0 ? (
        <Alert severity="info">Ningún estudiante tiene la nota del año en Bajo en esta asignatura.</Alert>
      ) : (
        <>
          <TableContainer component={Paper}>
            <Table size="small" sx={{ '& td, & th': { whiteSpace: 'nowrap' } }}>
              <TableHead>
                <TableRow>
                  <TableCell>Estudiante</TableCell>
                  <TableCell align="center">Nota del año</TableCell>
                  <TableCell align="center">Recuperación</TableCell>
                  <TableCell align="center">Definitiva</TableCell>
                  <TableCell>Desempeño</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filas.map((f) => (
                  <TableRow key={f.matriculaId}>
                    <TableCell>
                      {f.apellidos} {f.nombres}
                    </TableCell>
                    <TableCell align="center">
                      {f.notaAnio === null ? '-' : `${formato(f.notaAnio)}${f.completa ? '' : '*'}`}
                    </TableCell>
                    <TableCell align="center" sx={{ py: 0.5 }}>
                      <InputBase
                        value={cambios[f.matriculaId] ?? formato(f.recuperacion)}
                        readOnly={!datos.editable}
                        onChange={(e) => setCambios((c) => ({ ...c, [f.matriculaId]: e.target.value }))}
                        inputProps={{
                          inputMode: 'decimal',
                          'aria-label': `Recuperación final de ${f.nombres} ${f.apellidos}`,
                          style: { textAlign: 'center' },
                        }}
                        sx={{
                          width: 52,
                          px: 0.5,
                          border: `1px solid ${cambios[f.matriculaId] !== undefined ? COLORES.sol : COLORES.linea}`,
                          borderRadius: 1,
                        }}
                      />
                    </TableCell>
                    <TableCell align="center" sx={{ fontWeight: 700 }}>
                      {formato(f.notaDefinitiva)}
                    </TableCell>
                    <TableCell>
                      <EstadoDesempeno desempeno={f.desempeno} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {datos.editable && (
            <Box>
              <Button variant="contained" onClick={guardar} disabled={guardando || Object.keys(cambios).length === 0}>
                Guardar recuperaciones
              </Button>
            </Box>
          )}
        </>
      )}
    </Stack>
  );
}
