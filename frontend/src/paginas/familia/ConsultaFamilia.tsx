import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import {
  descargarBoletinFamilia,
  listarEstudiantesFamilia,
  obtenerResumenFamilia,
  type AsignaturaFamilia,
  type EstudianteFamilia,
  type ResumenFamilia,
} from '../../api/familia';
import { NOMBRE_ESTADO_ASISTENCIA } from '../../api/asistencia';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';
import EstadoDesempeno from '../notas/Desempeno';

const FECHA = new Intl.DateTimeFormat('es-CO', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
const nota = (valor: number | null) => (valor === null ? '-' : valor.toFixed(1));

/**
 * Consulta de las familias: notas por periodo, faltas y boletines de los estudiantes que el acudiente tiene a cargo
 * (o del propio estudiante). Pensada primero para el celular.
 */
export default function ConsultaFamilia() {
  const estudiantes = useQuery({ queryKey: ['familia-estudiantes'], queryFn: listarEstudiantesFamilia });
  const [elegido, setElegido] = useState<number | null>(null);

  if (estudiantes.isPending) return <CircularProgress />;
  if (estudiantes.isError) return <Alert severity="error">{mensajeDeError(estudiantes.error)}</Alert>;

  const lista = estudiantes.data;
  const actual = lista.find((e) => e.id === elegido) ?? lista[0];

  return (
    <>
      <Encabezado titulo="Notas y asistencia" descripcion="Notas de cada periodo, faltas y boletines." />
      {lista.length === 0 ? (
        <Paper sx={{ px: 2.5, py: 2, maxWidth: 680 }}>
          <Typography sx={{ fontWeight: 600, mb: 0.5 }}>Todavía no tiene estudiantes para consultar</Typography>
          <Typography variant="body2" color="text.secondary">
            La consulta se activa cuando secretaría aprueba la matrícula en línea que usted hizo. Si su acudido ya
            estaba matriculado, pida el acceso en la secretaría del colegio con su documento de identidad.
          </Typography>
        </Paper>
      ) : (
        <>
          {lista.length > 1 && (
            <ToggleButtonGroup
              exclusive
              size="small"
              value={actual.id}
              onChange={(_, id: number | null) => id !== null && setElegido(id)}
              sx={{ mb: 2, flexWrap: 'wrap' }}
            >
              {lista.map((e) => (
                <ToggleButton key={e.id} value={e.id} sx={{ textTransform: 'none', px: 2 }}>
                  {e.nombres} {e.apellidos}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
          )}
          <Estudiante key={actual.id} estudiante={actual} />
        </>
      )}
    </>
  );
}

function Estudiante({ estudiante }: { estudiante: EstudianteFamilia }) {
  const [matriculaId, setMatriculaId] = useState<number | null>(estudiante.matriculas[0]?.id ?? null);
  const resumen = useQuery({
    queryKey: ['familia-resumen', estudiante.id, matriculaId],
    queryFn: () => obtenerResumenFamilia(estudiante.id, matriculaId),
    enabled: matriculaId !== null,
  });

  if (matriculaId === null) {
    return (
      <Alert severity="info">
        {estudiante.nombres} no tiene una matrícula activa. Si cree que es un error, comuníquese con la secretaría.
      </Alert>
    );
  }

  return (
    <>
      {estudiante.matriculas.length > 1 && (
        <TextField
          select
          size="small"
          label="Año lectivo"
          value={matriculaId}
          onChange={(e) => setMatriculaId(Number(e.target.value))}
          sx={{ mb: 2, minWidth: 160 }}
        >
          {estudiante.matriculas.map((m) => (
            <MenuItem key={m.id} value={m.id}>
              {m.anio}
            </MenuItem>
          ))}
        </TextField>
      )}
      {resumen.isPending && <CircularProgress />}
      {resumen.isError && <Alert severity="error">{mensajeDeError(resumen.error)}</Alert>}
      {resumen.data && <Resumen datos={resumen.data} />}
    </>
  );
}

function Resumen({ datos }: { datos: ResumenFamilia }) {
  const boletin = useMutation({
    mutationFn: (periodoId: number) => descargarBoletinFamilia(datos.estudianteId, datos.matriculaId, periodoId),
  });
  const cerrados = datos.periodos.filter((p) => p.cerrado);
  const abierto = datos.periodos.some((p) => !p.cerrado);
  const faltasSinJustificar = datos.asignaturas.reduce((total, a) => total + a.faltasSinJustificar, 0);

  return (
    <>
      <Paper sx={{ px: 2.5, py: 2, mb: 2 }}>
        <Typography variant="h6" component="h2">
          {datos.estudiante}
        </Typography>
        <Typography color="text.secondary">
          {datos.grupo ? `${datos.grupo} · ${datos.sede} · Año ${datos.anio}` : `Año ${datos.anio} · Sin grupo asignado`}
        </Typography>
        {datos.director && (
          <Typography variant="body2" color="text.secondary">
            Director de grupo: {datos.director}
          </Typography>
        )}
        {datos.boletinDisponible && datos.periodos.length > 0 && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mt: 1.5 }}>
            {cerrados.map((p) => (
              <Button
                key={p.id}
                size="small"
                variant="outlined"
                disabled={boletin.isPending}
                onClick={() => boletin.mutate(p.id)}
              >
                Boletín periodo {p.numero}
              </Button>
            ))}
            {cerrados.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                El boletín de cada periodo se puede descargar aquí cuando el colegio cierre el periodo.
              </Typography>
            )}
          </Box>
        )}
        {boletin.isError && (
          <Alert severity="error" sx={{ mt: 1.5 }}>
            {mensajeDeError(boletin.error)}
          </Alert>
        )}
      </Paper>

      {datos.grupo && (
        <>
          <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 2, flexWrap: 'wrap', mb: 1 }}>
            <Typography variant="h6" component="h2">
              Notas
            </Typography>
            {abierto && (
              <Typography variant="body2" color="text.secondary">
                Las notas de un periodo que no ha cerrado son parciales y pueden cambiar.
              </Typography>
            )}
          </Box>
          {datos.asignaturas.length === 0 ? (
            <Typography color="text.secondary" sx={{ mb: 3 }}>
              El grupo todavía no tiene asignaturas.
            </Typography>
          ) : (
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
                gap: 1.5,
                mb: 3,
              }}
            >
              {datos.asignaturas.map((a) => (
                <TarjetaAsignatura key={a.nombre} asignatura={a} datos={datos} />
              ))}
            </Box>
          )}
        </>
      )}

      <Typography variant="h6" component="h2" sx={{ mb: 1 }}>
        Faltas y retardos
      </Typography>
      {datos.novedades.length === 0 ? (
        <Typography color="text.secondary">No tiene faltas ni retardos registrados este año.</Typography>
      ) : (
        <>
          {faltasSinJustificar > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Las faltas sin justificar se justifican en la secretaría o con el director de grupo dentro del plazo del
              SIEE.
            </Typography>
          )}
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Fecha</TableCell>
                  <TableCell>Asignatura</TableCell>
                  <TableCell>Estado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {datos.novedades.map((n) => (
                  <TableRow key={n.detalleId}>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{FECHA.format(new Date(n.fecha))}</TableCell>
                    <TableCell>
                      {n.asignatura}
                      <Typography variant="body2" color="text.secondary">
                        {n.horas === 1 ? '1 hora' : `${n.horas} horas`}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Estado
                        tono={n.estado === 'FALTA' ? 'negativo' : n.estado === 'RETARDO' ? 'alerta' : 'neutro'}
                        texto={n.estado === 'FALTA' ? 'Sin justificar' : NOMBRE_ESTADO_ASISTENCIA[n.estado]}
                      />
                      {n.justificacion && (
                        <Typography variant="body2" color="text.secondary">
                          {n.justificacion}
                        </Typography>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </>
  );
}

function TarjetaAsignatura({ asignatura: a, datos }: { asignatura: AsignaturaFamilia; datos: ResumenFamilia }) {
  const faltas = [
    a.faltasSinJustificar > 0 && `${a.faltasSinJustificar} h sin justificar`,
    a.faltasJustificadas > 0 && `${a.faltasJustificadas} h justificadas`,
    a.retardos > 0 && `${a.retardos} h con retardo`,
  ].filter(Boolean);
  return (
    <Paper sx={{ px: 2, py: 1.5 }}>
      <Typography sx={{ fontWeight: 600 }}>{a.nombre}</Typography>
      {a.docente && (
        <Typography variant="body2" color="text.secondary">
          {a.docente}
        </Typography>
      )}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: `repeat(${datos.periodos.length + 1}, minmax(0, 1fr))`,
          gap: 1,
          mt: 1.25,
          pt: 1.25,
          borderTop: `1px solid ${COLORES.linea}`,
        }}
      >
        {datos.periodos.map((p, i) => {
          const n = a.periodos[i];
          return (
            <Box key={p.id}>
              <Typography variant="caption" color="text.secondary">
                Periodo {p.numero}
              </Typography>
              <Typography
                sx={{
                  fontWeight: 600,
                  color: n.desempeno === 'BAJO' ? 'error.main' : undefined,
                  fontStyle: n.nota !== null && !p.cerrado ? 'italic' : undefined,
                }}
              >
                {nota(n.nota)}
              </Typography>
              {n.nota !== null && !p.cerrado && (
                <Typography variant="caption" color="text.secondary">
                  parcial
                </Typography>
              )}
            </Box>
          );
        })}
        <Box>
          <Typography variant="caption" color="text.secondary">
            {datos.periodos.every((p) => p.cerrado) ? 'Año' : 'Acumulado'}
          </Typography>
          <Typography sx={{ fontWeight: 700, color: a.desempeno === 'BAJO' ? 'error.main' : undefined }}>
            {nota(a.definitiva)}
          </Typography>
          {a.desempeno && (
            <Typography variant="caption" component="div">
              <EstadoDesempeno desempeno={a.desempeno} />
            </Typography>
          )}
        </Box>
      </Box>
      {faltas.length > 0 && (
        <Typography variant="body2" sx={{ mt: 1, color: a.faltasSinJustificar > 0 ? 'error.main' : 'text.secondary' }}>
          Faltas: {faltas.join(', ')}
        </Typography>
      )}
    </Paper>
  );
}
