import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Alert, Box, Button, Grid2 as Grid, MenuItem, TextField, Typography } from '@mui/material';
import { ErrorApi } from '../../api/cliente';
import { NOMBRE_GENERO, NOMBRE_PARENTESCO } from '../../api/estudiantes';
import {
  actualizarSolicitud,
  crearSolicitud,
  NOMBRE_TIPO_SOLICITUD,
  type OfertaMatricula,
  type Solicitud,
} from '../../api/matricula';
import { NOMBRE_TIPO_DOCUMENTO } from '../../tipos';

// Las mismas reglas que valida el backend en DatosSolicitudDto
const esquema = z.object({
  sedeId: z.number({ error: 'Seleccione la sede' }),
  gradoId: z.number({ error: 'Seleccione el grado' }),
  tipo: z.enum(['NUEVO', 'ANTIGUO'], 'Indique si es estudiante nuevo o antiguo'),
  parentesco: z.enum(['MADRE', 'PADRE', 'ABUELO', 'TIO', 'HERMANO', 'TUTOR', 'OTRO'], 'Seleccione el parentesco'),
  tipoDocumento: z.enum(['RC', 'TI', 'CC', 'CE', 'PPT']),
  numeroDocumento: z.string().trim().regex(/^[0-9A-Za-z]{3,20}$/, 'Solo letras y números, entre 3 y 20 caracteres'),
  nombres: z.string().trim().min(1, 'Ingrese los nombres').max(100),
  apellidos: z.string().trim().min(1, 'Ingrese los apellidos').max(100),
  fechaNacimiento: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingrese la fecha de nacimiento'),
  genero: z.enum(['FEMENINO', 'MASCULINO'], 'Seleccione el género'),
  direccion: z.string().trim().min(1, 'Ingrese la dirección o vereda').max(200),
  telefono: z.string().trim().max(20),
  correo: z.union([z.literal(''), z.email('Correo inválido').max(150)]),
  eps: z.string().trim().max(100),
  grupoSanguineo: z.string().trim().max(5),
  condicionDiscapacidad: z.string().trim().max(150),
  condicionesEspeciales: z.string().trim().max(500),
  institucionAnterior: z.string().trim().max(150),
});

type Datos = z.infer<typeof esquema>;

interface Props {
  oferta: OfertaMatricula;
  /** Solicitud a corregir, o null para una nueva. */
  solicitud: Solicitud | null;
  alGuardar: (guardada: Solicitud) => void;
  alCancelar?: () => void;
}

