import { Box, Tab, Tabs } from '@mui/material';
import { Link as RouterLink, Navigate, Outlet, useLocation } from 'react-router';
import { useSesion } from '../../sesion/useSesion';
import type { Rol } from '../../tipos';
import Encabezado from '../../componentes/Encabezado';

const SECCIONES: { texto: string; ruta: string; roles: Rol[] }[] = [
  { texto: 'Planilla de notas', ruta: 'planilla', roles: ['DOCENTE', 'ADMINISTRADOR', 'COORDINADOR_ACADEMICO'] },
  {
    texto: 'Consolidado por grupo',
    ruta: 'consolidado',
    roles: ['DOCENTE', 'ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA'],
  },
  { texto: 'Escala de valoración', ruta: 'escala', roles: ['ADMINISTRADOR', 'COORDINADOR_ACADEMICO'] },
];

/** Contenedor de notas: cada usuario ve las pestanas que le corresponden segun sus roles. */
export default function Notas() {
  const { pathname } = useLocation();
  const { tieneAlgunRol } = useSesion();
  const visibles = SECCIONES.filter((s) => tieneAlgunRol(s.roles));
  const actual = visibles.find((s) => pathname.endsWith('/' + s.ruta))?.ruta;
  if (!actual && visibles.length > 0) {
    return <Navigate to={visibles[0].ruta} replace />;
  }
  return (
    <>
      <Encabezado titulo="Notas" descripcion="Actividades por dimensión (Saber, Hacer y Ser) y desempeño según el SIEE." />
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
