import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  buscarEstudiantes,
  NOMBRE_ESTADO_ESTUDIANTE,
  type EstadoEstudiante,
} from '../../api/estudiantes';
import { useSesion } from '../../sesion/useSesion';
import DialogoEstudiante from './DialogoEstudiante';

export const COLOR_ESTADO: Record<EstadoEstudiante, 'default' | 'success' | 'warning' | 'info'> = {
  ASPIRANTE: 'info',
  ACTIVO: 'success',
  RETIRADO: 'warning',
  GRADUADO: 'default',
};

/** Busqueda de estudiantes. Al hacer clic en uno se abre su ficha. */
export default function ListaEstudiantes() {
  const navegar = useNavigate();
  const { tieneAlgunRol } = useSesion();
  const puedeEditar = tieneAlgunRol(['ADMINISTRADOR', 'SECRETARIA']);
  const [buscar, setBuscar] = useState('');
  const [estado, setEstado] = useState<EstadoEstudiante | ''>('');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [creando, setCreando] = useState(false);

  const consulta = useQuery({
    queryKey: ['estudiantes', buscar, estado, pagina, tamano],
    queryFn: () => buscarEstudiantes(buscar, estado, pagina, tamano),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <Typography variant="h5" component="h1" sx={{ flexGrow: 1 }}>
          Estudiantes
        </Typography>
        <TextField
          size="small"
          label="Buscar por nombre, documento o codigo"
          value={buscar}
          onChange={(e) => {
            setBuscar(e.target.value);
            setPagina(0);
          }}
          sx={{ minWidth: 280 }}
        />
        <TextField
          select
          size="small"
          label="Estado"
          value={estado}
          onChange={(e) => {
            setEstado(e.target.value as EstadoEstudiante | '');
            setPagina(0);
          }}
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">Todos</MenuItem>
          {Object.entries(NOMBRE_ESTADO_ESTUDIANTE).map(([valor, nombre]) => (
            <MenuItem key={valor} value={valor}>
              {nombre}
            </MenuItem>
          ))}
        </TextField>
        {puedeEditar && (
          <Button variant="contained" onClick={() => setCreando(true)}>
            Nuevo estudiante
          </Button>
        )}
      </Box>

      {consulta.isError && <Alert severity="error">{consulta.error.message}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Documento</TableCell>
              <TableCell>Codigo</TableCell>
              <TableCell>Estado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {consulta.data?.contenido.map((e) => (
              <TableRow key={e.id} hover sx={{ cursor: 'pointer' }} onClick={() => navegar(`/estudiantes/${e.id}`)}>
                <TableCell>
                  {e.apellidos} {e.nombres}
                </TableCell>
                <TableCell>
                  {e.tipoDocumento} {e.numeroDocumento}
                </TableCell>
                <TableCell>{e.codigo}</TableCell>
                <TableCell>
                  <Chip size="small" color={COLOR_ESTADO[e.estado]} label={NOMBRE_ESTADO_ESTUDIANTE[e.estado]} />
                </TableCell>
              </TableRow>
            ))}
            {consulta.data?.contenido.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  No hay estudiantes que coincidan con la busqueda
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={consulta.data?.totalElementos ?? 0}
          page={pagina}
          rowsPerPage={tamano}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_, nueva) => setPagina(nueva)}
          onRowsPerPageChange={(e) => {
            setTamano(Number(e.target.value));
            setPagina(0);
          }}
          labelRowsPerPage="Filas por pagina"
          labelDisplayedRows={({ from, to, count }) => `${from} a ${to} de ${count}`}
        />
      </TableContainer>

      {creando && (
        <DialogoEstudiante
          estudiante={null}
          alCerrar={() => setCreando(false)}
          alGuardar={(nuevo) => navegar(`/estudiantes/${nuevo.id}`)}
        />
      )}
    </>
  );
}
