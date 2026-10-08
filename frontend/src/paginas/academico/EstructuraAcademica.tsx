import { Box, Tab, Tabs, Typography } from '@mui/material';
import { Link as RouterLink, Outlet, useLocation } from 'react-router';

const SECCIONES = [
  { texto: 'Sedes', ruta: 'sedes' },
  { texto: 'Anios lectivos', ruta: 'anios' },
  { texto: 'Grados', ruta: 'grados' },
  { texto: 'Areas y asignaturas', ruta: 'areas' },
  { texto: 'Plan de estudios', ruta: 'plan' },
  { texto: 'Docentes', ruta: 'docentes' },
  { texto: 'Grupos y carga', ruta: 'grupos' },
];

/** Contenedor de las pantallas de estructura academica, con una pestana por seccion. */
export default function EstructuraAcademica() {
  const { pathname } = useLocation();
  const actual = SECCIONES.find((s) => pathname.endsWith('/' + s.ruta))?.ruta ?? false;
  return (
    <>
      <Typography variant="h5" component="h1" sx={{ mb: 1 }}>
        Estructura academica
      </Typography>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={actual} variant="scrollable" scrollButtons="auto">
          {SECCIONES.map((s) => (
            <Tab key={s.ruta} value={s.ruta} label={s.texto} component={RouterLink} to={s.ruta} />
          ))}
        </Tabs>
      </Box>
      <Outlet />
    </>
  );
}
