import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, Link as RouterLink } from 'react-router';
import { Alert, Box, Button, Container, Grid2 as Grid, Link, MenuItem, Paper, TextField, Typography } from '@mui/material';
import { registrarAcudiente } from '../api/auth';
import { ErrorApi } from '../api/cliente';
import escudo from '../assets/escudo.png';
import { CLAVE_SESION, useSesion } from '../sesion/useSesion';
import { NOMBRE_TIPO_DOCUMENTO, type TipoDocumento } from '../tipos';

// Las mismas reglas que valida el backend en RegistroAcudienteDto
const esquema = z
  .object({
    tipoDocumento: z.enum(['CC', 'CE', 'PPT', 'TI', 'RC']),
    numeroDocumento: z.string().trim().regex(/^[0-9A-Za-z]{3,20}$/, 'Solo letras y números, entre 3 y 20 caracteres'),
    nombres: z.string().trim().min(1, 'Ingrese sus nombres').max(100),
    apellidos: z.string().trim().min(1, 'Ingrese sus apellidos').max(100),
    telefono: z.string().trim().regex(/^[0-9 +]{7,20}$/, 'Ingrese un teléfono de contacto'),
    correo: z.union([z.literal(''), z.email('Correo inválido').max(150)]),
    contrasena: z.string().min(8, 'Mínimo 8 caracteres').max(72, 'Máximo 72 caracteres'),
    confirmacion: z.string(),
  })
  .refine((d) => d.contrasena === d.confirmacion, { path: ['confirmacion'], message: 'Las contraseñas no coinciden' });

type Datos = z.infer<typeof esquema>;

/** Registro publico para padres, madres y acudientes que van a hacer la matricula en linea. */
export default function Registro() {
  const { usuario } = useSesion();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    setError: marcarError,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      tipoDocumento: 'CC',
      numeroDocumento: '',
      nombres: '',
      apellidos: '',
      telefono: '',
      correo: '',
      contrasena: '',
      confirmacion: '',
    },
  });

  // Al crear la cuenta la sesion queda abierta y esta misma pantalla lleva a la matricula en linea
  if (usuario) {
    return <Navigate to={usuario.roles.includes('ACUDIENTE') ? '/matricula-en-linea' : '/'} replace />;
  }

  const enviar = async ({ confirmacion: _, ...datos }: Datos) => {
    setError(null);
    try {
      const actual = await registrarAcudiente(datos);
      queryClient.setQueryData(CLAVE_SESION, actual);
    } catch (e) {
      if (e instanceof ErrorApi) {
        setError(e.message);
        Object.entries(e.errores).forEach(([campo, mensaje]) => marcarError(campo as keyof Datos, { message: mensaje }));
      } else {
        setError('No hay conexión con el servidor');
      }
    }
  };

  const campo = (nombre: keyof Datos, etiqueta: string, ancho: number, extra: object = {}) => (
    <Grid size={{ xs: 12, sm: ancho }}>
      <TextField
        label={etiqueta}
        fullWidth
        {...register(nombre)}
        error={!!errors[nombre]}
        helperText={errors[nombre]?.message}
        {...extra}
      />
    </Grid>
  );

  return (
    <Container maxWidth="sm">
      <Paper sx={{ my: { xs: 3, sm: 6 }, p: { xs: 3, sm: 4 } }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <Box component="img" src={escudo} alt="Escudo de la IEM El Encano" sx={{ height: 64 }} />
          <Box>
            <Typography variant="h5" component="h1">
              Crear cuenta de acudiente
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Para hacer la matrícula en línea de sus hijos o acudidos.
            </Typography>
          </Box>
        </Box>
        <Typography variant="body2" sx={{ mb: 2 }}>
          Escriba sus propios datos, no los del estudiante. Para ingresar después, su usuario será su número de
          documento.
        </Typography>
        <Box component="form" onSubmit={handleSubmit(enviar)} noValidate>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="tipoDocumento"
                control={control}
                render={({ field }) => (
                  <TextField {...field} select label="Tipo de documento" fullWidth>
                    {(['CC', 'CE', 'PPT', 'TI'] as TipoDocumento[]).map((valor) => (
                      <MenuItem key={valor} value={valor}>
                        {NOMBRE_TIPO_DOCUMENTO[valor]}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            {campo('numeroDocumento', 'Número de documento', 6, { autoComplete: 'username' })}
            {campo('nombres', 'Nombres', 6)}
            {campo('apellidos', 'Apellidos', 6)}
            {campo('telefono', 'Celular', 6, { inputMode: 'tel' })}
            {campo('correo', 'Correo (opcional)', 6, { type: 'email' })}
            {campo('contrasena', 'Contraseña', 6, { type: 'password', autoComplete: 'new-password' })}
            {campo('confirmacion', 'Repita la contraseña', 6, { type: 'password', autoComplete: 'new-password' })}
          </Grid>
          <Button type="submit" variant="contained" size="large" fullWidth sx={{ mt: 3 }} disabled={isSubmitting}>
            Crear cuenta
          </Button>
          <Typography variant="body2" sx={{ mt: 2, textAlign: 'center' }}>
            ¿Ya tiene cuenta?{' '}
            <Link component={RouterLink} to="/login">
              Ingrese aquí
            </Link>
          </Typography>
        </Box>
      </Paper>
    </Container>
  );
}
