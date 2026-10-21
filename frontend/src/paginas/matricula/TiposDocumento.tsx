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
  Link,
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
import { Link as RouterLink } from 'react-router';
import {
  actualizarTipoDocumento,
  crearTipoDocumento,
  listarTiposDocumento,
  type DatosTipoDocumento,
  type TipoDocumentoMatricula,
} from '../../api/matricula';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { mensajeDeError } from '../academico/mensajes';

function DialogoTipo({
  tipo,
  siguienteOrden,
  alCerrar,
  alGuardar,
}: {
  tipo: TipoDocumentoMatricula | null;
  siguienteOrden: number;
  alCerrar: () => void;
  alGuardar: () => void;
}) {
  const [datos, setDatos] = useState<DatosTipoDocumento>({
    nombre: tipo?.nombre ?? '',
    descripcion: tipo?.descripcion ?? '',
    obligatorio: tipo?.obligatorio ?? true,
    activo: tipo?.activo ?? true,
    orden: tipo?.orden ?? siguienteOrden,
  });
  const [error, setError] = useState<string | null>(null);

  const guardar = async () => {
    setError(null);
    try {
      if (tipo) await actualizarTipoDocumento(tipo.id, datos);
      else await crearTipoDocumento(datos);
      alGuardar();
    } catch (e) {
      setError(mensajeDeError(e));
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{tipo ? 'Editar documento' : 'Nuevo documento'}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField
            label="Nombre"
            value={datos.nombre}
            onChange={(e) => setDatos({ ...datos, nombre: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 100 } }}
          />
          <TextField
            label="Indicación para el acudiente (opcional)"
            value={datos.descripcion ?? ''}
            onChange={(e) => setDatos({ ...datos, descripcion: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 300 } }}
            multiline
          />
          <TextField
            label="Orden en la lista"
            type="number"
            value={datos.orden}
            onChange={(e) => setDatos({ ...datos, orden: Number(e.target.value) })}
            sx={{ maxWidth: 160 }}
          />
          <FormControlLabel
            control={
              <Checkbox checked={datos.obligatorio} onChange={(e) => setDatos({ ...datos, obligatorio: e.target.checked })} />
            }
            label="Obligatorio para enviar la solicitud"
          />
          <FormControlLabel
            control={<Checkbox checked={datos.activo} onChange={(e) => setDatos({ ...datos, activo: e.target.checked })} />}
            label="Se pide actualmente"
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={!datos.nombre.trim()} onClick={guardar}>
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Lista de documentos que se piden en la matricula en linea. No se borran; se dejan de pedir. */
export default function TiposDocumento() {
  const queryClient = useQueryClient();
  const consulta = useQuery({ queryKey: ['tipos-documento', 'todos'], queryFn: () => listarTiposDocumento(false) });
  const [editando, setEditando] = useState<TipoDocumentoMatricula | null | 'nuevo'>(null);
  const tipos = consulta.data ?? [];

  return (
    <>
      <Typography variant="body2" sx={{ mb: 1 }}>
        <Link component={RouterLink} to="/solicitudes-matricula">
          Volver a las solicitudes
        </Link>
      </Typography>
      <Encabezado
        titulo="Documentos que se piden"
        descripcion="Lo que el acudiente debe subir en la matrícula en línea. Un documento que ya no se pide se desactiva, no se borra."
        acciones={
          <Button variant="contained" onClick={() => setEditando('nuevo')}>
            Agregar documento
          </Button>
        }
      />
      {consulta.isError && <Alert severity="error">{mensajeDeError(consulta.error)}</Alert>}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Documento</TableCell>
              <TableCell>Obligatorio</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {tipos.map((t) => (
              <TableRow key={t.id}>
                <TableCell>
                  {t.nombre}
                  {t.descripcion && (
                    <Typography variant="body2" color="text.secondary">
                      {t.descripcion}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>{t.obligatorio ? 'Sí' : 'No'}</TableCell>
                <TableCell>
                  <Estado tono={t.activo ? 'positivo' : 'neutro'} texto={t.activo ? 'Se pide' : 'No se pide'} />
                </TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setEditando(t)}>
                    Editar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {editando && (
        <DialogoTipo
          tipo={editando === 'nuevo' ? null : editando}
          siguienteOrden={Math.max(0, ...tipos.map((t) => t.orden)) + 1}
          alCerrar={() => setEditando(null)}
          alGuardar={() => {
            queryClient.invalidateQueries({ queryKey: ['tipos-documento'] });
            setEditando(null);
          }}
        />
      )}
    </>
  );
}
