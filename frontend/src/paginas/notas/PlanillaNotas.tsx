import { useEffect, useMemo, useState, type KeyboardEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  InputBase,
  MenuItem,
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
import { hoyIso } from '../../api/asistencia';
import {
  DIMENSIONES,
  NOMBRE_DIMENSION,
  guardarNotas,
  listarCargasNotas,
  obtenerPlanilla,
  periodoActual,
  type Actividad,
  type ConfiguracionEvaluacion,
  type Dimension,
  type PlanillaNotas as Planilla,
} from '../../api/notas';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';
import EstadoDesempeno from './Desempeno';
import DialogoActividad from './DialogoActividad';

const clave = (actividadId: number, matriculaId: number) => `${actividadId}-${matriculaId}`;
const formato = (valor: number | null | undefined) => (valor === null || valor === undefined ? '' : valor.toFixed(1));

/** Convierte lo que escribe el docente (acepta coma) en nota; undefined si no es valida. */
function leerNota(texto: string, config: ConfiguracionEvaluacion): number | null | undefined {
  const limpio = texto.trim().replace(',', '.');
  if (limpio === '') return null;
  if (!/^\d+(\.\d)?$/.test(limpio)) return undefined;
  const valor = Number(limpio);
  return valor >= config.notaMinima && valor <= config.notaMaxima ? valor : undefined;
}

/**
 * Planilla de notas de una clase en un periodo. Las actividades se agrupan por dimension;
 * el docente escribe las notas y guarda todas juntas. Enter baja a la siguiente fila.
 */
export default function PlanillaNotas() {
  const queryClient = useQueryClient();
  const [parametros] = useSearchParams();
  const [cargaId, setCargaId] = useState<number | ''>(parametros.get('carga') ? Number(parametros.get('carga')) : '');
  const [periodoId, setPeriodoId] = useState<number | ''>('');
  const [cambios, setCambios] = useState<Record<string, string>>({});
  const [dialogo, setDialogo] = useState<{ actividad: Actividad | null } | null>(null);
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);

  const cargas = useQuery({ queryKey: ['notas-cargas'], queryFn: listarCargasNotas });
  const carga = cargas.data?.find((c) => c.cargaId === cargaId);
  const planilla = useQuery({
    queryKey: ['notas-planilla', cargaId, periodoId],
    queryFn: () => obtenerPlanilla(Number(cargaId), Number(periodoId)),
    enabled: cargaId !== '' && periodoId !== '' && carga !== undefined && !carga.cualitativa,
  });

  // Si el docente tiene una sola clase, se selecciona sola
  useEffect(() => {
    if (cargas.data?.length === 1) setCargaId(cargas.data[0].cargaId);
  }, [cargas.data]);

  // Al elegir la clase se propone el periodo en curso
  useEffect(() => {
    if (carga) setPeriodoId(periodoActual(carga.periodos, hoyIso())?.id ?? '');
  }, [carga]);

  useEffect(() => {
    setCambios({});
    setMensaje(null);
  }, [cargaId, periodoId]);

  const porDimension = useMemo(() => {
    const grupos: Record<Dimension, Actividad[]> = { SABER: [], HACER: [], SER: [] };
    planilla.data?.actividades.forEach((a) => grupos[a.dimension].push(a));
    return grupos;
  }, [planilla.data]);
  const columnas = DIMENSIONES.flatMap((d) => porDimension[d]);

  const actualizarPlanilla = (nueva: Planilla) => {
    queryClient.setQueryData(['notas-planilla', cargaId, periodoId], nueva);
  };

  const pendientes = Object.keys(cambios).length;
  const config = planilla.data?.configuracion;

  const guardar = async () => {
    if (!planilla.data || !config) return;
    const notas: { actividadId: number; matriculaId: number; valor: number | null }[] = [];
    for (const [k, texto] of Object.entries(cambios)) {
      const valor = leerNota(texto, config);
      if (valor === undefined) {
        setMensaje({
          tipo: 'error',
          texto: `Hay notas que no son válidas. Use valores de ${formato(config.notaMinima)} a ${formato(config.notaMaxima)} con una decimal.`,
        });
        return;
      }
      const [actividadId, matriculaId] = k.split('-').map(Number);
      notas.push({ actividadId, matriculaId, valor });
    }
    setGuardando(true);
    setMensaje(null);
    try {
      actualizarPlanilla(await guardarNotas(Number(cargaId), Number(periodoId), notas));
      setCambios({});
      setMensaje({ tipo: 'success', texto: 'Notas guardadas' });
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  const alTeclear = (e: KeyboardEvent<HTMLInputElement>, fila: number, columna: number) => {
    if (e.key !== 'Enter') return;
    e.preventDefault();
    document.getElementById(`nota-${fila + 1}-${columna}`)?.focus();
  };

  if (cargas.isPending) return <CircularProgress />;
  if (cargas.isError) return <Alert severity="error">{mensajeDeError(cargas.error)}</Alert>;
  if (cargas.data.length === 0) {
    return <Alert severity="info">No tiene grupos ni asignaturas asignadas en la carga académica.</Alert>;
  }

  const bordeGrupo = `2px solid ${COLORES.linea}`;

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'center' } }}>
        <TextField
          select
          label="Clase"
          value={carga ? cargaId : ''}
          onChange={(e) => setCargaId(Number(e.target.value))}
          sx={{ minWidth: 280 }}
        >
          {cargas.data.map((c) => (
            <MenuItem key={c.cargaId} value={c.cargaId}>
              {c.grupo} - {c.asignatura} ({c.sede}, {c.anio})
            </MenuItem>
          ))}
        </TextField>
        {carga && !carga.cualitativa && (
          <TextField
            select
            label="Periodo"
            value={periodoId}
            onChange={(e) => setPeriodoId(Number(e.target.value))}
            sx={{ minWidth: 160 }}
          >
            {carga.periodos.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                Periodo {p.numero}
                {p.cerrado ? ' (cerrado)' : ''}
              </MenuItem>
            ))}
          </TextField>
        )}
        {planilla.data?.editable && (
          <Stack direction="row" spacing={1.5} sx={{ ml: { sm: 'auto' } }}>
            <Button variant="outlined" onClick={() => setDialogo({ actividad: null })}>
              Nueva actividad
            </Button>
            <Button variant="contained" onClick={guardar} disabled={guardando || pendientes === 0}>
              Guardar notas
            </Button>
          </Stack>
        )}
      </Stack>

      {cargaId === '' && <Alert severity="info">Seleccione la clase para ver la planilla.</Alert>}
      {carga?.cualitativa && (
        <Alert severity="info">
          {carga.grupo.split(' ')[0]} se evalúa de forma cualitativa, sin notas numéricas (SIEE art. 4.3.1).
        </Alert>
      )}
      {mensaje && <Alert severity={mensaje.tipo}>{mensaje.texto}</Alert>}
      {planilla.isFetching && !planilla.data && <CircularProgress />}
      {planilla.isError && <Alert severity="error">{mensajeDeError(planilla.error)}</Alert>}

      {planilla.data && config && (
        <>
          <Typography variant="body2" color="text.secondary">
            Pesos: Saber {config.pesoSaber} %, Hacer {config.pesoHacer} %, Ser {config.pesoSer} %. Escala de{' '}
            {formato(config.notaMinima)} a {formato(config.notaMaxima)}; aprueba con {formato(config.notaAprobatoria)}.
            {!planilla.data.editable && ' El periodo está cerrado y solo se puede consultar.'}
            {pendientes > 0 && ` Tiene ${pendientes} ${pendientes === 1 ? 'cambio' : 'cambios'} sin guardar.`}
          </Typography>
          {planilla.data.actividades.length === 0 && (
            <Alert severity="info">
              Este periodo aún no tiene actividades. Cree la primera con «Nueva actividad» y elija si evalúa el Saber, el
              Hacer o el Ser.
            </Alert>
          )}
          {planilla.data.estudiantes.length === 0 ? (
            <Alert severity="info">El grupo no tiene estudiantes matriculados.</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table size="small" sx={{ '& td, & th': { whiteSpace: 'nowrap' } }}>
                <TableHead>
                  <TableRow>
                    <TableCell rowSpan={2} sx={{ position: 'sticky', left: 0, zIndex: 3, bgcolor: COLORES.encabezado }}>
                      Estudiante
                    </TableCell>
                    {DIMENSIONES.map((d) => (
                      <TableCell key={d} align="center" colSpan={porDimension[d].length + 1} sx={{ borderLeft: bordeGrupo }}>
                        {NOMBRE_DIMENSION[d]} ({d === 'SABER' ? config.pesoSaber : d === 'HACER' ? config.pesoHacer : config.pesoSer} %)
                      </TableCell>
                    ))}
                    <TableCell rowSpan={2} align="center" sx={{ borderLeft: bordeGrupo }}>
                      Nota
                    </TableCell>
                    <TableCell rowSpan={2}>Desempeño</TableCell>
                  </TableRow>
                  <TableRow>
                    {DIMENSIONES.map((d) => [
                      ...porDimension[d].map((a, i) => (
                        <TableCell key={a.id} align="center" sx={{ borderLeft: i === 0 ? bordeGrupo : undefined, fontWeight: 400 }}>
                          {planilla.data.editable ? (
                            <Button size="small" onClick={() => setDialogo({ actividad: a })} title="Editar o borrar la actividad">
                              {a.nombre}
                            </Button>
                          ) : (
                            a.nombre
                          )}
                          {a.peso > 1 && (
                            <Typography variant="caption" display="block" color="text.secondary">
                              Vale x{a.peso}
                            </Typography>
                          )}
                        </TableCell>
                      )),
                      <TableCell
                        key={d + '-prom'}
                        align="center"
                        sx={{ borderLeft: porDimension[d].length === 0 ? bordeGrupo : undefined }}
                      >
                        Prom.
                      </TableCell>,
                    ])}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {planilla.data.estudiantes.map((f, fila) => (
                    <TableRow key={f.matriculaId}>
                      <TableCell sx={{ position: 'sticky', left: 0, zIndex: 1, bgcolor: '#fff' }}>
                        {fila + 1}. {f.apellidos} {f.nombres}
                      </TableCell>
                      {DIMENSIONES.map((d) => [
                        ...porDimension[d].map((a, i) => {
                          const k = clave(a.id, f.matriculaId);
                          const texto = cambios[k] ?? formato(f.notas[String(a.id)]);
                          const invalida = cambios[k] !== undefined && leerNota(cambios[k], config) === undefined;
                          const columna = columnas.indexOf(a);
                          return (
                            <TableCell key={a.id} align="center" sx={{ borderLeft: i === 0 ? bordeGrupo : undefined, py: 0.5 }}>
                              <InputBase
                                id={`nota-${fila}-${columna}`}
                                value={texto}
                                readOnly={!planilla.data.editable}
                                onChange={(e) => setCambios((c) => ({ ...c, [k]: e.target.value }))}
                                onKeyDown={(e) => alTeclear(e as KeyboardEvent<HTMLInputElement>, fila, columna)}
                                inputProps={{
                                  inputMode: 'decimal',
                                  'aria-label': `${a.nombre} de ${f.nombres} ${f.apellidos}`,
                                  style: { textAlign: 'center' },
                                }}
                                sx={{
                                  width: 52,
                                  px: 0.5,
                                  border: `1px solid ${invalida ? '#b3261e' : cambios[k] !== undefined ? COLORES.sol : COLORES.linea}`,
                                  borderRadius: 1,
                                  bgcolor: cambios[k] !== undefined ? '#fdf7ea' : '#fff',
                                }}
                              />
                            </TableCell>
                          );
                        }),
                        <TableCell
                          key={d + '-prom'}
                          align="center"
                          sx={{ color: 'text.secondary', borderLeft: porDimension[d].length === 0 ? bordeGrupo : undefined }}
                        >
                          {formato(d === 'SABER' ? f.saber : d === 'HACER' ? f.hacer : f.ser) || '-'}
                        </TableCell>,
                      ])}
                      <TableCell
                        align="center"
                        sx={{ borderLeft: bordeGrupo, fontWeight: 700 }}
                        title={f.notaPeriodo !== null && !f.completa ? 'Nota parcial: falta alguna dimensión' : undefined}
                      >
                        {f.notaPeriodo === null ? '-' : `${formato(f.notaPeriodo)}${f.completa ? '' : '*'}`}
                      </TableCell>
                      <TableCell>
                        <EstadoDesempeno desempeno={f.desempeno} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <Box>
            <Typography variant="caption" color="text.secondary">
              * Nota parcial: todavía falta alguna dimensión con notas. Una actividad sin nota no cuenta en el promedio;
              si el estudiante no la presentó, registre la nota mínima.
            </Typography>
          </Box>
        </>
      )}

      {dialogo && cargaId !== '' && periodoId !== '' && (
        <DialogoActividad
          cargaId={cargaId}
          periodoId={periodoId}
          actividad={dialogo.actividad}
          alCerrar={() => setDialogo(null)}
          alGuardar={(nueva) => {
            actualizarPlanilla(nueva);
            // Si se borro una actividad, se descartan sus cambios sin guardar
            const vigentes = new Set(nueva.actividades.map((a) => String(a.id)));
            setCambios((c) => Object.fromEntries(Object.entries(c).filter(([k]) => vigentes.has(k.split('-')[0]))));
            setDialogo(null);
          }}
        />
      )}
    </Stack>
  );
}
