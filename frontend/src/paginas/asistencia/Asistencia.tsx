import { Box, Tab, Tabs, Typography } from '@mui/material';
import { Link as RouterLink, Navigate, Outlet, useLocation } from 'react-router';
import { useSesion } from '../../sesion/useSesion';
import type { Rol } from '../../tipos';

const SECCIONES: { texto: string; ruta: string; roles: Rol[] }[] = [
  { texto: 'Tomar asistencia', ruta: 'tomar', roles: ['DOCENTE', 'ADMINISTRADOR', 'COORDINADOR_ACADEMICO'] },
  {
    texto: 'Consolidado por grupo',
    ruta: 'consolidado',
    roles: ['DOCENTE', 'ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA'],
  },
];

/** Contenedor de asistencia: cada usuario ve las pestanas que le corresponden segun sus roles. */
export default function Asistencia() {
  const { pathname } = useLocation();
  const { tieneAlgunRol } = useSesion();
  const visibles = SECCIONES.filter((s) => tieneAlgunRol(s.roles));
  const actual = visibles.find((s) => pathname.endsWith('/' + s.ruta))?.ruta;
  if (!actual && visibles.length > 0) {
    return <Navigate to={visibles[0].ruta} replace />;
  }
  return (
    <>
      <Typography variant="h5" component="h1" sx={{ mb: 1 }}>
        Asistencia
      </Typography>
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs value={actual ?? false} variant="scrollable" scrollButtons="auto">
          {visibles.map((s) => (
            <Tab key={s.ruta} value={s.ruta} label={s.texto} component={RouterLink} to={s.ruta} />
          ))}
        </Tabs>
      </Box>
      <Outlet />
    </>
  );
}
