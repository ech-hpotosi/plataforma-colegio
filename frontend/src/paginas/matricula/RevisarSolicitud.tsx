import { useState, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Link,
  MenuItem,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import { Link as RouterLink, useParams } from 'react-router';
import { listarGrupos } from '../../api/academico';
import { NOMBRE_GENERO, NOMBRE_PARENTESCO } from '../../api/estudiantes';
import {
  aprobarSolicitud,
  devolverSolicitud,
  NOMBRE_ESTADO_DOCUMENTO,
  NOMBRE_ESTADO_SOLICITUD,
  NOMBRE_TIPO_SOLICITUD,
  obtenerSolicitud,
  rechazarSolicitud,
  revisarDocumento,
  tamanoLegible,
  urlArchivo,
  type DocumentoSolicitud,
  type Solicitud,
} from '../../api/matricula';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { useSesion } from '../../sesion/useSesion';
import { COLORES } from '../../tema';
import { NOMBRE_TIPO_DOCUMENTO } from '../../tipos';
import { mensajeDeError } from '../academico/mensajes';
import { fechaCorta, fechaHora, TONO_DOCUMENTO, TONO_SOLICITUD } from './estados';

function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <Paper sx={{ mb: 2.5 }}>
      <Typography variant="h6" component="h2" sx={{ px: 2.5, pt: 2, pb: 1 }}>
        {titulo}
      </Typography>
      <Box sx={{ px: 2.5, pb: 2.5 }}>{children}</Box>
    </Paper>
  );
}

function Datos({ filas }: { filas: [string, string | null | undefined][] }) {
  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' }, gap: 1.5 }}>
      {filas.map(([etiqueta, valor]) => (
        <Box key={etiqueta}>
          <Typography variant="caption" color="text.secondary">
            {etiqueta}
          </Typography>
          <Typography>{valor || 'No indicado'}</Typography>
        </Box>
      ))}
    </Box>
  );
}

