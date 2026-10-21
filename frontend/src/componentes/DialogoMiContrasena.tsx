import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';
import { cambiarMiContrasena } from '../api/auth';
import { ErrorApi } from '../api/cliente';

const esquema = z
  .object({
    actual: z.string().min(1, 'Ingrese la contraseña actual'),
    nueva: z.string().min(8, 'Mínimo 8 caracteres').max(72, 'Máximo 72 caracteres'),
    confirmacion: z.string(),
  })
  .refine((d) => d.nueva === d.confirmacion, {
    path: ['confirmacion'],
    message: 'Las contraseñas no coinciden',
  });

type Datos = z.infer<typeof esquema>;

/** Cada usuario cambia su propia contrasena desde el menu lateral. */
export default function DialogoMiContrasena({ alCerrar }: { alCerrar: () => void }) {
  const [error, setError] = useState<string | null>(null);
  const [listo, setListo] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({ resolver: zodResolver(esquema), defaultValues: { actual: '', nueva: '', confirmacion: '' } });

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      await cambiarMiContrasena(datos.actual, datos.nueva);
      setListo(true);
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No hay conexión con el servidor');
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>Cambiar mi contraseña</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && <Alert severity="error">{error}</Alert>}
          {listo ? (
            <Alert severity="success">Contraseña actualizada. Úsela la próxima vez que ingrese.</Alert>
          ) : (
            <>
              <TextField
                label="Contraseña actual"
                type="password"
                fullWidth
                margin="normal"
                autoComplete="current-password"
                {...register('actual')}
                error={!!errors.actual}
                helperText={errors.actual?.message}
              />
              <TextField
                label="Contraseña nueva"
                type="password"
                fullWidth
                margin="normal"
                autoComplete="new-password"
                {...register('nueva')}
                error={!!errors.nueva}
                helperText={errors.nueva?.message}
              />
              <TextField
                label="Repita la contraseña nueva"
                type="password"
                fullWidth
                margin="normal"
                autoComplete="new-password"
                {...register('confirmacion')}
                error={!!errors.confirmacion}
                helperText={errors.confirmacion?.message}
              />
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={alCerrar}>
            {listo ? 'Cerrar' : 'Cancelar'}
          </Button>
          {!listo && (
            <Button type="submit" variant="contained" disabled={isSubmitting}>
              Guardar
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  );
}
