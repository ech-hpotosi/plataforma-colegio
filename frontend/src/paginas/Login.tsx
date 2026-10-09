import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { Alert, Box, Button, TextField, Typography } from '@mui/material';
import { iniciarSesion } from '../api/auth';
import { ErrorApi } from '../api/cliente';
import escudo from '../assets/escudo.png';
import { FranjaBandera } from '../componentes/Plantilla';
import { CLAVE_SESION, useSesion } from '../sesion/useSesion';
import { COLORES } from '../tema';

const esquema = z.object({
  nombreUsuario: z.string().trim().min(1, 'Ingrese el usuario'),
  contrasena: z.string().min(1, 'Ingrese la contraseña'),
});

type DatosLogin = z.infer<typeof esquema>;

export default function Login() {
  const { usuario } = useSesion();
  const queryClient = useQueryClient();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosLogin>({ resolver: zodResolver(esquema) });

  const destino = (ubicacion.state as { desde?: string } | null)?.desde ?? '/';

  if (usuario) {
    return <Navigate to={destino} replace />;
  }

  const enviar = async (datos: DatosLogin) => {
    setError(null);
    try {
      const actual = await iniciarSesion(datos.nombreUsuario, datos.contrasena);
      queryClient.setQueryData(CLAVE_SESION, actual);
      navegar(destino, { replace: true });
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No hay conexión con el servidor');
    }
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <FranjaBandera />
      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: { xs: 'column', md: 'row' } }}>
        {/* Panel institucional: en celular se reduce a una cabecera */}
        <Box
          sx={{
            flex: { md: '0 0 44%' },
            bgcolor: COLORES.lagunaOscuro,
            color: '#fff',
            display: 'flex',
            flexDirection: { xs: 'row', md: 'column' },
            alignItems: { xs: 'center', md: 'flex-start' },
            justifyContent: { md: 'center' },
            gap: { xs: 2, md: 3 },
            px: { xs: 2.5, md: 8 },
            py: { xs: 2.5, md: 6 },
          }}
        >
          <Box
            sx={{
              bgcolor: '#fff',
              borderRadius: '50%',
              p: { xs: 0.75, md: 2 },
              width: { xs: 64, md: 150 },
              height: { xs: 64, md: 150 },
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
          >
            <Box component="img" src={escudo} alt="Escudo de la IEM El Encano" sx={{ width: '100%', height: '100%', objectFit: 'contain' }} />
          </Box>
          <Box>
            <Typography
              sx={{ fontFamily: (t) => t.typography.h4.fontFamily, fontWeight: 600, fontSize: { xs: '1.15rem', md: '2rem' }, lineHeight: 1.2 }}
            >
              Institución Educativa Municipal El Encano
            </Typography>
            <Typography sx={{ mt: 1, color: COLORES.sol, fontWeight: 600, display: { xs: 'none', md: 'block' } }}>
              Estudio, trabajo y ciencia
            </Typography>
            <Typography sx={{ mt: 2, opacity: 0.8, maxWidth: 420, display: { xs: 'none', md: 'block' } }}>
              Notas, asistencia y matrícula de nuestras 11 sedes, en un solo lugar.
            </Typography>
          </Box>
        </Box>

        <Box sx={{ flexGrow: 1, display: 'flex', alignItems: { xs: 'flex-start', md: 'center' }, justifyContent: 'center', px: 2, py: { xs: 4, md: 6 } }}>
          <Box sx={{ width: '100%', maxWidth: 380 }}>
            <Typography variant="h5" component="h1">
              Ingresar
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              Use el usuario y la contraseña que le entregó el colegio.
            </Typography>
            <Box component="form" onSubmit={handleSubmit(enviar)} noValidate sx={{ mt: 2 }}>
              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}
              <TextField
                label="Usuario"
                fullWidth
                margin="normal"
                autoComplete="username"
                autoFocus
                {...register('nombreUsuario')}
                error={!!errors.nombreUsuario}
                helperText={errors.nombreUsuario?.message}
              />
              <TextField
                label="Contraseña"
                type="password"
                fullWidth
                margin="normal"
                autoComplete="current-password"
                {...register('contrasena')}
                error={!!errors.contrasena}
                helperText={errors.contrasena?.message}
              />
              <Button type="submit" variant="contained" size="large" fullWidth sx={{ mt: 2 }} disabled={isSubmitting}>
                Ingresar
              </Button>
            </Box>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
