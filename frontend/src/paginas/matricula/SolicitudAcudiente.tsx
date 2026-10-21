import { useRef, useState, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Link,
  Paper,
  Typography,
} from '@mui/material';
import { Link as RouterLink, useParams } from 'react-router';
import { NOMBRE_GENERO, NOMBRE_PARENTESCO } from '../../api/estudiantes';
import {
  consultarOferta,
  enviarSolicitud,
  NOMBRE_ESTADO_DOCUMENTO,
  NOMBRE_ESTADO_SOLICITUD,
  NOMBRE_TIPO_SOLICITUD,
  obtenerSolicitud,
  quitarDocumento,
  subirDocumento,
  tamanoLegible,
  urlArchivo,
  type DocumentoSolicitud,
  type OfertaMatricula,
  type Solicitud,
} from '../../api/matricula';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { COLORES } from '../../tema';
import { NOMBRE_TIPO_DOCUMENTO } from '../../tipos';
import { mensajeDeError } from '../academico/mensajes';
import FormularioSolicitud from './FormularioSolicitud';
import { fechaCorta, TONO_DOCUMENTO, TONO_SOLICITUD } from './estados';

const TAMANO_MAXIMO = 5 * 1024 * 1024;

/** Bloque numerado de la solicitud: 1 datos, 2 documentos, 3 envio. */
function Paso({ numero, titulo, children }: { numero: number; titulo: string; children: ReactNode }) {
  return (
    <Paper sx={{ mb: 2.5 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 2.5, pt: 2, pb: 1 }}>
        <Box
          sx={{
            width: 28,
            height: 28,
            borderRadius: '50%',
            bgcolor: COLORES.laguna,
            color: '#fff',
            display: 'grid',
            placeItems: 'center',
            fontWeight: 700,
            fontSize: '0.9rem',
            flexShrink: 0,
          }}
        >
          {numero}
        </Box>
        <Typography variant="h6" component="h2">
          {titulo}
        </Typography>
      </Box>
      <Box sx={{ px: 2.5, pb: 2.5 }}>{children}</Box>
    </Paper>
  );
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string | null | undefined }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {etiqueta}
      </Typography>
      <Typography>{valor || 'No indicado'}</Typography>
    </Box>
  );
}

/** Fila de un documento con sus botones para subir, cambiar, ver o quitar el archivo. */
function FilaDocumento({
  solicitud,
  documento,
  alCambiar,
}: {
  solicitud: Solicitud;
  documento: DocumentoSolicitud;
  alCambiar: (s: Solicitud) => void;
}) {
  const entrada = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [subiendo, setSubiendo] = useState(false);

  const elegir = async (archivo: File | undefined) => {
    if (!archivo) return;
    setError(null);
    if (archivo.size > TAMANO_MAXIMO) {
      setError('El archivo pesa más de 5 MB. Tome la foto con menor resolución o reduzca el PDF.');
      return;
    }
    setSubiendo(true);
    try {
      alCambiar(await subirDocumento(solicitud.id, documento.tipoDocumentoId, archivo));
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setSubiendo(false);
      if (entrada.current) entrada.current.value = '';
    }
  };

  const quitar = async () => {
    setError(null);
    try {
      alCambiar(await quitarDocumento(solicitud.id, documento.tipoDocumentoId));
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
          {documento.descripcion && (
            <Typography variant="body2" color="text.secondary">
              {documento.descripcion}
            </Typography>
          )}
          {documento.id !== null && (
            <Typography variant="body2" sx={{ mt: 0.5 }}>
              <Link href={urlArchivo(solicitud.id, documento.id)} target="_blank" rel="noopener">
                {documento.nombreArchivo}
              </Link>{' '}
              <Typography component="span" variant="body2" color="text.secondary">
                {tamanoLegible(documento.tamanoBytes ?? 0)}
              </Typography>
            </Typography>
          )}
          {documento.estado && solicitud.estado !== 'BORRADOR' && (
            <Box sx={{ mt: 0.5 }}>
              <Estado tono={TONO_DOCUMENTO[documento.estado]} texto={NOMBRE_ESTADO_DOCUMENTO[documento.estado]} />
              {documento.observacion && (
                <Typography variant="body2" color="error">
                  {documento.observacion}
                </Typography>
              )}
            </Box>
          )}
        </Box>
        {solicitud.editable && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <input
              ref={entrada}
              type="file"
              hidden
              accept="application/pdf,image/jpeg,image/png"
              aria-label={`Archivo para ${documento.nombre}`}
              onChange={(e) => elegir(e.target.files?.[0])}
            />
            <Button
              variant={documento.id === null ? 'contained' : 'outlined'}
              size="small"
              disabled={subiendo}
              onClick={() => entrada.current?.click()}
            >
              {subiendo ? 'Subiendo...' : documento.id === null ? 'Subir archivo' : 'Cambiar'}
            </Button>
            {documento.id !== null && (
              <Button size="small" color="inherit" onClick={quitar}>
                Quitar
              </Button>
            )}
          </Box>
        )}
      </Box>
      {error && (
        <Alert severity="error" sx={{ mt: 1 }}>
          {error}
        </Alert>
      )}
    </Box>
  );
}

