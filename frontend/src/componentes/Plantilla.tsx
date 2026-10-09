import { useState } from 'react';
import { Box, Button, Drawer, Typography } from '@mui/material';
import { NavLink, Outlet, useLocation } from 'react-router';
import escudo from '../assets/escudo.png';
import { useSesion } from '../sesion/useSesion';
import { COLORES } from '../tema';
import { NOMBRE_ROL, type Rol } from '../tipos';

interface OpcionMenu {
  texto: string;
  ruta: string;
  roles?: Rol[];
}

interface GrupoMenu {
  titulo: string;
  opciones: OpcionMenu[];
}

const ANCHO_MENU = 248;

// Cada modulo nuevo agrega aqui su opcion de menu con los roles que la pueden ver
const MENU: GrupoMenu[] = [
  { titulo: 'General', opciones: [{ texto: 'Inicio', ruta: '/' }] },
  {
    titulo: 'Académico',
    opciones: [
      {
        texto: 'Estudiantes',
        ruta: '/estudiantes',
        roles: ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA'],
      },
      {
        texto: 'Asistencia',
        ruta: '/asistencia',
        roles: ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE'],
      },
      { texto: 'Estructura académica', ruta: '/academico', roles: ['ADMINISTRADOR', 'COORDINADOR_ACADEMICO'] },
    ],
  },
  { titulo: 'Administración', opciones: [{ texto: 'Usuarios', ruta: '/usuarios', roles: ['ADMINISTRADOR'] }] },
];

function MenuLateral({ alNavegar }: { alNavegar?: () => void }) {
  const { usuario, tieneAlgunRol, salir } = useSesion();
  const grupos = MENU.map((g) => ({
    ...g,
    opciones: g.opciones.filter((o) => !o.roles || tieneAlgunRol(o.roles)),
  })).filter((g) => g.opciones.length > 0);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: '#fff' }}>
      <Box sx={{ px: 2.5, pt: 3, pb: 2.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <Box component="img" src={escudo} alt="" sx={{ width: 48, height: 48, objectFit: 'contain' }} />
        <Box>
          <Typography sx={{ fontFamily: (t) => t.typography.h6.fontFamily, fontWeight: 600, lineHeight: 1.2 }}>
            IEM El Encano
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Estudio, trabajo y ciencia
          </Typography>
        </Box>
      </Box>

      <Box component="nav" aria-label="Menú principal" sx={{ flexGrow: 1, px: 1.5 }}>
        {grupos.map((grupo) => (
          <Box key={grupo.titulo} sx={{ mb: 2 }}>
            <Typography
              variant="caption"
              component="div"
              sx={{ px: 1.5, mb: 0.5, color: 'text.secondary', fontWeight: 600, letterSpacing: '0.04em' }}
            >
              {grupo.titulo}
            </Typography>
            {grupo.opciones.map((opcion) => (
              <Box
                key={opcion.ruta}
                component={NavLink}
                to={opcion.ruta}
                end={opcion.ruta === '/'}
                onClick={alNavegar}
                sx={{
                  display: 'block',
                  px: 1.5,
                  py: 0.9,
                  borderRadius: 1,
                  borderLeft: '3px solid transparent',
                  color: 'text.primary',
                  textDecoration: 'none',
                  fontWeight: 500,
                  '&:hover': { bgcolor: COLORES.encabezado },
                  '&.active': {
                    bgcolor: '#eef2f6',
                    borderLeftColor: COLORES.sol,
                    color: COLORES.lagunaOscuro,
                    fontWeight: 700,
                  },
                }}
              >
                {opcion.texto}
              </Box>
            ))}
          </Box>
        ))}
      </Box>

      <Box sx={{ px: 2.5, py: 2, borderTop: `1px solid ${COLORES.linea}` }}>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {usuario?.nombreCompleto}
        </Typography>
        <Typography variant="caption" color="text.secondary" component="div" sx={{ mb: 1 }}>
          {usuario?.roles.map((r) => NOMBRE_ROL[r]).join(', ')}
        </Typography>
        <Button variant="outlined" size="small" onClick={salir}>
          Cerrar sesión
        </Button>
      </Box>
    </Box>
  );
}

/**
 * Estructura de las pantallas internas: menu lateral fijo en computador y menu desplegable en celular.
 */
export default function Plantilla() {
  const [abierto, setAbierto] = useState(false);
  const { pathname } = useLocation();
  const bordeMenu = `1px solid ${COLORES.linea}`;

  return (
    <Box sx={{ minHeight: '100vh' }}>
      <Box sx={{ position: 'sticky', top: 0, zIndex: 1100 }}>
        {/* Barra superior solo en celular */}
        <Box
          sx={{
            display: { xs: 'flex', md: 'none' },
            alignItems: 'center',
            gap: 1.5,
            px: 2,
            py: 1,
            bgcolor: '#fff',
            borderBottom: bordeMenu,
          }}
        >
          <Box component="img" src={escudo} alt="" sx={{ width: 32, height: 32, objectFit: 'contain' }} />
          <Typography sx={{ flexGrow: 1, fontFamily: (t) => t.typography.h6.fontFamily, fontWeight: 600 }}>
            IEM El Encano
          </Typography>
          <Button variant="outlined" size="small" onClick={() => setAbierto(true)}>
            Menú
          </Button>
        </Box>
      </Box>

      <Box
        component="aside"
        sx={{
          display: { xs: 'none', md: 'block' },
          position: 'fixed',
          top: 4,
          bottom: 0,
          left: 0,
          width: ANCHO_MENU,
          borderRight: bordeMenu,
        }}
      >
        <MenuLateral />
      </Box>
      <Drawer
        open={abierto}
        onClose={() => setAbierto(false)}
        sx={{ display: { md: 'none' } }}
        slotProps={{ paper: { sx: { width: ANCHO_MENU } } }}
      >
        <MenuLateral key={pathname} alNavegar={() => setAbierto(false)} />
      </Drawer>

      <Box component="main" sx={{ ml: { md: `${ANCHO_MENU}px` }, px: { xs: 2, sm: 3, md: 5 }, py: { xs: 2.5, md: 4 } }}>
        <Box sx={{ maxWidth: 1200 }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
