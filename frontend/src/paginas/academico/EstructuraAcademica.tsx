import { Box, Tab, Tabs } from '@mui/material';
import { Link as RouterLink, Outlet, useLocation } from 'react-router';
import Encabezado from '../../componentes/Encabezado';

const SECCIONES = [
  { texto: 'Sedes', ruta: 'sedes' },
  { texto: 'Años lectivos', ruta: 'anios' },
  { texto: 'Grados', ruta: 'grados' },
  { texto: 'Áreas y asignaturas', ruta: 'areas' },
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
      <Encabezado titulo="Estructura académica" descripcion="Lo que se configura antes de iniciar el año: sedes, periodos, plan de estudios, grupos y docentes." />
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3, mt: -1 }}>
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
