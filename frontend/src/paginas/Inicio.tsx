import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router';
import { obtenerPendientesAsistencia } from '../api/asistencia';
import Encabezado from '../componentes/Encabezado';
import Estado from '../componentes/Estado';
import { useSesion } from '../sesion/useSesion';
import { COLORES } from '../tema';
import { NOMBRE_ROL, type Rol } from '../tipos';
import { mensajeDeError } from './academico/mensajes';

const ROLES_ASISTENCIA: Rol[] = ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE'];
const MAXIMO_EN_RIESGO = 10;

const FECHA_LARGA = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
const FECHA_CORTA = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', month: 'short' });

/** Fecha ISO (aaaa-mm-dd) en la hora local, sin que el cambio a UTC la corra un dia. */
function fechaCorta(iso: string) {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FECHA_CORTA.format(new Date(anio, mes - 1, dia));
}

/** Bloque del inicio: titulo con un resumen corto y filas separadas por una linea. */
function Seccion({ titulo, resumen, children }: { titulo: string; resumen: ReactNode; children: ReactNode }) {
  return (
    <Paper>
      <Box sx={{ px: 2.5, pt: 2, pb: 1.5 }}>
        <Typography variant="h6" component="h2">
          {titulo}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {resumen}
        </Typography>
      </Box>
      {children}
    </Paper>
  );
}

function Fila({ principal, detalle, estado, accion }: { principal: string; detalle: string; estado?: ReactNode; accion: ReactNode }) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap',
        columnGap: 2,
        rowGap: 0.5,
        px: 2.5,
        py: 1.25,
        borderTop: `1px solid ${COLORES.linea}`,
      }}
    >
      <Box sx={{ flex: '1 1 220px', minWidth: 0 }}>
        <Typography sx={{ fontWeight: 600 }}>{principal}</Typography>
        <Typography variant="body2" color="text.secondary">
          {detalle}
        </Typography>
      </Box>
      {estado && <Box sx={{ flex: '0 0 auto', typography: 'body2' }}>{estado}</Box>}
      <Box sx={{ flex: '0 0 auto', ml: 'auto' }}>{accion}</Box>
    </Box>
  );
}

/**
 * Pantalla de inicio: muestra lo que cada usuario tiene pendiente, no repite el menu.
 * Hoy los pendientes salen de asistencia; con notas y matricula en linea se agregan sus secciones.
 */
export default function Inicio() {
  const { usuario, tieneAlgunRol } = useSesion();
  const veAsistencia = tieneAlgunRol(ROLES_ASISTENCIA);
  const pendientes = useQuery({
    queryKey: ['asistencia-pendientes'],
    queryFn: obtenerPendientesAsistencia,
    enabled: veAsistencia,
  });
  const hoy = FECHA_LARGA.format(new Date());

  const contenido = () => {
    if (!veAsistencia) {
      return <Alert severity="info">Por ahora no hay información disponible para su perfil.</Alert>;
    }
    if (pendientes.isPending) return <CircularProgress />;
    if (pendientes.isError) return <Alert severity="error">{mensajeDeError(pendientes.error)}</Alert>;

    const { clasesHoy, faltasPorJustificar, estudiantesEnRiesgo, porcentajeMaximo } = pendientes.data;
    const sinRegistrar = clasesHoy.filter((c) => !c.registrada).length;
    const nada = clasesHoy.length === 0 && faltasPorJustificar.length === 0 && estudiantesEnRiesgo.length === 0;

    if (nada) {
      return (
        <Paper sx={{ px: 2.5, py: 2 }}>
          <Typography sx={{ fontWeight: 600 }}>No tiene pendientes por ahora.</Typography>
          <Typography variant="body2" color="text.secondary">
            Aquí aparecerán las clases sin asistencia, las faltas por justificar y los estudiantes en riesgo.
          </Typography>
        </Paper>
      );
    }

    return (
      <Stack spacing={2.5}>
        {clasesHoy.length > 0 && (
          <Seccion
            titulo="Asistencia de hoy"
            resumen={
              sinRegistrar === 0
                ? 'Ya registró la asistencia de todas sus clases.'
                : `Le falta registrar ${sinRegistrar} de ${clasesHoy.length} ${clasesHoy.length === 1 ? 'clase' : 'clases'}. Registre solo las que dictó hoy.`
            }
          >
            {clasesHoy.map((c) => (
              <Fila
                key={c.cargaId}
                principal={`${c.grupo} - ${c.asignatura}`}
                detalle={c.sede}
                estado={<Estado tono={c.registrada ? 'positivo' : 'neutro'} texto={c.registrada ? 'Registrada' : 'Sin registrar'} />}
                accion={
                  <Button component={RouterLink} to={`/asistencia/tomar?carga=${c.cargaId}`} size="small">
                    {c.registrada ? 'Corregir' : 'Tomar'}
                  </Button>
                }
              />
            ))}
          </Seccion>
        )}

        {faltasPorJustificar.length > 0 && (
          <Seccion
            titulo="Faltas por justificar"
            resumen="Faltas sin justificar que todavía están dentro del plazo del SIEE."
          >
            {faltasPorJustificar.map((f) => (
              <Fila
                key={`${f.matriculaId}-${f.fecha}`}
                principal={f.estudiante}
                detalle={`${f.grupo}. Faltó el ${fechaCorta(f.fecha)}, ${f.horas} ${f.horas === 1 ? 'hora' : 'horas'}.`}
                estado={<Estado tono="alerta" texto={`Plazo hasta el ${fechaCorta(f.fechaLimite)}`} />}
                accion={
                  <Button component={RouterLink} to={`/asistencia/consolidado?grupo=${f.grupoId}`} size="small">
                    Ver grupo
                  </Button>
                }
              />
            ))}
          </Seccion>
        )}

        {estudiantesEnRiesgo.length > 0 && (
          <Seccion
            titulo="Estudiantes en riesgo por inasistencia"
            resumen={`Según el SIEE se pierde la asignatura con más del ${porcentajeMaximo} % de inasistencia sin justificar.`}
          >
            {estudiantesEnRiesgo.slice(0, MAXIMO_EN_RIESGO).map((e) => (
              <Fila
                key={e.matriculaId}
                principal={e.estudiante}
                detalle={`${e.grupo}. ${e.asignatura}: ${e.porcentaje.toFixed(1)} %`}
                estado={
                  <Estado tono={e.superaLimite ? 'negativo' : 'alerta'} texto={e.superaLimite ? 'Supera el límite' : 'Cerca del límite'} />
                }
                accion={
                  <Button component={RouterLink} to={`/asistencia/consolidado?grupo=${e.grupoId}`} size="small">
                    Ver grupo
                  </Button>
                }
              />
            ))}
            {estudiantesEnRiesgo.length > MAXIMO_EN_RIESGO && (
              <Typography variant="body2" color="text.secondary" sx={{ px: 2.5, py: 1.25, borderTop: `1px solid ${COLORES.linea}` }}>
                Y {estudiantesEnRiesgo.length - MAXIMO_EN_RIESGO} estudiantes más. Revise el consolidado de cada grupo.
              </Typography>
            )}
          </Seccion>
        )}
      </Stack>
    );
  };

  return (
    <>
      <Encabezado
        titulo={`Bienvenido, ${usuario?.nombreCompleto ?? ''}`}
        descripcion={`${hoy.charAt(0).toUpperCase()}${hoy.slice(1)}. ${usuario?.roles.map((r) => NOMBRE_ROL[r]).join(', ') ?? ''}`}
      />
      <Box sx={{ maxWidth: 900 }}>{contenido()}</Box>
    </>
  );
}
