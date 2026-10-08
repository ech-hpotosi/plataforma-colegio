import { Box, CircularProgress } from '@mui/material';
import { Navigate, Outlet, useLocation } from 'react-router';
import { useSesion } from '../sesion/useSesion';
import type { Rol } from '../tipos';

interface Props {
  /** Si se indica, el usuario debe tener al menos uno de estos roles. */
  roles?: Rol[];
}

/** Deja pasar solo a usuarios con sesion y, si se piden, con alguno de los roles indicados. */
export default function RutaProtegida({ roles }: Props) {
  const { usuario, cargando, tieneAlgunRol } = useSesion();
  const ubicacion = useLocation();

  if (cargando) {
    return (
      <Box sx={{ mt: 8, textAlign: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }
  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />;
  }
  if (roles && !tieneAlgunRol(roles)) {
    return <Navigate to="/sin-permiso" replace />;
  }
  return <Outlet />;
}
