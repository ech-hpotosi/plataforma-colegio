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
  FormControlLabel,
  Grid2 as Grid,
  MenuItem,
  TextField,
} from '@mui/material';
import { ErrorApi } from '../../api/cliente';
import {
  actualizarEstudiante,
  crearEstudiante,
  NOMBRE_GENERO,
  type Estudiante,
} from '../../api/estudiantes';
import { NOMBRE_TIPO_DOCUMENTO, type TipoDocumento } from '../../tipos';

// Las mismas reglas que valida el backend en GuardarEstudianteDto
const esquema = z.object({
  tipoDocumento: z.enum(['RC', 'TI', 'CC', 'CE', 'PPT']),
  numeroDocumento: z.string().trim().regex(/^[0-9A-Za-z]{3,20}$/, 'Solo letras y números, entre 3 y 20 caracteres'),
  nombres: z.string().trim().min(1, 'Ingrese los nombres').max(100),
  apellidos: z.string().trim().min(1, 'Ingrese los apellidos').max(100),
  telefono: z.string().trim().max(20),
  correo: z.union([z.literal(''), z.email('Correo inválido').max(150)]),
  codigo: z.string().trim().max(20),
  fechaNacimiento: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingrese la fecha de nacimiento'),
  genero: z.enum(['FEMENINO', 'MASCULINO'], 'Seleccione el género'),
  direccion: z.string().trim().max(200),
  eps: z.string().trim().max(100),
  grupoSanguineo: z.string().trim().max(5),
  condicionDiscapacidad: z.string().trim().max(150),
  tienePiar: z.boolean(),
  condicionesEspeciales: z.string().trim().max(500),
});

type Datos = z.infer<typeof esquema>;

interface Props {
  /** Estudiante a editar, o null para crear uno nuevo. */
  estudiante: Estudiante | null;
  alCerrar: () => void;
  alGuardar: (guardado: Estudiante) => void;
}

export default function DialogoEstudiante({ estudiante, alCerrar, alGuardar }: Props) {
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
      tipoDocumento: estudiante?.tipoDocumento ?? 'TI',
      numeroDocumento: estudiante?.numeroDocumento ?? '',
      nombres: estudiante?.nombres ?? '',
      apellidos: estudiante?.apellidos ?? '',
      telefono: estudiante?.telefono ?? '',
      correo: estudiante?.correo ?? '',
      codigo: estudiante?.codigo ?? '',
      fechaNacimiento: estudiante?.fechaNacimiento ?? '',
      genero: estudiante?.genero,
      direccion: estudiante?.direccion ?? '',
      eps: estudiante?.eps ?? '',
      grupoSanguineo: estudiante?.grupoSanguineo ?? '',
      condicionDiscapacidad: estudiante?.condicionDiscapacidad ?? '',
      tienePiar: estudiante?.tienePiar ?? false,
      condicionesEspeciales: estudiante?.condicionesEspeciales ?? '',
    },
  });

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      const guardado = estudiante ? await actualizarEstudiante(estudiante.id, datos) : await crearEstudiante(datos);
      alGuardar(guardado);
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
    <Dialog open onClose={alCerrar} maxWidth="md" fullWidth>
      <DialogTitle>{estudiante ? 'Editar ficha del estudiante' : 'Nuevo estudiante'}</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Controller
                name="tipoDocumento"
                control={control}
                render={({ field }) => (
                  <TextField {...field} select label="Tipo de documento" fullWidth>
                    {Object.entries(NOMBRE_TIPO_DOCUMENTO).map(([valor, nombre]) => (
                      <MenuItem key={valor} value={valor as TipoDocumento}>
                        {nombre}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            {campo('numeroDocumento', 'Número de documento', 4)}
            {campo('codigo', 'Código interno (opcional)', 4)}
            {campo('nombres', 'Nombres', 6)}
            {campo('apellidos', 'Apellidos', 6)}
            {campo('fechaNacimiento', 'Fecha de nacimiento', 4, {
              type: 'date',
              slotProps: { inputLabel: { shrink: true } },
            })}
            <Grid size={{ xs: 12, sm: 4 }}>
              <Controller
                name="genero"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    value={field.value ?? ''}
                    select
                    label="Género"
                    fullWidth
                    error={!!errors.genero}
                    helperText={errors.genero?.message}
                  >
                    {Object.entries(NOMBRE_GENERO).map(([valor, nombre]) => (
                      <MenuItem key={valor} value={valor}>
                        {nombre}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            {campo('grupoSanguineo', 'Grupo sanguíneo (RH)', 4)}
            {campo('telefono', 'Teléfono', 4)}
            {campo('correo', 'Correo', 4, { type: 'email' })}
            {campo('eps', 'EPS', 4)}
            {campo('direccion', 'Dirección o vereda', 12)}
            {campo('condicionDiscapacidad', 'Discapacidad (si aplica)', 8)}
            <Grid size={{ xs: 12, sm: 4 }} sx={{ display: 'flex', alignItems: 'center' }}>
              <Controller
                name="tienePiar"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    label="Tiene PIAR"
                    control={<Checkbox checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                  />
                )}
              />
            </Grid>
            {campo('condicionesEspeciales', 'Otras condiciones (etnia, desplazamiento, etc.)', 12, {
              multiline: true,
              minRows: 2,
            })}
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
