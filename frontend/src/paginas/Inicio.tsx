import { Box, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router';
import Encabezado from '../componentes/Encabezado';
import { useSesion } from '../sesion/useSesion';
import { COLORES } from '../tema';
import { NOMBRE_ROL, type Rol } from '../tipos';

interface Acceso {
  titulo: string;
  descripcion: string;
  ruta: string;
  roles: Rol[];
}

const ACCESOS: Acceso[] = [
  {
    titulo: 'Tomar asistencia',
    descripcion: 'Marque las faltas, retardos y permisos de la clase de hoy.',
    ruta: '/asistencia/tomar',
    roles: ['DOCENTE', 'ADMINISTRADOR', 'COORDINADOR_ACADEMICO'],
  },
  {
    titulo: 'Consolidado de asistencia',
    descripcion: 'Faltas por grupo y estudiantes que pasan el límite del SIEE.',
    ruta: '/asistencia/consolidado',
    roles: ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE'],
  },
  {
    titulo: 'Estudiantes',
    descripcion: 'Fichas, acudientes y matrículas.',
    ruta: '/estudiantes',
    roles: ['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA'],
  },
  {
    titulo: 'Estructura académica',
    descripcion: 'Sedes, años lectivos, plan de estudios, grupos y carga académica.',
    ruta: '/academico',
    roles: ['ADMINISTRADOR', 'COORDINADOR_ACADEMICO'],
  },
  {
    titulo: 'Usuarios',
    descripcion: 'Cuentas, roles y contraseñas.',
    ruta: '/usuarios',
    roles: ['ADMINISTRADOR'],
  },
];

const FECHA = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

export default function Inicio() {
  const { usuario, tieneAlgunRol } = useSesion();
  const accesos = ACCESOS.filter((a) => tieneAlgunRol(a.roles));
  const hoy = FECHA.format(new Date());

  return (
    <>
      <Encabezado
        titulo={`Bienvenido, ${usuario?.nombreCompleto ?? ''}`}
        descripcion={`${hoy.charAt(0).toUpperCase()}${hoy.slice(1)}. ${usuario?.roles.map((r) => NOMBRE_ROL[r]).join(', ') ?? ''}`}
      />
      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1fr 1fr 1fr' } }}>
        {accesos.map((a) => (
          <Box
            key={a.ruta}
            component={RouterLink}
            to={a.ruta}
            sx={{
              display: 'block',
              p: 2.5,
              bgcolor: '#fff',
              border: `1px solid ${COLORES.linea}`,
              borderRadius: 1.5,
              color: 'inherit',
              textDecoration: 'none',
              '&:hover': { borderColor: COLORES.laguna, bgcolor: COLORES.encabezado },
            }}
          >
            <Typography sx={{ fontWeight: 700 }}>{a.titulo}</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {a.descripcion}
            </Typography>
          </Box>
        ))}
      </Box>
    </>
  );
}
