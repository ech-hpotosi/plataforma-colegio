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
  guardarRecuperaciones,
  leerNota,
  listarCargasNotas,
  obtenerPlanilla,
  periodoActual,
  type Actividad,
  type Dimension,
  type PlanillaNotas as Planilla,
} from '../../api/notas';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';
import EstadoDesempeno from './Desempeno';
import DialogoActividad from './DialogoActividad';
import RecuperacionFinal from './RecuperacionFinal';

const clave = (actividadId: number, matriculaId: number) => `${actividadId}-${matriculaId}`;
/** Porcentaje con una decimal solo si la tiene: 50, 33.3. */
const porcentajeTexto = (valor: number) => String(Math.round(valor * 10) / 10);
const formato = (valor: number | null | undefined) => (valor === null || valor === undefined ? '' : valor.toFixed(1));

/**
 * Planilla de notas de una clase en un periodo. Las actividades se agrupan por dimension;
 * el docente escribe las notas y guarda todas juntas. Enter baja a la siguiente fila.
 * A quien queda en Bajo se le puede registrar la recuperacion del periodo; la opcion "Recuperación final"
 * del selector de periodo muestra la recuperacion del anio.
 */
export default function PlanillaNotas() {
  const queryClient = useQueryClient();
  const [parametros] = useSearchParams();
  const [cargaId, setCargaId] = useState<number | ''>(parametros.get('carga') ? Number(parametros.get('carga')) : '');
  const [periodoId, setPeriodoId] = useState<number | '' | 'final'>('');
  const [cambios, setCambios] = useState<Record<string, string>>({});
  const [cambiosRec, setCambiosRec] = useState<Record<number, string>>({});
  const [dialogo, setDialogo] = useState<{ actividad: Actividad | null } | null>(null);
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);

  const cargas = useQuery({ queryKey: ['notas-cargas'], queryFn: listarCargasNotas });
  const carga = cargas.data?.find((c) => c.cargaId === cargaId);
  const planilla = useQuery({
    queryKey: ['notas-planilla', cargaId, periodoId],
    queryFn: () => obtenerPlanilla(Number(cargaId), Number(periodoId)),
    enabled: cargaId !== '' && typeof periodoId === 'number' && carga !== undefined && !carga.cualitativa,
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
    setCambiosRec({});
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

  const pendientes = Object.keys(cambios).length + Object.keys(cambiosRec).length;
  const config = planilla.data?.configuracion;
  const pesoDimension = (d: Dimension) =>
    !config ? 0 : d === 'SABER' ? config.pesoSaber : d === 'HACER' ? config.pesoHacer : config.pesoSer;
  const sumaPorcentajes = (d: Dimension) => porDimension[d].reduce((suma, a) => suma + (a.porcentaje ?? 0), 0);

  const guardar = async () => {
    if (!planilla.data || !config) return;
    const notas: { actividadId: number; matriculaId: number; valor: number | null }[] = [];
    const recuperaciones: { matriculaId: number; nota: number | null; observacion: string | null }[] = [];
    for (const [k, texto] of Object.entries(cambiosRec)) {
      const nota = leerNota(texto, config);
      if (nota !== undefined) recuperaciones.push({ matriculaId: Number(k), nota, observacion: null });
    }
    const recInvalida = recuperaciones.length !== Object.keys(cambiosRec).length;
    for (const [k, texto] of Object.entries(cambios)) {
      const valor = leerNota(texto, config);
      if (valor === undefined || recInvalida) {
        setMensaje({
          tipo: 'error',
          texto: `Hay notas que no son válidas. Use valores de ${formato(config.notaMinima)} a ${formato(config.notaMaxima)} con una decimal.`,
        });
        return;
      }
      const [actividadId, matriculaId] = k.split('-').map(Number);
      notas.push({ actividadId, matriculaId, valor });
    }
    if (recInvalida) {
      setMensaje({
        tipo: 'error',
        texto: `Hay recuperaciones que no son válidas. Use valores de ${formato(config.notaMinima)} a ${formato(config.notaMaxima)} con una decimal.`,
      });
      return;
    }
    setGuardando(true);
    setMensaje(null);
    try {
      // Primero las notas, porque la recuperacion depende de la nota ya calculada
      if (notas.length > 0) {
        actualizarPlanilla(await guardarNotas(Number(cargaId), Number(periodoId), notas));
        setCambios({});
      }
      if (recuperaciones.length > 0) {
        actualizarPlanilla(await guardarRecuperaciones(Number(cargaId), Number(periodoId), recuperaciones));
        setCambiosRec({});
      }
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
  // La recuperacion es para quien tiene la nota calculada en Bajo, o ya tiene una registrada
  const puedeRecuperar = (f: { notaPeriodo: number | null; recuperacion: number | null }) =>
    f.recuperacion !== null || (f.notaPeriodo !== null && config !== undefined && f.notaPeriodo < config.notaAprobatoria);
  const mostrarRecuperacion = !!planilla.data?.admiteRecuperacion && !!planilla.data?.estudiantes.some(puedeRecuperar);

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
            onChange={(e) => setPeriodoId(e.target.value === 'final' ? 'final' : Number(e.target.value))}
            sx={{ minWidth: 200 }}
          >
            {carga.periodos.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                Periodo {p.numero}
                {p.cerrado ? ' (cerrado)' : ''}
              </MenuItem>
            ))}
            <MenuItem value="final">Recuperación final</MenuItem>
          </TextField>
        )}
        {typeof periodoId === 'number' && (planilla.data?.editable || planilla.data?.admiteRecuperacion) && (
          <Stack direction="row" spacing={1.5} sx={{ ml: { sm: 'auto' } }}>
            {planilla.data.editable && (
              <Button variant="outlined" onClick={() => setDialogo({ actividad: null })}>
                Nueva actividad
              </Button>
            )}
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
      {periodoId === 'final' && carga && !carga.cualitativa && <RecuperacionFinal cargaId={carga.cargaId} />}
      {planilla.isFetching && !planilla.data && <CircularProgress />}
      {planilla.isError && <Alert severity="error">{mensajeDeError(planilla.error)}</Alert>}

      {planilla.data && config && typeof periodoId === 'number' && (
        <>
          <Typography variant="body2" color="text.secondary">
            Pesos: Saber {config.pesoSaber} %, Hacer {config.pesoHacer} %, Ser {config.pesoSer} %. Escala de{' '}
            {formato(config.notaMinima)} a {formato(config.notaMaxima)}; aprueba con {formato(config.notaAprobatoria)}.
            {!planilla.data.editable && ' El periodo está cerrado: las notas solo se consultan, pero aún puede registrar recuperaciones.'}
            {pendientes > 0 && ` Tiene ${pendientes} ${pendientes === 1 ? 'cambio' : 'cambios'} sin guardar.`}
          </Typography>
          {DIMENSIONES.filter(
            (d) => porDimension[d].length > 0 && porDimension[d].every((a) => a.porcentaje !== null) && sumaPorcentajes(d) < 100,
          ).map((d) => (
            <Alert key={d} severity="warning">
              Los porcentajes de las actividades del {NOMBRE_DIMENSION[d]} suman {sumaPorcentajes(d)} %. Mientras no lleguen a 100 %,
              la nota del {NOMBRE_DIMENSION[d]} se calcula en proporción a lo asignado.
            </Alert>
          ))}
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
              <Table size="small" sx={{ '& td, & th': { whiteSpace: 'nowrap', px: 1 } }}>
                <TableHead>
                  <TableRow>
                    <TableCell rowSpan={2} sx={{ position: 'sticky', left: 0, zIndex: 3, bgcolor: COLORES.encabezado }}>
                      Estudiante
                    </TableCell>
                    {DIMENSIONES.map((d) => (
                      <TableCell key={d} align="center" colSpan={porDimension[d].length + 1} sx={{ borderLeft: bordeGrupo }}>
                        {NOMBRE_DIMENSION[d]} ({pesoDimension(d)} %)
                      </TableCell>
                    ))}
                    <TableCell rowSpan={2} align="center" sx={{ borderLeft: bordeGrupo }}>
                      Nota
                    </TableCell>
                    {mostrarRecuperacion && (
                      <>
                        <TableCell rowSpan={2} align="center">
                          Recuperación
                          <Typography variant="caption" display="block" color="text.secondary">
                            máximo {formato(config.topeRecuperacion)}
                          </Typography>
                        </TableCell>
                      </>
                    )}
                    <TableCell rowSpan={2}>Desempeño</TableCell>
                  </TableRow>
                  <TableRow>
                    {DIMENSIONES.map((d) => [
                      ...porDimension[d].map((a, i) => (
                        <TableCell
                          key={a.id}
                          align="center"
                          sx={{ borderLeft: i === 0 ? bordeGrupo : undefined, fontWeight: 400, whiteSpace: 'normal', maxWidth: 110, lineHeight: 1.2 }}
                        >
                          {planilla.data.editable ? (
                            <Button size="small" sx={{ lineHeight: 1.2, minWidth: 0 }} onClick={() => setDialogo({ actividad: a })} title="Editar o borrar la actividad">
                              {a.nombre}
                            </Button>
                          ) : (
                            a.nombre
                          )}
                          <Typography variant="caption" display="block" color="text.secondary">
                            {porcentajeTexto(a.porcentajeEfectivo)} % del {NOMBRE_DIMENSION[d]}
                          </Typography>
                          <Typography variant="caption" display="block" color="text.secondary">
                            {porcentajeTexto((a.porcentajeEfectivo * pesoDimension(d)) / 100)} % del periodo
                          </Typography>
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
                        {f.notaDefinitiva === null ? '-' : `${formato(f.notaDefinitiva)}${f.completa ? '' : '*'}`}
                        {f.recuperacion !== null && (
                          <Typography variant="caption" display="block" color="text.secondary" sx={{ fontWeight: 400 }}>
                            sin recuperar {formato(f.notaPeriodo)}
                          </Typography>
                        )}
                      </TableCell>
                      {mostrarRecuperacion && (
                        <>
                          <TableCell align="center" sx={{ py: 0.5 }}>
                            {puedeRecuperar(f) ? (
                              <InputBase
                                value={cambiosRec[f.matriculaId] ?? formato(f.recuperacion)}
                                onChange={(e) => setCambiosRec((c) => ({ ...c, [f.matriculaId]: e.target.value }))}
                                inputProps={{
                                  inputMode: 'decimal',
                                  'aria-label': `Recuperación de ${f.nombres} ${f.apellidos}`,
                                  style: { textAlign: 'center' },
                                }}
                                sx={{
                                  width: 52,
                                  px: 0.5,
                                  border: `1px solid ${
                                    cambiosRec[f.matriculaId] !== undefined && leerNota(cambiosRec[f.matriculaId], config) === undefined
                                      ? '#b3261e'
                                      : cambiosRec[f.matriculaId] !== undefined
                                        ? COLORES.sol
                                        : COLORES.linea
                                  }`,
                                  borderRadius: 1,
                                  bgcolor: cambiosRec[f.matriculaId] !== undefined ? '#fdf7ea' : '#fff',
                                }}
                              />
                            ) : (
                              ''
                            )}
                          </TableCell>
                        </>
                      )}
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
              {mostrarRecuperacion &&
                ` La recuperación aparece para quien queda en Bajo; la nota final del periodo es la mayor entre la calculada y la recuperación, sin pasar de ${formato(config.topeRecuperacion)}.`}
            </Typography>
          </Box>
        </>
      )}

      {dialogo && cargaId !== '' && typeof periodoId === 'number' && (
        <DialogoActividad
          cargaId={cargaId}
          periodoId={periodoId}
          actividad={dialogo.actividad}
          actividades={planilla.data?.actividades ?? []}
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