/** Datos del estudiante que diligencia el acudiente. */
export default function FormularioSolicitud({ oferta, solicitud, alGuardar, alCancelar }: Props) {
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    watch,
    setError: marcarError,
    formState: { errors, isSubmitting },
  } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      sedeId: solicitud?.sedeId,
      gradoId: solicitud?.gradoId,
      tipo: solicitud?.tipo,
      parentesco: solicitud?.parentesco,
      tipoDocumento: solicitud?.tipoDocumento ?? 'TI',
      numeroDocumento: solicitud?.numeroDocumento ?? '',
      nombres: solicitud?.nombres ?? '',
      apellidos: solicitud?.apellidos ?? '',
      fechaNacimiento: solicitud?.fechaNacimiento ?? '',
      genero: solicitud?.genero,
      direccion: solicitud?.direccion ?? '',
      telefono: solicitud?.telefono ?? '',
      correo: solicitud?.correo ?? '',
      eps: solicitud?.eps ?? '',
      grupoSanguineo: solicitud?.grupoSanguineo ?? '',
      condicionDiscapacidad: solicitud?.condicionDiscapacidad ?? '',
      condicionesEspeciales: solicitud?.condicionesEspeciales ?? '',
      institucionAnterior: solicitud?.institucionAnterior ?? '',
    },
  });
  const esNuevo = watch('tipo') === 'NUEVO';

  const guardar = async (datos: Datos) => {
    setError(null);
    const cuerpo = { ...datos, anioLectivoId: oferta.anioLectivoId };
    try {
      alGuardar(solicitud ? await actualizarSolicitud(solicitud.id, cuerpo) : await crearSolicitud(cuerpo));
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

  // Lista desplegable conectada al formulario; los ids numericos se guardan como numero
  const lista = (
    nombre: 'sedeId' | 'gradoId' | 'tipo' | 'parentesco' | 'tipoDocumento' | 'genero',
    etiqueta: string,
    opciones: [string | number, string][],
    ancho: number,
  ) => (
    <Grid size={{ xs: 12, sm: ancho }}>
      <Controller
        name={nombre}
        control={control}
        render={({ field }) => (
          <TextField
            select
            label={etiqueta}
            fullWidth
            value={field.value ?? ''}
            onChange={(e) => {
              const valor = e.target.value;
              field.onChange(typeof opciones[0]?.[0] === 'number' ? Number(valor) : valor);
            }}
            onBlur={field.onBlur}
            error={!!errors[nombre]}
            helperText={errors[nombre]?.message}
          >
            {opciones.map(([valor, texto]) => (
              <MenuItem key={valor} value={valor}>
                {texto}
              </MenuItem>
            ))}
          </TextField>
        )}
      />
    </Grid>
  );

  const titulo = (texto: string) => (
    <Grid size={12}>
      <Typography variant="subtitle1" component="h3" sx={{ fontWeight: 700, mt: 1 }}>
        {texto}
      </Typography>
    </Grid>
  );

  return (
    <Box component="form" onSubmit={handleSubmit(guardar)} noValidate>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      <Grid container spacing={2}>
        {titulo(`Matrícula ${oferta.anio}`)}
        {lista('sedeId', 'Sede donde va a estudiar', oferta.sedes.map((s) => [s.id, s.nombre]), 6)}
        {lista('gradoId', 'Grado al que entra', oferta.grados.map((g) => [g.id, g.nombre]), 6)}
        {lista('tipo', 'El estudiante', Object.entries(NOMBRE_TIPO_SOLICITUD), 6)}
        {lista('parentesco', 'Usted es su', Object.entries(NOMBRE_PARENTESCO), 6)}

        {titulo('Datos del estudiante')}
        {lista('tipoDocumento', 'Tipo de documento', Object.entries(NOMBRE_TIPO_DOCUMENTO), 6)}
        {campo('numeroDocumento', 'Número de documento', 6)}
        {campo('nombres', 'Nombres', 6)}
        {campo('apellidos', 'Apellidos', 6)}
        {campo('fechaNacimiento', 'Fecha de nacimiento', 6, { type: 'date', slotProps: { inputLabel: { shrink: true } } })}
        {lista('genero', 'Género', Object.entries(NOMBRE_GENERO), 6)}
        {campo('direccion', 'Dirección o vereda donde vive', 12)}
        {campo('telefono', 'Teléfono del estudiante (opcional)', 6, { inputMode: 'tel' })}
        {campo('correo', 'Correo del estudiante (opcional)', 6, { type: 'email' })}
        {esNuevo && campo('institucionAnterior', 'Colegio o escuela donde estudió el año pasado', 12)}

        {titulo('Salud')}
        {campo('eps', 'EPS o SISBÉN', 6)}
        {campo('grupoSanguineo', 'Grupo sanguíneo (opcional)', 6)}
        {campo('condicionDiscapacidad', 'Discapacidad o diagnóstico, si tiene', 12)}
        {campo('condicionesEspeciales', 'Otra información que el colegio deba saber (opcional)', 12, {
          multiline: true,
          minRows: 2,
        })}
      </Grid>
      <Box sx={{ display: 'flex', gap: 1.5, justifyContent: 'flex-end', mt: 3 }}>
        {alCancelar && <Button onClick={alCancelar}>Cancelar</Button>}
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          {solicitud ? 'Guardar cambios' : 'Guardar y seguir con los documentos'}
        </Button>
      </Box>
    </Box>
  );
}
