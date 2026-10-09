import { useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link as RouterLink, useParams } from 'react-router';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid2 as Grid,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  desvincularAcudiente,
  listarMatriculas,
  NOMBRE_ESTADO_ESTUDIANTE,
  NOMBRE_GENERO,
  NOMBRE_PARENTESCO,
  obtenerEstudiante,
  retirarMatricula,
  type AcudienteVinculado,
  type Matricula,
} from '../../api/estudiantes';
import { useSesion } from '../../sesion/useSesion';
import { mensajeDeError } from '../academico/mensajes';
import DialogoAcudiente from './DialogoAcudiente';
import DialogoEstudiante from './DialogoEstudiante';
import DialogoMatricula from './DialogoMatricula';
import { TONO_ESTADO } from './ListaEstudiantes';
import Estado from '../../componentes/Estado';

function fechaCorta(iso: string | null) {
  if (!iso) return '';
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: ReactNode }) {
  return (
    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
      <Typography variant="caption" color="text.secondary">
        {etiqueta}
      </Typography>
      <Typography>{valor || '-'}</Typography>
    </Grid>
  );
}

/** Ficha del estudiante: datos personales, acudientes y matriculas por anio. */
export default function DetalleEstudiante() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const { tieneAlgunRol } = useSesion();
  const puedeEditar = tieneAlgunRol(['ADMINISTRADOR', 'SECRETARIA']);
  const [editando, setEditando] = useState(false);
  const [acudiente, setAcudiente] = useState<AcudienteVinculado | 'nuevo' | null>(null);
  const [matricula, setMatricula] = useState<Matricula | 'nueva' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const estudiante = useQuery({ queryKey: ['estudiante', id], queryFn: () => obtenerEstudiante(id) });
  const matriculas = useQuery({ queryKey: ['matriculas', id], queryFn: () => listarMatriculas(id) });

  const recargar = () => {
    queryClient.invalidateQueries({ queryKey: ['estudiante', id] });
    queryClient.invalidateQueries({ queryKey: ['matriculas', id] });
    queryClient.invalidateQueries({ queryKey: ['estudiantes'] });
  };

  const quitarAcudiente = useMutation({
    mutationFn: (acudienteId: number) => desvincularAcudiente(id, acudienteId),
    onMutate: () => setError(null),
    onError: (e) => setError(mensajeDeError(e)),
    onSuccess: (datos) => queryClient.setQueryData(['estudiante', id], datos),
  });

  const retirar = useMutation({
    mutationFn: ({ matriculaId, motivo }: { matriculaId: number; motivo: string }) =>
      retirarMatricula(matriculaId, motivo),
    onMutate: () => setError(null),
    onError: (e) => setError(mensajeDeError(e)),
    onSuccess: recargar,
  });

  if (estudiante.isError) {
    return <Alert severity="error">{estudiante.error.message}</Alert>;
  }
  const e = estudiante.data;
  if (!e) return null;

  return (
    <>
      <Button component={RouterLink} to="/estudiantes" size="small" sx={{ mb: 1, ml: -1 }}>
        Volver a estudiantes
      </Button>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <Typography variant="h5" component="h1">
          {e.nombres} {e.apellidos}
        </Typography>
        <Estado tono={TONO_ESTADO[e.estado]} texto={NOMBRE_ESTADO_ESTUDIANTE[e.estado]} />
        <Box sx={{ flexGrow: 1 }} />
        {puedeEditar && (
          <Button variant="outlined" onClick={() => setEditando(true)}>
            Editar ficha
          </Button>
        )}
      </Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Datos personales
          </Typography>
          <Grid container spacing={2}>
            <Dato etiqueta="Documento" valor={`${e.tipoDocumento} ${e.numeroDocumento}`} />
            <Dato etiqueta="Código" valor={e.codigo} />
            <Dato etiqueta="Fecha de nacimiento" valor={fechaCorta(e.fechaNacimiento)} />
            <Dato etiqueta="Género" valor={NOMBRE_GENERO[e.genero]} />
            <Dato etiqueta="Teléfono" valor={e.telefono} />
            <Dato etiqueta="Correo" valor={e.correo} />
            <Dato etiqueta="Dirección" valor={e.direccion} />
            <Dato etiqueta="EPS" valor={e.eps} />
            <Dato etiqueta="Grupo sanguíneo" valor={e.grupoSanguineo} />
            <Dato etiqueta="Discapacidad" valor={e.condicionDiscapacidad} />
            <Dato etiqueta="PIAR" valor={e.tienePiar ? 'Sí' : 'No'} />
            <Dato etiqueta="Otras condiciones" valor={e.condicionesEspeciales} />
          </Grid>
        </CardContent>
      </Card>

      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Acudientes
            </Typography>
            {puedeEditar && <Button onClick={() => setAcudiente('nuevo')}>Agregar acudiente</Button>}
          </Box>
          {e.acudientes.length === 0 ? (
            <Typography color="text.secondary">No tiene acudientes registrados</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Nombre</TableCell>
                  <TableCell>Parentesco</TableCell>
                  <TableCell>Documento</TableCell>
                  <TableCell>Teléfono</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {e.acudientes.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell>
                      {a.nombres} {a.apellidos} {a.principal && <Chip size="small" color="primary" label="Principal" />}
                    </TableCell>
                    <TableCell>{NOMBRE_PARENTESCO[a.parentesco]}</TableCell>
                    <TableCell>
                      {a.tipoDocumento} {a.numeroDocumento}
                    </TableCell>
                    <TableCell>{a.telefono}</TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      {puedeEditar && (
                        <>
                          <Button size="small" onClick={() => setAcudiente(a)}>
                            Editar
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            onClick={() => {
                              if (window.confirm(`Quitar a ${a.nombres} ${a.apellidos} como acudiente?`)) {
                                quitarAcudiente.mutate(a.id);
                              }
                            }}
                          >
                            Quitar
                          </Button>
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card variant="outlined">
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Matrículas
            </Typography>
            {puedeEditar && <Button onClick={() => setMatricula('nueva')}>Matricular</Button>}
          </Box>
          {matriculas.data?.length === 0 ? (
            <Typography color="text.secondary">No tiene matrículas</Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Año</TableCell>
                  <TableCell>Grupo</TableCell>
                  <TableCell>Sede</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {matriculas.data?.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell>{m.anio}</TableCell>
                    <TableCell>{m.grupo ?? 'Sin grupo'}</TableCell>
                    <TableCell>{m.sede}</TableCell>
                    <TableCell>
                      {m.estado === 'ACTIVA'
                        ? `Activa desde ${fechaCorta(m.fechaMatricula)}`
                        : `Retirada el ${fechaCorta(m.fechaRetiro)}: ${m.motivoRetiro}`}
                    </TableCell>
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      {puedeEditar && m.estado === 'ACTIVA' && (
                        <>
                          <Button size="small" onClick={() => setMatricula(m)}>
                            Cambiar grupo
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            onClick={() => {
                              const motivo = window.prompt('Motivo del retiro');
                              if (motivo?.trim()) retirar.mutate({ matriculaId: m.id, motivo });
                            }}
                          >
                            Retirar
                          </Button>
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {editando && (
        <DialogoEstudiante
          estudiante={e}
          alCerrar={() => setEditando(false)}
          alGuardar={(guardado) => {
            setEditando(false);
            queryClient.setQueryData(['estudiante', id], guardado);
            queryClient.invalidateQueries({ queryKey: ['estudiantes'] });
          }}
        />
      )}
      {acudiente !== null && (
        <DialogoAcudiente
          estudianteId={id}
          acudiente={acudiente === 'nuevo' ? null : acudiente}
          alCerrar={() => setAcudiente(null)}
          alGuardar={(guardado) => {
            setAcudiente(null);
            queryClient.setQueryData(['estudiante', id], guardado);
          }}
        />
      )}
      {matricula !== null && (
        <DialogoMatricula
          estudianteId={id}
          matricula={matricula === 'nueva' ? null : matricula}
          alCerrar={() => setMatricula(null)}
          alGuardar={() => {
            setMatricula(null);
            recargar();
          }}
        />
      )}
    </>
  );
}
