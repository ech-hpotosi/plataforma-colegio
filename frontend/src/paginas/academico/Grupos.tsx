import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid2 as Grid,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import {
  actualizarGrupo,
  crearGrupo,
  guardarCarga,
  listarCarga,
  listarDocentes,
  listarGrados,
  listarGrupos,
  listarSedes,
  NOMBRE_JORNADA,
  type DatosGrupo,
  type Docente,
  type Grupo,
  type Jornada,
} from '../../api/academico';
import { mensajeDeError } from './mensajes';
import SelectorAnio from './SelectorAnio';

/** Grupos de un anio lectivo por sede, con su director y su carga academica. */
export default function Grupos() {
  const queryClient = useQueryClient();
  const [anioId, setAnioId] = useState<number | null>(null);
  const [sedeId, setSedeId] = useState<number | null>(null);
  const [editando, setEditando] = useState<Grupo | 'nuevo' | null>(null);
  const [cargaDe, setCargaDe] = useState<Grupo | null>(null);

  const sedes = useQuery({ queryKey: ['sedes'], queryFn: listarSedes });
  const docentes = useQuery({ queryKey: ['docentes'], queryFn: listarDocentes });
  const grupos = useQuery({
    queryKey: ['grupos', anioId, sedeId],
    queryFn: () => listarGrupos(anioId!, sedeId),
    enabled: anioId !== null,
  });

  return (
    <>
      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
        <SelectorAnio valor={anioId} alCambiar={setAnioId} />
        <TextField
          select
          size="small"
          label="Sede"
          value={sedeId ?? 0}
          onChange={(e) => setSedeId(Number(e.target.value) || null)}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value={0}>Todas las sedes</MenuItem>
          {sedes.data?.map((s) => (
            <MenuItem key={s.id} value={s.id}>
              {s.nombre}
            </MenuItem>
          ))}
        </TextField>
        <Box sx={{ flexGrow: 1 }} />
        <Button variant="contained" disabled={anioId === null} onClick={() => setEditando('nuevo')}>
          Nuevo grupo
        </Button>
      </Box>
      {grupos.isError && <Alert severity="error">{grupos.error.message}</Alert>}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Sede</TableCell>
              <TableCell>Grado</TableCell>
              <TableCell>Grupo</TableCell>
              <TableCell>Jornada</TableCell>
              <TableCell>Cupo</TableCell>
              <TableCell>Director de grupo</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {grupos.data?.map((grupo) => (
              <TableRow key={grupo.id} hover>
                <TableCell>{grupo.sede}</TableCell>
                <TableCell>{grupo.grado}</TableCell>
                <TableCell>{grupo.nombre}</TableCell>
                <TableCell>{NOMBRE_JORNADA[grupo.jornada]}</TableCell>
                <TableCell>{grupo.cupo}</TableCell>
                <TableCell>{grupo.director ?? 'Sin asignar'}</TableCell>
                <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                  <Button size="small" onClick={() => setEditando(grupo)}>
                    Editar
                  </Button>
                  <Button size="small" onClick={() => setCargaDe(grupo)}>
                    Carga academica
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {grupos.data?.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No hay grupos registrados
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {editando !== null && anioId !== null && (
        <DialogoGrupo
          grupo={editando === 'nuevo' ? null : editando}
          anioId={anioId}
          sedeInicial={sedeId}
          docentes={docentes.data ?? []}
          alCerrar={() => setEditando(null)}
          alGuardar={() => {
            setEditando(null);
            queryClient.invalidateQueries({ queryKey: ['grupos'] });
          }}
        />
      )}
      {cargaDe && <DialogoCarga grupo={cargaDe} docentes={docentes.data ?? []} alCerrar={() => setCargaDe(null)} />}
    </>
  );
}

function nombreDocente(d: Docente) {
  return `${d.apellidos} ${d.nombres}`;
}

/** Docentes que pueden trabajar en la sede: los que la tienen asignada o no tienen sedes. */
function docentesDeSede(docentes: Docente[], sedeId: number) {
  return docentes.filter((d) => d.sedeIds.length === 0 || d.sedeIds.includes(sedeId));
}

function DialogoGrupo({
  grupo,
  anioId,
  sedeInicial,
  docentes,
  alCerrar,
  alGuardar,
}: {
  grupo: Grupo | null;
  anioId: number;
  sedeInicial: number | null;
  docentes: Docente[];
  alCerrar: () => void;
  alGuardar: () => void;
}) {
  const sedes = useQuery({ queryKey: ['sedes'], queryFn: listarSedes });
  const grados = useQuery({ queryKey: ['grados'], queryFn: listarGrados });
  const [datos, setDatos] = useState<Omit<DatosGrupo, 'anioLectivoId'>>({
    sedeId: grupo?.sedeId ?? sedeInicial ?? 0,
    gradoId: grupo?.gradoId ?? 0,
    nombre: grupo?.nombre ?? '',
    jornada: grupo?.jornada ?? 'MANANA',
    cupo: grupo?.cupo ?? 35,
    directorId: grupo?.directorId ?? null,
  });
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const cambiar = (cambios: Partial<typeof datos>) => setDatos((actual) => ({ ...actual, ...cambios }));

  const guardar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!datos.sedeId || !datos.gradoId || !datos.nombre.trim()) {
      setError('Seleccione la sede y el grado, e ingrese el nombre del grupo');
      return;
    }
    setError(null);
    setGuardando(true);
    try {
      const cuerpo = { ...datos, anioLectivoId: anioId };
      await (grupo ? actualizarGrupo(grupo.id, cuerpo) : crearGrupo(cuerpo));
      alGuardar();
    } catch (err) {
      setError(mensajeDeError(err));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{grupo ? `Editar grupo ${grupo.nombre}` : 'Nuevo grupo'}</DialogTitle>
      <form onSubmit={guardar} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                select
                fullWidth
                label="Sede"
                value={datos.sedeId || ''}
                onChange={(e) => cambiar({ sedeId: Number(e.target.value), directorId: null })}
              >
                {sedes.data?.map((s) => (
                  <MenuItem key={s.id} value={s.id}>
                    {s.nombre}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                select
                fullWidth
                label="Grado"
                value={datos.gradoId || ''}
                onChange={(e) => cambiar({ gradoId: Number(e.target.value) })}
              >
                {grados.data?.map((g) => (
                  <MenuItem key={g.id} value={g.id}>
                    {g.nombre}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                fullWidth
                label="Nombre"
                placeholder="6-01"
                value={datos.nombre}
                onChange={(e) => cambiar({ nombre: e.target.value })}
                slotProps={{ htmlInput: { maxLength: 20 } }}
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 4 }}>
              <TextField
                select
                fullWidth
                label="Jornada"
                value={datos.jornada}
                onChange={(e) => cambiar({ jornada: e.target.value as Jornada })}
              >
                {Object.entries(NOMBRE_JORNADA).map(([valor, nombre]) => (
                  <MenuItem key={valor} value={valor}>
                    {nombre}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 6, sm: 4 }}>
              <TextField
                fullWidth
                type="number"
                label="Cupo"
                value={datos.cupo}
                onChange={(e) => cambiar({ cupo: Number(e.target.value) })}
                slotProps={{ htmlInput: { min: 1, max: 60 } }}
              />
            </Grid>
            <Grid size={12}>
              <TextField
                select
                fullWidth
                label="Director de grupo"
                value={datos.directorId ?? 0}
                onChange={(e) => cambiar({ directorId: Number(e.target.value) || null })}
              >
                <MenuItem value={0}>Sin asignar</MenuItem>
                {docentesDeSede(docentes, datos.sedeId).map((d) => (
                  <MenuItem key={d.id} value={d.id}>
                    {nombreDocente(d)}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={guardando}>
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

function DialogoCarga({ grupo, docentes, alCerrar }: { grupo: Grupo; docentes: Docente[]; alCerrar: () => void }) {
  const queryClient = useQueryClient();
  const carga = useQuery({ queryKey: ['carga', grupo.id], queryFn: () => listarCarga(grupo.id) });
  // Docente elegido por asignatura; 0 significa sin docente
  const [elegidos, setElegidos] = useState<Record<number, number>>({});
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const disponibles = docentesDeSede(docentes, grupo.sedeId);

  useEffect(() => {
    if (carga.data) {
      setElegidos(Object.fromEntries(carga.data.map((c) => [c.asignaturaId, c.docenteId ?? 0])));
    }
  }, [carga.data]);

  const guardar = async () => {
    setError(null);
    setGuardando(true);
    try {
      const asignaciones = Object.entries(elegidos)
        .filter(([, docenteId]) => docenteId > 0)
        .map(([asignaturaId, docenteId]) => ({ asignaturaId: Number(asignaturaId), docenteId }));
      const guardada = await guardarCarga(grupo.id, asignaciones);
      queryClient.setQueryData(['carga', grupo.id], guardada);
      alCerrar();
    } catch (e) {
      setError(mensajeDeError(e));
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="md" fullWidth>
      <DialogTitle>
        Carga academica de {grupo.grado} {grupo.nombre} ({grupo.sede})
      </DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {carga.data?.length === 0 && (
          <Typography>
            El grado {grupo.grado} no tiene plan de estudios en este anio. Configurelo en Plan de estudios.
          </Typography>
        )}
        {carga.data && carga.data.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Asignatura</TableCell>
                <TableCell>Area</TableCell>
                <TableCell>Horas</TableCell>
                <TableCell>Docente</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {carga.data.map((item) => (
                <TableRow key={item.asignaturaId}>
                  <TableCell>{item.asignatura}</TableCell>
                  <TableCell>{item.area}</TableCell>
                  <TableCell>{item.intensidadHoraria}</TableCell>
                  <TableCell>
                    <TextField
                      select
                      size="small"
                      fullWidth
                      value={elegidos[item.asignaturaId] ?? 0}
                      onChange={(e) =>
                        setElegidos((actual) => ({ ...actual, [item.asignaturaId]: Number(e.target.value) }))
                      }
                      slotProps={{
                        // Sin etiqueta visible: el nombre accesible del selector es la asignatura
                        select: {
                          SelectDisplayProps: {
                            'aria-label': `Docente de ${item.asignatura}`,
                            'aria-labelledby': undefined,
                          } as object,
                        },
                      }}
                    >
                      <MenuItem value={0}>Sin asignar</MenuItem>
                      {disponibles.map((d) => (
                        <MenuItem key={d.id} value={d.id}>
                          {nombreDocente(d)}
                        </MenuItem>
                      ))}
                    </TextField>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" onClick={guardar} disabled={guardando || !carga.data?.length}>
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
