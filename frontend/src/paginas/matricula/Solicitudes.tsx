import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
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
} from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router';
import {
  buscarSolicitudes,
  NOMBRE_ESTADO_SOLICITUD,
  type EstadoSolicitud,
} from '../../api/matricula';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { useSesion } from '../../sesion/useSesion';
import SelectorAnio from '../academico/SelectorAnio';
import { mensajeDeError } from '../academico/mensajes';
import { fechaHora, TONO_SOLICITUD } from './estados';

/** Bandeja de la secretaria: solicitudes de matricula en linea del anio, las mas antiguas primero. */
export default function Solicitudes() {
  const navegar = useNavigate();
  const { tieneAlgunRol } = useSesion();
  const [anioId, setAnioId] = useState<number | null>(null);
  const [estado, setEstado] = useState<EstadoSolicitud | ''>('ENVIADA');
  const [buscar, setBuscar] = useState('');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(25);

  const consulta = useQuery({
    queryKey: ['solicitudes', anioId, estado, buscar, pagina, tamano],
    queryFn: () => buscarSolicitudes(anioId!, estado, buscar, pagina, tamano),
    enabled: anioId !== null,
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <Encabezado
        titulo="Solicitudes de matrícula"
        descripcion="Matrículas en línea que envían los acudientes. Revise los documentos y apruebe, devuelva o rechace."
        acciones={
          tieneAlgunRol(['ADMINISTRADOR', 'SECRETARIA']) && (
            <Button variant="outlined" component={RouterLink} to="/solicitudes-matricula/documentos">
              Documentos que se piden
            </Button>
          )
        }
      />
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <SelectorAnio
          valor={anioId}
          alCambiar={(id) => {
            setAnioId(id);
            setPagina(0);
          }}
        />
        <TextField
          select
          size="small"
          label="Estado"
          value={estado}
          onChange={(e) => {
            setEstado(e.target.value as EstadoSolicitud | '');
            setPagina(0);
          }}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">Todas</MenuItem>
          {(['ENVIADA', 'CON_OBSERVACIONES', 'APROBADA', 'RECHAZADA'] as EstadoSolicitud[]).map((e) => (
            <MenuItem key={e} value={e}>
              {NOMBRE_ESTADO_SOLICITUD[e]}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          label="Buscar por nombre o documento"
          value={buscar}
          onChange={(e) => {
            setBuscar(e.target.value);
            setPagina(0);
          }}
          sx={{ minWidth: 320 }}
        />
      </Box>

      {consulta.isError && <Alert severity="error">{mensajeDeError(consulta.error)}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Estudiante</TableCell>
              <TableCell>Grado y sede</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Acudiente</TableCell>
              <TableCell>Enviada</TableCell>
              <TableCell>Estado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {consulta.data?.contenido.map((s) => (
              <TableRow
                key={s.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => navegar(`/solicitudes-matricula/${s.id}`)}
              >
                <TableCell>
                  {s.estudiante}
                  <Box component="div" sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>
                    {s.numeroDocumento}
                  </Box>
                </TableCell>
                <TableCell>
                  {s.grado}, {s.sede}
                </TableCell>
                <TableCell>{s.tipo === 'NUEVO' ? 'Nuevo' : 'Antiguo'}</TableCell>
                <TableCell>{s.solicitante}</TableCell>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{fechaHora(s.fechaEnvio)}</TableCell>
                <TableCell>
                  <Estado tono={TONO_SOLICITUD[s.estado]} texto={NOMBRE_ESTADO_SOLICITUD[s.estado]} />
                  {s.estado === 'ENVIADA' && s.documentosPorRevisar > 0 && (
                    <Box component="div" sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>
                      {s.documentosPorRevisar} {s.documentosPorRevisar === 1 ? 'documento' : 'documentos'} por revisar
                    </Box>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {consulta.data?.contenido.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No hay solicitudes {estado ? `en estado "${NOMBRE_ESTADO_SOLICITUD[estado].toLowerCase()}"` : ''}
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
          rowsPerPageOptions={[25, 50, 100]}
          onPageChange={(_, nueva) => setPagina(nueva)}
          onRowsPerPageChange={(e) => {
            setTamano(Number(e.target.value));
            setPagina(0);
          }}
          labelRowsPerPage="Filas por página"
          labelDisplayedRows={({ from, to, count }) => `${from} a ${to} de ${count}`}
        />
      </TableContainer>
    </>
  );
}
