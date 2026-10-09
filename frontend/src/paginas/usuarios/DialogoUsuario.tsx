import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Alert,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  FormGroup,
  FormHelperText,
  FormLabel,
  Grid2 as Grid,
  MenuItem,
  Switch,
  TextField,
} from '@mui/material';
import { actualizarUsuario, crearUsuario, type Usuario } from '../../api/usuarios';
import { ErrorApi } from '../../api/cliente';
import { NOMBRE_ROL, NOMBRE_TIPO_DOCUMENTO, ROLES, type Rol, type TipoDocumento } from '../../tipos';

// Las mismas reglas que valida el backend en CrearUsuarioDto y ActualizarUsuarioDto
const esquema = z.object({
  tipoDocumento: z.enum(['RC', 'TI', 'CC', 'CE', 'PPT']),
  numeroDocumento: z.string().trim().regex(/^[0-9A-Za-z]{3,20}$/, 'Solo letras y números, entre 3 y 20 caracteres'),
  nombres: z.string().trim().min(1, 'Ingrese los nombres').max(100),
  apellidos: z.string().trim().min(1, 'Ingrese los apellidos').max(100),
  telefono: z.string().trim().max(20),
  correo: z.union([z.literal(''), z.email('Correo inválido').max(150)]),
  nombreUsuario: z
    .string()
    .trim()
    .regex(/^[a-z0-9._-]{3,50}$/, 'Use minúsculas, números, punto, guion o guion bajo (3 a 50)'),
  contrasena: z.string(),
  activo: z.boolean(),
  roles: z.array(z.enum(ROLES as [Rol, ...Rol[]])).min(1, 'Seleccione al menos un rol'),
});

type Datos = z.infer<typeof esquema>;

interface Props {
  /** Usuario a editar, o null para crear uno nuevo. */
  usuario: Usuario | null;
  alCerrar: () => void;
  alGuardar: () => void;
}

export default function DialogoUsuario({ usuario, alCerrar, alGuardar }: Props) {
  const esNuevo = usuario === null;
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    setError: marcarError,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({
    resolver: zodResolver(
      esNuevo
        ? esquema.refine((d) => d.contrasena.length >= 8 && d.contrasena.length <= 72, {
            path: ['contrasena'],
            message: 'La contraseña debe tener entre 8 y 72 caracteres',
          })
        : esquema,
    ),
    defaultValues: {
      tipoDocumento: usuario?.tipoDocumento ?? 'CC',
      numeroDocumento: usuario?.numeroDocumento ?? '',
      nombres: usuario?.nombres ?? '',
      apellidos: usuario?.apellidos ?? '',
      telefono: usuario?.telefono ?? '',
      correo: usuario?.correo ?? '',
      nombreUsuario: usuario?.nombreUsuario ?? '',
      contrasena: '',
      activo: usuario?.activo ?? true,
      roles: usuario?.roles ?? [],
    },
  });

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      if (esNuevo) {
        await crearUsuario({
          tipoDocumento: datos.tipoDocumento,
          numeroDocumento: datos.numeroDocumento,
          nombres: datos.nombres,
          apellidos: datos.apellidos,
          telefono: datos.telefono,
          correo: datos.correo,
          nombreUsuario: datos.nombreUsuario,
          contrasena: datos.contrasena,
          roles: datos.roles,
        });
      } else {
        await actualizarUsuario(usuario.id, {
          nombres: datos.nombres,
          apellidos: datos.apellidos,
          telefono: datos.telefono,
          correo: datos.correo,
          activo: datos.activo,
          roles: datos.roles,
        });
      }
      alGuardar();
    } catch (e) {
      if (e instanceof ErrorApi) {
        setError(e.message);
        Object.entries(e.errores).forEach(([campo, mensaje]) =>
          marcarError(campo as keyof Datos, { message: mensaje }),
        );
      } else {
        setError('No hay conexión con el servidor');
      }
    }
  };

  return (
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{esNuevo ? 'Nuevo usuario' : `Editar ${usuario.nombreUsuario}`}</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 5 }}>
              <Controller
                name="tipoDocumento"
                control={control}
                render={({ field }) => (
                  <TextField {...field} select label="Tipo de documento" fullWidth disabled={!esNuevo}>
                    {Object.entries(NOMBRE_TIPO_DOCUMENTO).map(([valor, nombre]) => (
                      <MenuItem key={valor} value={valor as TipoDocumento}>
                        {nombre}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 7 }}>
              <TextField
                label="Número de documento"
                fullWidth
                disabled={!esNuevo}
                {...register('numeroDocumento')}
                error={!!errors.numeroDocumento}
                helperText={errors.numeroDocumento?.message}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Nombres"
                fullWidth
                {...register('nombres')}
                error={!!errors.nombres}
                helperText={errors.nombres?.message}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Apellidos"
                fullWidth
                {...register('apellidos')}
                error={!!errors.apellidos}
                helperText={errors.apellidos?.message}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Teléfono"
                fullWidth
                {...register('telefono')}
                error={!!errors.telefono}
                helperText={errors.telefono?.message}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Correo"
                type="email"
                fullWidth
                {...register('correo')}
                error={!!errors.correo}
                helperText={errors.correo?.message}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Nombre de usuario"
                fullWidth
                disabled={!esNuevo}
                {...register('nombreUsuario')}
                error={!!errors.nombreUsuario}
                helperText={errors.nombreUsuario?.message}
              />
            </Grid>
            {esNuevo && (
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Contraseña inicial"
                  type="password"
                  fullWidth
                  autoComplete="new-password"
                  {...register('contrasena')}
                  error={!!errors.contrasena}
                  helperText={errors.contrasena?.message}
                />
              </Grid>
            )}
            <Grid size={12}>
              <Controller
                name="roles"
                control={control}
                render={({ field }) => (
                  <FormControl error={!!errors.roles} component="fieldset">
                    <FormLabel component="legend">Roles</FormLabel>
                    <FormGroup row>
                      {ROLES.map((rol) => (
                        <FormControlLabel
                          key={rol}
                          label={NOMBRE_ROL[rol]}
                          control={
                            <Checkbox
                              checked={field.value.includes(rol)}
                              onChange={(e) =>
                                field.onChange(
                                  e.target.checked ? [...field.value, rol] : field.value.filter((r) => r !== rol),
                                )
                              }
                            />
                          }
                        />
                      ))}
                    </FormGroup>
                    <FormHelperText>{errors.roles?.message}</FormHelperText>
                  </FormControl>
                )}
              />
            </Grid>
            {!esNuevo && (
              <Grid size={12}>
                <Controller
                  name="activo"
                  control={control}
                  render={({ field }) => (
                    <FormControlLabel
                      label="Usuario activo"
                      control={<Switch checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                    />
                  )}
                />
              </Grid>
            )}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
