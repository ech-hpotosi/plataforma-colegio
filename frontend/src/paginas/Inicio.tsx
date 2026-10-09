import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router';
import { obtenerPendientesAsistencia } from '../api/asistencia';
import { obtenerAvance, type AvanceClase } from '../api/seguimiento';
import BarraAvance from '../componentes/BarraAvance';
import Encabezado from '../componentes/Encabezado';
import Estado from '../componentes/Estado';
import { useSesion } from '../sesion/useSesion';
import { COLORES } from '../tema';
import { NOMBRE_ROL, type Rol } from '../tipos';
import { mensajeDeError } from './academico/mensajes';
import {
  LIMITE_DIAS_ASISTENCIA,
  asistenciaAtrasada,
  estadoAsistencia,
  fechaCorta,
  notasCompletas,
  textoNotas,
} from './seguimiento/avance';

const ROLES_ASISTENCIA: Rol[] = ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE'];
const ROLES_SEGUIMIENTO: Rol[] = ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA'];
const MAXIMO_EN_RIESGO = 10;

const FECHA_LARGA = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
/** Dias que faltan para el cierre del periodo, contados desde hoy segun el servidor. */
function diasParaCierre(hoy: string, cierre: string) {
  const fecha = (iso: string) => {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return Date.UTC(anio, mes - 1, dia);
  };
  return Math.round((fecha(cierre) - fecha(hoy)) / 86_400_000);
}

function textoCierre(hoy: string, c: AvanceClase) {
  if (c.periodoCerrado) return `El periodo ${c.periodoNumero} ya está cerrado.`;
  const dias = diasParaCierre(hoy, c.cierrePeriodo);
  const cuando =
    dias < 0
      ? 'ya terminó'
      : dias === 0
        ? 'cierra hoy'
        : `cierra el ${fechaCorta(c.cierrePeriodo)}, en ${dias} ${dias === 1 ? 'día' : 'días'}`;
  return `Periodo ${c.periodoNumero}: ${cuando}.`;
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
  const veSeguimiento = tieneAlgunRol(ROLES_SEGUIMIENTO);
  const pendientes = useQuery({
    queryKey: ['asistencia-pendientes'],
    queryFn: obtenerPendientesAsistencia,
    enabled: veAsistencia,
  });
  // Directivos ven el resumen de todas las clases; el docente, el avance de las suyas
  const avance = useQuery({ queryKey: ['seguimiento-avance', ''], queryFn: () => obtenerAvance(), enabled: veAsistencia });
  const hoy = FECHA_LARGA.format(new Date());

  const contenido = () => {
    if (!veAsistencia) {
      return <Alert severity="info">Por ahora no hay información disponible para su perfil.</Alert>;
    }
    if (pendientes.isPending || avance.isPending) return <CircularProgress />;
    if (pendientes.isError) return <Alert severity="error">{mensajeDeError(pendientes.error)}</Alert>;
    if (avance.isError) return <Alert severity="error">{mensajeDeError(avance.error)}</Alert>;

    const { clasesHoy, faltasPorJustificar, estudiantesEnRiesgo, porcentajeMaximo } = pendientes.data;
    const sinRegistrar = clasesHoy.filter((c) => !c.registrada).length;
    const { hoy: hoyServidor, clases } = avance.data;
    // El docente ve sus clases con pendientes; los directivos, el resumen de todas
    const propias = veSeguimiento ? [] : clases.filter((c) => !notasCompletas(c) || asistenciaAtrasada(c));
    const resumenDirectivos = veSeguimiento && clases.length > 0;
    const completas = clases.filter(notasCompletas).length;
    const atrasadas = clases.filter(asistenciaAtrasada).length;
    const nada =
      clasesHoy.length === 0 &&
      faltasPorJustificar.length === 0 &&
      estudiantesEnRiesgo.length === 0 &&
      propias.length === 0 &&
      !resumenDirectivos;

    if (nada) {
      return (
        <Paper sx={{ px: 2.5, py: 2 }}>
          <Typography sx={{ fontWeight: 600 }}>No tiene pendientes por ahora.</Typography>
          <Typography variant="body2" color="text.secondary">
            Aquí aparecerán las clases sin asistencia, las notas por registrar, las faltas por justificar y los
            estudiantes en riesgo.
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

        {propias.length > 0 && (
          <Seccion
            titulo="Avance del periodo"
            resumen={`${textoCierre(hoyServidor, propias[0])} Clases con notas incompletas o asistencia sin registrar hace más de ${LIMITE_DIAS_ASISTENCIA} días.`}
          >
            {propias.map((c) => {
              const asistencia = estadoAsistencia(c);
              const irAAsistencia = notasCompletas(c);
              return (
                <Fila
                  key={c.cargaId}
                  principal={`${c.grupo} - ${c.asignatura}`}
                  detalle={`${textoNotas(c)} Asistencia: ${asistencia.texto.charAt(0).toLowerCase()}${asistencia.texto.slice(1)}.`}
                  estado={
                    c.cualitativa ? undefined : (
                      <Box sx={{ width: 170 }}>
                        <BarraAvance porcentaje={c.porcentajeNotas} etiqueta={`Notas de ${c.grupo} - ${c.asignatura}`} />
                      </Box>
                    )
                  }
                  accion={
                    <Button
                      component={RouterLink}
                      to={irAAsistencia ? `/asistencia/tomar?carga=${c.cargaId}` : `/notas/planilla?carga=${c.cargaId}`}
                      size="small"
                    >
                      {irAAsistencia ? 'Asistencia' : 'Planilla'}
                    </Button>
                  }
                />
              );
            })}
          </Seccion>
        )}

        {resumenDirectivos && (
          <Seccion titulo="Avance del registro" resumen="Notas y asistencia de todas las clases en el periodo en curso.">
            <Fila
              principal={`Notas completas en ${completas} de ${clases.length} clases`}
              detalle={`Asistencia atrasada en ${atrasadas} ${atrasadas === 1 ? 'clase' : 'clases'} (más de ${LIMITE_DIAS_ASISTENCIA} días sin registrar).`}
              estado={
                <Box sx={{ width: 170 }}>
                  <BarraAvance
                    porcentaje={Math.floor((completas * 100) / clases.length)}
                    etiqueta="Clases con notas completas"
                  />
                </Box>
              }
              accion={
                <Button component={RouterLink} to="/seguimiento" size="small">
                  Ver seguimiento
                </Button>
              }
            />
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
