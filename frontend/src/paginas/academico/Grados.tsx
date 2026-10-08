import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Paper,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { actualizarGrado, listarGrados, NOMBRE_NIVEL, type Grado } from '../../api/academico';

/** Grados fijos del sistema educativo. Solo se configura si se evaluan de forma cualitativa. */
export default function Grados() {
  const queryClient = useQueryClient();
  const consulta = useQuery({ queryKey: ['grados'], queryFn: listarGrados });
  const cambiar = useMutation({
    mutationFn: (grado: Grado) => actualizarGrado(grado.id, !grado.evaluacionCualitativa),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['grados'] }),
  });

  return (
    <>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Los grados con evaluacion cualitativa no llevan nota numerica en el boletin (por ejemplo Transicion, segun
        el SIEE).
      </Typography>
      {consulta.isError && <Alert severity="error">{consulta.error.message}</Alert>}
      {cambiar.isError && <Alert severity="error">{cambiar.error.message}</Alert>}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Grado</TableCell>
              <TableCell>Nivel</TableCell>
              <TableCell>Evaluacion cualitativa</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {consulta.data?.map((grado) => (
              <TableRow key={grado.id}>
                <TableCell>{grado.nombre}</TableCell>
                <TableCell>{NOMBRE_NIVEL[grado.nivel]}</TableCell>
                <TableCell>
                  <Switch
                    checked={grado.evaluacionCualitativa}
                    disabled={cambiar.isPending}
                    onChange={() => cambiar.mutate(grado)}
                    slotProps={{ input: { 'aria-label': `Evaluacion cualitativa de ${grado.nombre}` } }}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}
