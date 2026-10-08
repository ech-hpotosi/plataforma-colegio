import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material';
import { Link as RouterLink, Outlet } from 'react-router';
import { useSesion } from '../sesion/useSesion';
import type { Rol } from '../tipos';

interface OpcionMenu {
  texto: string;
  ruta: string;
  roles?: Rol[];
}

// Cada modulo nuevo agrega aqui su opcion de menu con los roles que la pueden ver
const MENU: OpcionMenu[] = [
  { texto: 'Inicio', ruta: '/' },
  { texto: 'Usuarios', ruta: '/usuarios', roles: ['ADMINISTRADOR'] },
];

/** Barra superior con el menu segun los roles del usuario y el boton de cerrar sesion. */
export default function Plantilla() {
  const { usuario, tieneAlgunRol, salir } = useSesion();
  return (
    <>
      <AppBar position="static">
        <Toolbar sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Typography variant="h6" component="div" sx={{ mr: 2 }}>
            IEM El Encano
          </Typography>
          <Box sx={{ flexGrow: 1, display: 'flex', gap: 1 }}>
            {MENU.filter((opcion) => !opcion.roles || tieneAlgunRol(opcion.roles)).map((opcion) => (
              <Button key={opcion.ruta} color="inherit" component={RouterLink} to={opcion.ruta}>
                {opcion.texto}
              </Button>
            ))}
          </Box>
          <Typography variant="body2" sx={{ display: { xs: 'none', sm: 'block' } }}>
            {usuario?.nombreCompleto}
          </Typography>
          <Button color="inherit" onClick={salir}>
            Cerrar sesion
          </Button>
        </Toolbar>
      </AppBar>
      <Container sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </>
  );
}
