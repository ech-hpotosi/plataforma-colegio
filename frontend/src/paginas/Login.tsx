import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { Alert, Box, Button, Container, Paper, TextField, Typography } from '@mui/material';
import { iniciarSesion } from '../api/auth';
import { ErrorApi } from '../api/cliente';
import escudo from '../assets/escudo.png';
import { CLAVE_SESION, useSesion } from '../sesion/useSesion';

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
    <Container maxWidth="xs">
      <Paper sx={{ mt: { xs: 4, sm: 8 }, p: { xs: 3, sm: 4 } }}>
        <Box sx={{ textAlign: 'center', mb: 2 }}>
          <Box component="img" src={escudo} alt="Escudo de la IEM El Encano" sx={{ height: 120 }} />
          <Typography variant="h5" component="h1" sx={{ mt: 1 }}>
            IEM El Encano
          </Typography>
          <Typography variant="subtitle1">Plataforma escolar</Typography>
          <Typography variant="caption" color="text.secondary">
            Estudio, trabajo y ciencia
          </Typography>
        </Box>
        <Box component="form" onSubmit={handleSubmit(enviar)} noValidate>
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
      </Paper>
    </Container>
  );
}
