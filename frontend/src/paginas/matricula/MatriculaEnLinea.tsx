import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { Link as RouterLink } from 'react-router';
import { consultarOferta, listarMisSolicitudes, NOMBRE_ESTADO_SOLICITUD } from '../../api/matricula';
import Encabezado from '../../componentes/Encabezado';
import Estado from '../../componentes/Estado';
import { COLORES } from '../../tema';
import { mensajeDeError } from '../academico/mensajes';
import { TONO_SOLICITUD } from './estados';

/** Inicio de la matricula en linea para el acudiente: sus solicitudes y el boton para hacer una nueva. */
export default function MatriculaEnLinea() {
  const oferta = useQuery({ queryKey: ['oferta-matricula'], queryFn: consultarOferta });
  const solicitudes = useQuery({ queryKey: ['mis-solicitudes'], queryFn: listarMisSolicitudes });

  if (oferta.isPending || solicitudes.isPending) return <CircularProgress />;
  if (oferta.isError) return <Alert severity="error">{mensajeDeError(oferta.error)}</Alert>;
  if (solicitudes.isError) return <Alert severity="error">{mensajeDeError(solicitudes.error)}</Alert>;

  const abierta = oferta.data[0];

  return (
    <>
      <Encabezado
        titulo="Matrícula en línea"
        descripcion={
          abierta
            ? `La matrícula para el año ${abierta.anio} está abierta. Haga una solicitud por cada estudiante.`
            : 'En este momento no hay matrícula en línea abierta. Cuando el colegio la abra podrá hacer la solicitud aquí.'
        }
        acciones={
          abierta && (
            <Button variant="contained" component={RouterLink} to="/matricula-en-linea/nueva">
              Nueva solicitud
            </Button>
          )
        }
      />

      {abierta && solicitudes.data.length === 0 && (
        <Paper sx={{ p: 2.5, mb: 2.5 }}>
          <Typography sx={{ fontWeight: 600, mb: 1 }}>Cómo funciona</Typography>
          <Box component="ol" sx={{ m: 0, pl: 2.5, '& li': { mb: 0.5 } }}>
            <li>Escriba los datos del estudiante.</li>
            <li>Suba una foto o PDF de cada documento que pide el colegio.</li>
            <li>Envíe la solicitud. La secretaría la revisa y le avisa aquí si hay que corregir algo.</li>
            <li>Cuando la aprueben, el estudiante queda matriculado.</li>
          </Box>
        </Paper>
      )}

      {solicitudes.data.length > 0 && (
        <Paper>
          {solicitudes.data.map((s, i) => (
            <Box
              key={s.id}
              component={RouterLink}
              to={`/matricula-en-linea/${s.id}`}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 2,
                px: 2.5,
                py: 1.75,
                color: 'inherit',
                textDecoration: 'none',
                borderTop: i === 0 ? 'none' : `1px solid ${COLORES.linea}`,
                '&:hover': { bgcolor: COLORES.encabezado },
              }}
            >
              <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                <Typography sx={{ fontWeight: 600 }}>{s.estudiante}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {s.grado}, {s.sede}. Matrícula {s.anio}
                </Typography>
              </Box>
              <Stack sx={{ alignItems: 'flex-end' }}>
                <Estado tono={TONO_SOLICITUD[s.estado]} texto={NOMBRE_ESTADO_SOLICITUD[s.estado]} />
              </Stack>
            </Box>
          ))}
        </Paper>
      )}
    </>
  );
}