/** Solicitud del acudiente: datos, documentos y envio a la secretaria. */
export default function SolicitudAcudiente() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const consulta = useQuery({ queryKey: ['solicitud', id], queryFn: () => obtenerSolicitud(id) });
  const oferta = useQuery({ queryKey: ['oferta-matricula'], queryFn: consultarOferta });
  const [editando, setEditando] = useState(false);
  const [autoriza, setAutoriza] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (consulta.isPending) return <CircularProgress />;
  if (consulta.isError) return <Alert severity="error">{mensajeDeError(consulta.error)}</Alert>;
  const s = consulta.data;

  const actualizar = (nueva: Solicitud) => {
    queryClient.setQueryData(['solicitud', id], nueva);
    queryClient.invalidateQueries({ queryKey: ['mis-solicitudes'] });
  };

  // Para corregir los datos se usa la oferta abierta; si ya cerro, al menos la sede y el grado elegidos
  const ofertaDeLaSolicitud: OfertaMatricula = oferta.data?.find((o) => o.anioLectivoId === s.anioLectivoId) ?? {
    anioLectivoId: s.anioLectivoId,
    anio: s.anio,
    sedes: [{ id: s.sedeId, nombre: s.sede }],
    grados: [{ id: s.gradoId, nombre: s.grado }],
  };
  const faltantes = s.documentos.filter((d) => d.obligatorio && d.id === null);
  const rechazados = s.documentos.filter((d) => d.estado === 'RECHAZADO');

  const enviar = async () => {
    setError(null);
    setEnviando(true);
    try {
      actualizar(await enviarSolicitud(s.id));
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <>
      <Typography variant="body2" sx={{ mb: 1 }}>
        <Link component={RouterLink} to="/matricula-en-linea">
          Volver a mis solicitudes
        </Link>
      </Typography>
      <Encabezado
        titulo={`${s.nombres} ${s.apellidos}`}
        descripcion={`Solicitud de matrícula ${s.anio}: ${s.grado}, ${s.sede}.`}
        acciones={<Estado tono={TONO_SOLICITUD[s.estado]} texto={NOMBRE_ESTADO_SOLICITUD[s.estado]} />}
      />

      {s.estado === 'ENVIADA' && (
        <Alert severity="info" sx={{ mb: 2.5 }}>
          La secretaría está revisando la solicitud. Si hay que corregir algo, lo verá aquí.
        </Alert>
      )}
      {s.estado === 'CON_OBSERVACIONES' && (
        <Alert severity="warning" sx={{ mb: 2.5 }}>
          <strong>La secretaría pide corregir:</strong> {s.observacion}
          <br />
          Haga los cambios y vuelva a enviar la solicitud al final de esta página.
        </Alert>
      )}
      {s.estado === 'RECHAZADA' && (
        <Alert severity="error" sx={{ mb: 2.5 }}>
          <strong>La solicitud fue rechazada:</strong> {s.observacion}
        </Alert>
      )}
      {s.estado === 'APROBADA' && (
        <Alert severity="success" sx={{ mb: 2.5 }}>
          El estudiante quedó matriculado para el año {s.anio}
          {s.grupo ? ` en ${s.grupo}` : ''}.
        </Alert>
      )}

      <Paso numero={1} titulo="Datos del estudiante">
        {editando ? (
          <FormularioSolicitud
            oferta={ofertaDeLaSolicitud}
            solicitud={s}
            alCancelar={() => setEditando(false)}
            alGuardar={(nueva) => {
              actualizar(nueva);
              setEditando(false);
            }}
          />
        ) : (
          <>
            <Box
              sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' }, gap: 1.5 }}
            >
              <Dato
                etiqueta="Documento"
                valor={`${NOMBRE_TIPO_DOCUMENTO[s.tipoDocumento]} ${s.numeroDocumento}`}
              />
              <Dato etiqueta="Fecha de nacimiento" valor={fechaCorta(s.fechaNacimiento)} />
              <Dato etiqueta="Género" valor={NOMBRE_GENERO[s.genero]} />
              <Dato etiqueta="Dirección" valor={s.direccion} />
              <Dato etiqueta="EPS o SISBÉN" valor={s.eps} />
              <Dato etiqueta="Estudiante" valor={NOMBRE_TIPO_SOLICITUD[s.tipo]} />
              <Dato etiqueta="Parentesco de quien matricula" valor={NOMBRE_PARENTESCO[s.parentesco]} />
              {s.tipo === 'NUEVO' && <Dato etiqueta="Institución anterior" valor={s.institucionAnterior} />}
            </Box>
            {s.editable && (
              <Button variant="outlined" size="small" sx={{ mt: 2 }} onClick={() => setEditando(true)}>
                Corregir datos
              </Button>
            )}
          </>
        )}
      </Paso>

      <Paso numero={2} titulo="Documentos">
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          Suba una foto clara o un PDF de cada documento, de máximo 5 MB.
        </Typography>
        {s.documentos.map((d) => (
          <FilaDocumento key={d.tipoDocumentoId} solicitud={s} documento={d} alCambiar={actualizar} />
        ))}
      </Paso>

      {s.editable && (
        <Paso numero={3} titulo="Enviar a la secretaría">
          {faltantes.length > 0 && (
            <Typography variant="body2" sx={{ mb: 1.5 }}>
              Falta subir: {faltantes.map((d) => d.nombre).join(', ')}.
            </Typography>
          )}
          {rechazados.length > 0 && (
            <Typography variant="body2" sx={{ mb: 1.5 }}>
              Cambie los documentos rechazados: {rechazados.map((d) => d.nombre).join(', ')}.
            </Typography>
          )}
          <FormControlLabel
            control={<Checkbox checked={autoriza} onChange={(e) => setAutoriza(e.target.checked)} />}
            label={
              <Typography variant="body2">
                Autorizo a la Institución Educativa Municipal El Encano a tratar mis datos personales y los del
                estudiante para la matrícula y la gestión académica, según la Ley 1581 de 2012. Declaro que la
                información y los documentos son verdaderos.
              </Typography>
            }
            sx={{ alignItems: 'flex-start', '& .MuiCheckbox-root': { pt: 0.25 } }}
          />
          {error && (
            <Alert severity="error" sx={{ mt: 1.5 }}>
              {error}
            </Alert>
          )}
          <Box sx={{ mt: 2 }}>
            <Button
              variant="contained"
              disabled={!autoriza || enviando || faltantes.length > 0 || rechazados.length > 0}
              onClick={enviar}
            >
              {s.estado === 'CON_OBSERVACIONES' ? 'Volver a enviar' : 'Enviar solicitud'}
            </Button>
          </Box>
        </Paso>
      )}
    </>
  );
}
