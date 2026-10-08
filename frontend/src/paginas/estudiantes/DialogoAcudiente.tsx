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
  actualizarAcudiente,
  buscarPersonaPorDocumento,
  NOMBRE_PARENTESCO,
  vincularAcudiente,
  type AcudienteVinculado,
  type Estudiante,
  type Parentesco,
} from '../../api/estudiantes';
import { NOMBRE_TIPO_DOCUMENTO, type TipoDocumento } from '../../tipos';

const esquema = z.object({
  tipoDocumento: z.enum(['RC', 'TI', 'CC', 'CE', 'PPT']),
  numeroDocumento: z.string().trim().regex(/^[0-9A-Za-z]{3,20}$/, 'Solo letras y numeros, entre 3 y 20 caracteres'),
  nombres: z.string().trim().min(1, 'Ingrese los nombres').max(100),
  apellidos: z.string().trim().min(1, 'Ingrese los apellidos').max(100),
  telefono: z.string().trim().max(20),
  correo: z.union([z.literal(''), z.email('Correo invalido').max(150)]),
  ocupacion: z.string().trim().max(100),
  parentesco: z.enum(Object.keys(NOMBRE_PARENTESCO) as [Parentesco, ...Parentesco[]], 'Seleccione el parentesco'),
  principal: z.boolean(),
});

type Datos = z.infer<typeof esquema>;

interface Props {
  estudianteId: number;
  /** Acudiente a editar, o null para agregar uno. */
  acudiente: AcudienteVinculado | null;
  alCerrar: () => void;
  alGuardar: (estudiante: Estudiante) => void;
}

/**
 * Agrega o edita un acudiente. Al escribir el documento de una persona que ya existe
 * (por ejemplo, la madre de un hermano) se llenan sus datos automaticamente.
 */
export default function DialogoAcudiente({ estudianteId, acudiente, alCerrar, alGuardar }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    getValues,
    reset,
    setError: marcarError,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      tipoDocumento: acudiente?.tipoDocumento ?? 'CC',
      numeroDocumento: acudiente?.numeroDocumento ?? '',
      nombres: acudiente?.nombres ?? '',
      apellidos: acudiente?.apellidos ?? '',
      telefono: acudiente?.telefono ?? '',
      correo: acudiente?.correo ?? '',
      ocupacion: acudiente?.ocupacion ?? '',
      parentesco: acudiente?.parentesco,
      principal: acudiente?.principal ?? false,
    },
  });

  const buscarPersona = async () => {
    const { tipoDocumento, numeroDocumento } = getValues();
    if (acudiente || numeroDocumento.trim().length < 3) return;
    try {
      const persona = await buscarPersonaPorDocumento(tipoDocumento, numeroDocumento.trim());
      reset({
        ...getValues(),
        nombres: persona.nombres,
        apellidos: persona.apellidos,
        telefono: persona.telefono ?? '',
        correo: persona.correo ?? '',
        ocupacion: persona.ocupacion ?? '',
      });
      setAviso('Esta persona ya estaba registrada; se cargaron sus datos.');
    } catch {
      setAviso(null);
    }
  };

  const guardar = async (datos: Datos) => {
    setError(null);
    try {
      const estudiante = acudiente
        ? await actualizarAcudiente(estudianteId, acudiente.id, datos)
        : await vincularAcudiente(estudianteId, datos);
      alGuardar(estudiante);
    } catch (e) {
      if (e instanceof ErrorApi) {
        setError(e.message);
        Object.entries(e.errores).forEach(([campo, mensaje]) =>
          marcarError(campo as keyof Datos, { message: mensaje }),
        );
      } else {
        setError('No hay conexion con el servidor');
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
    <Dialog open onClose={alCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{acudiente ? 'Editar acudiente' : 'Agregar acudiente'}</DialogTitle>
      <form onSubmit={handleSubmit(guardar)} noValidate>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          {aviso && (
            <Alert severity="info" sx={{ mb: 2 }}>
              {aviso}
            </Alert>
          )}
          <Grid container spacing={2} sx={{ pt: 1 }}>
            <Grid size={{ xs: 12, sm: 5 }}>
              <Controller
                name="tipoDocumento"
                control={control}
                render={({ field }) => (
                  <TextField {...field} select label="Tipo de documento" fullWidth disabled={!!acudiente}>
                    {Object.entries(NOMBRE_TIPO_DOCUMENTO).map(([valor, nombre]) => (
                      <MenuItem key={valor} value={valor as TipoDocumento}>
                        {nombre}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            {campo('numeroDocumento', 'Numero de documento', 7, {
              disabled: !!acudiente,
              ...register('numeroDocumento', { onBlur: buscarPersona }),
            })}
            {campo('nombres', 'Nombres', 6)}
            {campo('apellidos', 'Apellidos', 6)}
            {campo('telefono', 'Telefono', 6)}
            {campo('correo', 'Correo', 6, { type: 'email' })}
            {campo('ocupacion', 'Ocupacion', 6)}
            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="parentesco"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    value={field.value ?? ''}
                    select
                    label="Parentesco"
                    fullWidth
                    error={!!errors.parentesco}
                    helperText={errors.parentesco?.message}
                  >
                    {Object.entries(NOMBRE_PARENTESCO).map(([valor, nombre]) => (
                      <MenuItem key={valor} value={valor}>
                        {nombre}
                      </MenuItem>
                    ))}
                  </TextField>
                )}
              />
            </Grid>
            <Grid size={12}>
              <Controller
                name="principal"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    label="Acudiente principal (responsable ante el colegio)"
                    control={<Checkbox checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                  />
                )}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