/** Documento con el enlace para verlo y, mientras la solicitud esta en revision, aprobarlo o rechazarlo. */
function RevisionDocumento({
  solicitud,
  documento,
  puedeRevisar,
  alCambiar,
}: {
  solicitud: Solicitud;
  documento: DocumentoSolicitud;
  puedeRevisar: boolean;
  alCambiar: (s: Solicitud) => void;
}) {
  const [rechazando, setRechazando] = useState(false);
  const [observacion, setObservacion] = useState('');
  const [error, setError] = useState<string | null>(null);

  const revisar = async (estado: 'APROBADO' | 'RECHAZADO') => {
    if (documento.id === null) return;
    setError(null);
    try {
      alCambiar(await revisarDocumento(solicitud.id, documento.id, estado, estado === 'RECHAZADO' ? observacion : ''));
      setRechazando(false);
      setObservacion('');
    } catch (e) {
      setError(mensajeDeError(e));
    }
  };

  return (
    <Box sx={{ py: 1.5, borderTop: `1px solid ${COLORES.linea}` }}>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <Box sx={{ flexGrow: 1, minWidth: 220 }}>
          <Typography sx={{ fontWeight: 600 }}>
            {documento.nombre}
            {!documento.obligatorio && (
              <Typography component="span" variant="body2" color="text.secondary">
                {' '}
                (opcional)
              </Typography>
            )}
          </Typography>
          {documento.id === null ? (
            <Typography variant="body2" color="text.secondary">
              No lo subió
            </Typography>
          ) : (
            <Typography variant="body2">
              <Link href={urlArchivo(solicitud.id, documento.id)} target="_blank" rel="noopener">
                Ver {documento.nombreArchivo}
              </Link>{' '}
              <Typography component="span" variant="body2" color="text.secondary">
                {tamanoLegible(documento.tamanoBytes ?? 0)}, subido el {fechaHora(documento.fechaCarga)}
              </Typography>
            </Typography>
          )}
          {documento.estado && (
            <Box sx={{ mt: 0.5 }}>
              <Estado tono={TONO_DOCUMENTO[documento.estado]} texto={NOMBRE_ESTADO_DOCUMENTO[documento.estado]} />
              {documento.observacion && (
                <Typography variant="body2" color="text.secondary">
                  {documento.observacion}
                </Typography>
              )}
            </Box>
          )}
        </Box>
        {puedeRevisar && documento.id !== null && !rechazando && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              size="small"
              variant={documento.estado === 'APROBADO' ? 'contained' : 'outlined'}
              onClick={() => revisar('APROBADO')}
              aria-label={`Aprobar ${documento.nombre}`}
            >
              Aprobar
            </Button>
            <Button
              size="small"
              color="error"
              variant={documento.estado === 'RECHAZADO' ? 'contained' : 'outlined'}
              onClick={() => setRechazando(true)}
              aria-label={`Rechazar ${documento.nombre}`}
            >
              Rechazar
            </Button>
          </Box>
        )}
      </Box>
      {rechazando && (
        <Box sx={{ display: 'flex', gap: 1, mt: 1, alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <TextField
            size="small"
            label="Qué debe corregir el acudiente"
            value={observacion}
            onChange={(e) => setObservacion(e.target.value)}
            sx={{ flexGrow: 1, minWidth: 240 }}
            slotProps={{ htmlInput: { maxLength: 300 } }}
            autoFocus
          />
          <Button variant="contained" color="error" disabled={!observacion.trim()} onClick={() => revisar('RECHAZADO')}>
            Rechazar documento
          </Button>
          <Button onClick={() => setRechazando(false)}>Cancelar</Button>
        </Box>
      )}
      {error && (
        <Alert severity="error" sx={{ mt: 1 }}>
          {error}
        </Alert>
      )}
    </Box>
  );
}

type Accion = 'aprobar' | 'devolver' | 'rechazar';

/** Aprobar (con grupo opcional), devolver con observaciones o rechazar. */
function DialogoAccion({
  accion,
  solicitud,
  alCerrar,
  alTerminar,
}: {
  accion: Accion;
  solicitud: Solicitud;
  alCerrar: () => void;
  alTerminar: (s: Solicitud) => void;
}) {
  const [comentario, setComentario] = useState('');
  const [grupoId, setGrupoId] = useState<number | ''>('');
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const grupos = useQuery({
    queryKey: ['grupos', solicitud.anioLectivoId, solicitud.sedeId],
    queryFn: () => listarGrupos(solicitud.anioLectivoId, solicitud.sedeId),
    enabled: accion === 'aprobar',
  });
  const delGrado = (grupos.data ?? []).filter((g) => g.gradoId === solicitud.gradoId);

  const confirmar = async () => {
    setError(null);
    setGuardando(true);
    try {
      const resultado =
        accion === 'aprobar'
          ? await aprobarSolicitud(solicitud.id, grupoId === '' ? null : grupoId)
          : accion === 'devolver'
            ? await devolverSolicitud(solicitud.id, comentario)
            : await rechazarSolicitud(solicitud.id, comentario);
      alTerminar(resultado);
    } catch (e) {
      setError(mensajeDeError(e));
      setGuardando(false);
    }
  };

  const titulos: Record<Accion, string> = {
    aprobar: 'Aprobar y matricular',
    devolver: 'Devolver para corregir',
    rechazar: 'Rechazar solicitud',
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{titulos[accion]}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {accion === 'aprobar' ? (
          <>
            <Typography variant="body2" sx={{ mb: 2 }}>
              Se crea o actualiza la ficha de {solicitud.nombres} {solicitud.apellidos}, queda matriculado en{' '}
              {solicitud.anio} y {solicitud.solicitante.nombre} queda como acudiente con acceso a notas y asistencia.
            </Typography>
            <TextField
              select
              fullWidth
              label="Grupo (opcional)"
              value={grupoId}
              onChange={(e) => setGrupoId(e.target.value === '' ? '' : Number(e.target.value))}
              helperText={
                grupos.isSuccess && delGrado.length === 0
                  ? `No hay grupos de ${solicitud.grado} en ${solicitud.sede}. Puede asignarlo después en la ficha.`
                  : 'Si no lo elige ahora, lo puede asignar después en la ficha del estudiante.'
              }
            >
              <MenuItem value="">Sin grupo por ahora</MenuItem>
              {delGrado.map((g) => (
                <MenuItem key={g.id} value={g.id}>
                  {g.grado} {g.nombre}
                </MenuItem>
              ))}
            </TextField>
          </>
        ) : (
          <TextField
            fullWidth
            multiline
            minRows={3}
            label={accion === 'devolver' ? 'Qué debe corregir el acudiente' : 'Motivo del rechazo'}
            value={comentario}
            onChange={(e) => setComentario(e.target.value)}
            helperText="El acudiente verá este mensaje en su solicitud."
            slotProps={{ htmlInput: { maxLength: 500 } }}
            sx={{ mt: 1 }}
          />
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button
          variant="contained"
          color={accion === 'rechazar' ? 'error' : 'primary'}
          disabled={guardando || (accion !== 'aprobar' && !comentario.trim())}
          onClick={confirmar}
        >
          {titulos[accion]}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Detalle de una solicitud para la secretaria. */
export default function RevisarSolicitud() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const { tieneAlgunRol } = useSesion();
  const consulta = useQuery({ queryKey: ['solicitud', id], queryFn: () => obtenerSolicitud(id) });
  const [accion, setAccion] = useState<Accion | null>(null);

  if (consulta.isPending) return <CircularProgress />;
  if (consulta.isError) return <Alert severity="error">{mensajeDeError(consulta.error)}</Alert>;
  const s = consulta.data;
  const puedeRevisar = s.estado === 'ENVIADA' && tieneAlgunRol(['ADMINISTRADOR', 'SECRETARIA']);
  const hayRechazados = s.documentos.some((d) => d.estado === 'RECHAZADO');

  const actualizar = (nueva: Solicitud) => {
    queryClient.setQueryData(['solicitud', id], nueva);
    queryClient.invalidateQueries({ queryKey: ['solicitudes'] });
  };

  return (
    <>
      <Typography variant="body2" sx={{ mb: 1 }}>
        <Link component={RouterLink} to="/solicitudes-matricula">
          Volver a las solicitudes
        </Link>
      </Typography>
      <Encabezado
        titulo={`${s.nombres} ${s.apellidos}`}
        descripcion={`Matrícula ${s.anio}: ${s.grado}, ${s.sede}. ${NOMBRE_TIPO_SOLICITUD[s.tipo]}.`}
        acciones={
          puedeRevisar ? (
            <>
              <Button variant="outlined" onClick={() => setAccion('devolver')}>
                Devolver
              </Button>
              <Button variant="outlined" color="error" onClick={() => setAccion('rechazar')}>
                Rechazar
              </Button>
              <Button variant="contained" disabled={hayRechazados} onClick={() => setAccion('aprobar')}>
                Aprobar
              </Button>
            </>
          ) : (
            <Estado tono={TONO_SOLICITUD[s.estado]} texto={NOMBRE_ESTADO_SOLICITUD[s.estado]} />
          )
        }
      />
      {puedeRevisar && hayRechazados && (
        <Alert severity="info" sx={{ mb: 2.5 }}>
          Hay documentos rechazados. Devuelva la solicitud para que el acudiente los cambie.
        </Alert>
      )}
      {s.estado === 'APROBADA' && s.estudianteId !== null && (
        <Alert severity="success" sx={{ mb: 2.5 }}>
          Matriculado{s.grupo ? ` en ${s.grupo}` : ' sin grupo asignado'}.{' '}
          <Link component={RouterLink} to={`/estudiantes/${s.estudianteId}`}>
            Abrir la ficha del estudiante
          </Link>
        </Alert>
      )}
      {(s.estado === 'CON_OBSERVACIONES' || s.estado === 'RECHAZADA') && s.observacion && (
        <Alert severity={s.estado === 'RECHAZADA' ? 'error' : 'warning'} sx={{ mb: 2.5 }}>
          {s.estado === 'RECHAZADA' ? 'Rechazada: ' : 'Devuelta al acudiente: '}
          {s.observacion}
        </Alert>
      )}

      <Seccion titulo="Estudiante">
        <Datos
          filas={[
            ['Documento', `${NOMBRE_TIPO_DOCUMENTO[s.tipoDocumento]} ${s.numeroDocumento}`],
            ['Fecha de nacimiento', fechaCorta(s.fechaNacimiento)],
            ['Género', NOMBRE_GENERO[s.genero]],
            ['Dirección', s.direccion],
            ['Teléfono', s.telefono],
            ['Correo', s.correo],
            ['EPS o SISBÉN', s.eps],
            ['Grupo sanguíneo', s.grupoSanguineo],
            ['Discapacidad o diagnóstico', s.condicionDiscapacidad],
            ['Otra información', s.condicionesEspeciales],
            ...(s.tipo === 'NUEVO' ? ([['Institución anterior', s.institucionAnterior]] as [string, string | null][]) : []),
          ]}
        />
      </Seccion>

      <Seccion titulo="Acudiente que hace la solicitud">
        <Datos
          filas={[
            ['Nombre', s.solicitante.nombre],
            ['Parentesco', NOMBRE_PARENTESCO[s.parentesco]],
            ['Documento', `${s.solicitante.tipoDocumento} ${s.solicitante.numeroDocumento}`],
            ['Celular', s.solicitante.telefono],
            ['Correo', s.solicitante.correo],
            ['Autorizó tratamiento de datos', s.autorizaDatos ? 'Sí' : 'No'],
          ]}
        />
      </Seccion>

      <Seccion titulo="Documentos">
        {s.documentos.map((d) => (
          <RevisionDocumento
            key={d.tipoDocumentoId}
            solicitud={s}
            documento={d}
            puedeRevisar={puedeRevisar}
            alCambiar={actualizar}
          />
        ))}
      </Seccion>

      <Seccion titulo="Historial">
        {s.revisiones.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            Sin movimientos.
          </Typography>
        )}
        {s.revisiones.map((r, i) => (
          <Box key={i} sx={{ py: 1, borderTop: i === 0 ? 'none' : `1px solid ${COLORES.linea}` }}>
            <Typography variant="body2">
              <strong>{NOMBRE_ESTADO_SOLICITUD[r.estadoNuevo]}</strong>, {fechaHora(r.fecha)}, por {r.usuario}
            </Typography>
            {r.comentario && (
              <Typography variant="body2" color="text.secondary">
                {r.comentario}
              </Typography>
            )}
          </Box>
        ))}
      </Seccion>

      {accion && (
        <DialogoAccion
          accion={accion}
          solicitud={s}
          alCerrar={() => setAccion(null)}
          alTerminar={(nueva) => {
            actualizar(nueva);
            setAccion(null);
          }}
        />
      )}
    </>
  );
}
