import { Box, Tab, Tabs } from '@mui/material';
import { Link as RouterLink, Navigate, Outlet, useLocation } from 'react-router';
import { useSesion } from '../../sesion/useSesion';
import type { Rol } from '../../tipos';
import Encabezado from '../../componentes/Encabezado';

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
      <Encabezado titulo="Asistencia" descripcion="Registro por clase y seguimiento de faltas según el SIEE." />
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3, mt: -1 }}>
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
