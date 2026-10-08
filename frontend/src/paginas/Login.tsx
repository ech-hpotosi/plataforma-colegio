import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { Alert, Box, Button, Container, Paper, TextField, Typography } from '@mui/material';
import { iniciarSesion } from '../api/auth';
import { ErrorApi } from '../api/cliente';
import { CLAVE_SESION, useSesion } from '../sesion/useSesion';

const esquema = z.object({
  nombreUsuario: z.string().trim().min(1, 'Ingrese el usuario'),
  contrasena: z.string().min(1, 'Ingrese la contrasena'),
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
      setError(e instanceof ErrorApi ? e.message : 'No hay conexion con el servidor');
    }
  };

  return (
    <Container maxWidth="xs">
      <Paper sx={{ mt: 8, p: 4 }} elevation={3}>
        <Typography variant="h5" component="h1" align="center" gutterBottom>
          IEM El Encano
        </Typography>
        <Typography variant="subtitle1" align="center" color="text.secondary">
          Plataforma escolar
        </Typography>
        <Typography variant="caption" component="p" align="center" color="secondary" gutterBottom>
          Estudio, trabajo y ciencia
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
            label="Contrasena"
            type="password"
            fullWidth
            margin="normal"
            autoComplete="current-password"
            {...register('contrasena')}
            error={!!errors.contrasena}
            helperText={errors.contrasena?.message}
          />
          <Button type="submit" variant="contained" fullWidth sx={{ mt: 2 }} disabled={isSubmitting}>
            Ingresar
          </Button>
        </Box>
      </Paper>
    </Container>
  );
}
