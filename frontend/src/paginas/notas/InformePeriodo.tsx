import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
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
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { hoyIso } from '../../api/asistencia';
import {
  NOMBRE_DESEMPENO,
  guardarInforme,
  leerNota,
  listarCargasNotas,
  obtenerInforme,
  periodoActual,
  type DatosInforme,
  type Desempeno,
  type FilaInforme,
  type InformePeriodo as Informe,
} from '../../api/notas';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';
import EstadoDesempeno from './Desempeno';

// Del mejor al peor, como se leen en el boletin
const ORDEN: Desempeno[] = ['SUPERIOR', 'ALTO', 'BASICO', 'BAJO'];
const MAXIMO_DESCRIPTOR = 600;
const MAXIMO_OBSERVACION = 500;

const formato = (valor: number | null) => (valor === null ? '' : valor.toFixed(1));

/**
 * Informe del periodo de una clase para el boletin (SIEE art. 14): el concepto descriptivo de cada desempeno
 * y, por estudiante, la valoracion del comportamiento y una observacion para la familia.
 */
export default function InformePeriodo() {
  const queryClient = useQueryClient();
  const [parametros] = useSearchParams();
  const [cargaId, setCargaId] = useState<number | ''>(parametros.get('carga') ? Number(parametros.get('carga')) : '');
  const [periodoId, setPeriodoId] = useState<number | ''>('');
  const [descriptores, setDescriptores] = useState<Partial<Record<Desempeno, string>>>({});
  const [comportamientos, setComportamientos] = useState<Record<number, string>>({});
  const [observaciones, setObservaciones] = useState<Record<number, string>>({});
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);

  // En el celular cada estudiante va en un bloque propio; la tabla no cabe con la observacion
  const celular = useMediaQuery(useTheme().breakpoints.down('sm'));
  const cargas = useQuery({ queryKey: ['notas-cargas'], queryFn: listarCargasNotas });
  const carga = cargas.data?.find((c) => c.cargaId === cargaId);
  const informe = useQuery({
    queryKey: ['notas-informe', cargaId, periodoId],
    queryFn: () => obtenerInforme(Number(cargaId), Number(periodoId)),
    enabled: cargaId !== '' && periodoId !== '' && carga !== undefined && !carga.cualitativa,
  });

  useEffect(() => {
    if (cargas.data?.length === 1) setCargaId(cargas.data[0].cargaId);
  }, [cargas.data]);

  useEffect(() => {
    if (carga) setPeriodoId(periodoActual(carga.periodos, hoyIso())?.id ?? '');
  }, [carga]);

  useEffect(() => {
    setDescriptores({});
    setComportamientos({});
    setObservaciones({});
    setMensaje(null);
  }, [cargaId, periodoId]);

  if (cargas.isPending) return <CircularProgress />;
  if (cargas.isError) return <Alert severity="error">{mensajeDeError(cargas.error)}</Alert>;
  if (cargas.data.length === 0) {
    return <Alert severity="info">No tiene grupos ni asignaturas asignadas en la carga académica.</Alert>;
  }

  const datos = informe.data;
  const editable = !!datos?.editable;
  const descriptor = (d: Desempeno) => descriptores[d] ?? datos?.descriptores[d] ?? '';
  const comportamiento = (m: number, actual: number | null) => comportamientos[m] ?? formato(actual);
  const observacion = (m: number, actual: string | null) => observaciones[m] ?? actual ?? '';
  const cambiosEstudiantes = new Set([...Object.keys(comportamientos), ...Object.keys(observaciones)].map(Number));
  const pendientes = Object.keys(descriptores).length + cambiosEstudiantes.size;
  const cuantos = (d: Desempeno) => datos?.estudiantes.filter((e) => e.desempeno === d).length ?? 0;
  const sinDescriptor = ORDEN.filter((d) => cuantos(d) > 0 && !descriptor(d).trim());

  const guardar = async () => {
    if (!datos) return;
    const estudiantes: NonNullable<DatosInforme['estudiantes']> = [];
    for (const e of datos.estudiantes.filter((f) => cambiosEstudiantes.has(f.matriculaId))) {
      const valor = leerNota(comportamiento(e.matriculaId, e.comportamiento), datos.configuracion);
      if (valor === undefined) {
        setMensaje({
          tipo: 'error',
          texto: `El comportamiento de ${e.nombres} ${e.apellidos} debe estar entre ${formato(datos.configuracion.notaMinima)} y ${formato(datos.configuracion.notaMaxima)} con una decimal.`,
        });
        return;
      }
      const texto = observacion(e.matriculaId, e.observacion).trim();
      estudiantes.push({ matriculaId: e.matriculaId, comportamiento: valor, observacion: texto || null });
    }
    setGuardando(true);
    setMensaje(null);
    try {
      const nuevo = await guardarInforme(Number(cargaId), Number(periodoId), { descriptores, estudiantes });
      queryClient.setQueryData<Informe>(['notas-informe', cargaId, periodoId], nuevo);
      setDescriptores({});
      setComportamientos({});
      setObservaciones({});
      setMensaje({ tipo: 'success', texto: 'Informe guardado' });
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  const campoComportamiento = (e: FilaInforme) => {
    const texto = comportamiento(e.matriculaId, e.comportamiento);
    const invalido = !!datos && texto.trim() !== '' && leerNota(texto, datos.configuracion) === undefined;
    return (
      <TextField
        value={texto}
        onChange={(ev) => setComportamientos({ ...comportamientos, [e.matriculaId]: ev.target.value })}
        disabled={!editable}
        error={invalido}
        size="small"
        slotProps={{
          htmlInput: {
            'aria-label': `Comportamiento de ${e.nombres} ${e.apellidos}`,
            inputMode: 'decimal',
            maxLength: 3,
            style: { textAlign: 'center' },
          },
        }}
        sx={{ width: 64, '& .MuiInputBase-input': { py: 0.75 } }}
      />
    );
  };

  const campoObservacion = (e: FilaInforme) => (
    <TextField
      value={observacion(e.matriculaId, e.observacion)}
      onChange={(ev) => setObservaciones({ ...observaciones, [e.matriculaId]: ev.target.value })}
      disabled={!editable}
      size="small"
      fullWidth
      multiline
      maxRows={4}
      placeholder="Observación para la familia (opcional)"
      slotProps={{
        htmlInput: { 'aria-label': `Observación de ${e.nombres} ${e.apellidos}`, maxLength: MAXIMO_OBSERVACION },
      }}
    />
  );

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
            sx={{ minWidth: 200 }}
          >
            {carga.periodos.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                Periodo {p.numero}
                {p.cerrado ? ' (cerrado)' : ''}
              </MenuItem>
            ))}
          </TextField>
        )}
        {editable && (
          <Button variant="contained" onClick={guardar} disabled={guardando || pendientes === 0} sx={{ ml: { sm: 'auto' } }}>
            Guardar informe
          </Button>
        )}
      </Stack>

      {cargaId === '' && <Alert severity="info">Seleccione la clase para ver el informe del periodo.</Alert>}
      {carga?.cualitativa && (
        <Alert severity="info">
          {carga.grupo.split(' ')[0]} se evalúa de forma cualitativa; su informe descriptivo se agregará con el boletín.
        </Alert>
      )}
      {mensaje && <Alert severity={mensaje.tipo}>{mensaje.texto}</Alert>}
      {informe.isFetching && !datos && <CircularProgress />}
      {informe.isError && <Alert severity="error">{mensajeDeError(informe.error)}</Alert>}

      {datos && (
        <>
          <Typography variant="body2" color="text.secondary">
            Esto es lo que su clase aporta al boletín del periodo {datos.periodo}.
            {!editable && ' El periodo está cerrado: el informe solo se consulta.'}
            {pendientes > 0 && ` Tiene ${pendientes} ${pendientes === 1 ? 'cambio' : 'cambios'} sin guardar.`}
          </Typography>

          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" component="h2">
              Concepto descriptivo por desempeño
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              El boletín muestra a cada estudiante el texto del desempeño que obtuvo en su clase. Escríbalo para que lo
              entienda la familia.
            </Typography>
            {sinDescriptor.length > 0 && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                Hay estudiantes en {sinDescriptor.map((d) => NOMBRE_DESEMPENO[d]).join(', ')} y aún no tiene el concepto de
                ese desempeño.
              </Alert>
            )}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
              {ORDEN.map((d) => (
                <TextField
                  key={d}
                  label={`${NOMBRE_DESEMPENO[d]} (${cuantos(d)} ${cuantos(d) === 1 ? 'estudiante' : 'estudiantes'})`}
                  value={descriptor(d)}
                  onChange={(e) => setDescriptores({ ...descriptores, [d]: e.target.value })}
                  multiline
                  minRows={3}
                  disabled={!editable}
                  slotProps={{ htmlInput: { maxLength: MAXIMO_DESCRIPTOR } }}
                  helperText={`${descriptor(d).length} de ${MAXIMO_DESCRIPTOR} caracteres`}
                />
              ))}
            </Box>
          </Paper>

          {datos.estudiantes.length === 0 ? (
            <Alert severity="info">El grupo no tiene estudiantes matriculados.</Alert>
          ) : celular ? (
            <Paper>
              {datos.estudiantes.map((e, fila) => (
                <Box key={e.matriculaId} sx={{ p: 2, borderTop: fila === 0 ? 'none' : `1px solid ${COLORES.linea}` }}>
                  <Typography sx={{ fontWeight: 600 }}>
                    {fila + 1}. {e.apellidos} {e.nombres}
                  </Typography>
                  <Stack direction="row" spacing={2} useFlexGap sx={{ alignItems: 'center', flexWrap: 'wrap', my: 1 }}>
                    <Typography variant="body2" sx={{ whiteSpace: 'nowrap' }}>
                      Nota {e.notaDefinitiva === null ? '-' : `${formato(e.notaDefinitiva)}${e.completa ? '' : '*'}`}
                    </Typography>
                    <Typography variant="body2" component="span">
                      <EstadoDesempeno desempeno={e.desempeno} />
                    </Typography>
                    <Box sx={{ ml: 'auto', display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="body2" color="text.secondary">
                        Comportamiento
                      </Typography>
                      {campoComportamiento(e)}
                    </Box>
                  </Stack>
                  {campoObservacion(e)}
                </Box>
              ))}
            </Paper>
          ) : (
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Estudiante</TableCell>
                    <TableCell align="center">Nota</TableCell>
                    <TableCell>Desempeño</TableCell>
                    <TableCell align="center">
                      Comportamiento
                      <Typography variant="caption" display="block" color="text.secondary">
                        {formato(datos.configuracion.notaMinima)} a {formato(datos.configuracion.notaMaxima)}
                      </Typography>
                    </TableCell>
                    <TableCell sx={{ width: '45%' }}>Observación para la familia</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {datos.estudiantes.map((e, fila) => {
                    return (
                      <TableRow key={e.matriculaId}>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>
                          {fila + 1}. {e.apellidos} {e.nombres}
                        </TableCell>
                        <TableCell align="center" sx={{ fontWeight: 600 }}>
                          {e.notaDefinitiva === null ? '-' : `${formato(e.notaDefinitiva)}${e.completa ? '' : '*'}`}
                        </TableCell>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>
                          <EstadoDesempeno desempeno={e.desempeno} />
                        </TableCell>
                        <TableCell align="center">{campoComportamiento(e)}</TableCell>
                        <TableCell>{campoObservacion(e)}</TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
          <Typography variant="caption" color="text.secondary" sx={{ borderTop: `1px solid ${COLORES.linea}`, pt: 1 }}>
            La nota y el desempeño vienen de la planilla, con la recuperación aplicada. El boletín muestra el promedio del
            comportamiento que registran todos los docentes del grupo (SIEE art. 14).
          </Typography>
        </>
      )}
    </Stack>
  );
}
