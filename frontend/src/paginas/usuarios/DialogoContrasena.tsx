import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';
import { cambiarContrasena, type Usuario } from '../../api/usuarios';
import { ErrorApi } from '../../api/cliente';

const esquema = z
  .object({
    contrasena: z.string().min(8, 'Mínimo 8 caracteres').max(72, 'Máximo 72 caracteres'),
    confirmacion: z.string(),
  })
  .refine((d) => d.contrasena === d.confirmacion, {
    path: ['confirmacion'],
    message: 'Las contraseñas no coinciden',
  });

type Datos = z.infer<typeof esquema>;

interface Props {
  usuario: Usuario;
  alCerrar: () => void;
}

/** El administrador asigna una contrasena nueva. Tambien desbloquea la cuenta. */
export default function DialogoContrasena({ usuario, alCerrar }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [listo, setListo] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({ resolver: zodResolver(esquema), defaultValues: { contrasena: '', confirmacion: '' } });

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      await cambiarContrasena(usuario.id, datos.contrasena);
      setListo(true);
    } catch (e) {
      setError(e instanceof ErrorApi ? e.message : 'No hay conexión con el servidor');
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="xs" fullWidth>
      <DialogTitle>Cambiar contraseña de {usuario.nombreUsuario}</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && <Alert severity="error">{error}</Alert>}
          {listo ? (
            <Alert severity="success">Contraseña actualizada</Alert>
          ) : (
            <>
              <TextField
                label="Contraseña nueva"
                type="password"
                fullWidth
                margin="normal"
                autoComplete="new-password"
                {...register('contrasena')}
                error={!!errors.contrasena}
                helperText={errors.contrasena?.message}
              />
              <TextField
                label="Repita la contraseña"
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
          <Button onClick={alCerrar}>{listo ? 'Cerrar' : 'Cancelar'}</Button>
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
