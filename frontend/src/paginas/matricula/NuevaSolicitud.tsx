import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, CircularProgress, Paper } from '@mui/material';
import { useNavigate } from 'react-router';
import { consultarOferta } from '../../api/matricula';
import Encabezado from '../../componentes/Encabezado';
import { mensajeDeError } from '../academico/mensajes';
import FormularioSolicitud from './FormularioSolicitud';

export default function NuevaSolicitud() {
  const navegar = useNavigate();
  const queryClient = useQueryClient();
  const oferta = useQuery({ queryKey: ['oferta-matricula'], queryFn: consultarOferta });

  if (oferta.isPending) return <CircularProgress />;
  if (oferta.isError) return <Alert severity="error">{mensajeDeError(oferta.error)}</Alert>;
  if (oferta.data.length === 0) {
    return <Alert severity="info">En este momento no hay matrícula en línea abierta.</Alert>;
  }

  return (
    <>
      <Encabezado
        titulo="Nueva solicitud de matrícula"
        descripcion="Paso 1 de 3: datos del estudiante. Después sube los documentos y la envía."
      />
      <Paper sx={{ p: { xs: 2, sm: 3 } }}>
        <FormularioSolicitud
          oferta={oferta.data[0]}
          solicitud={null}
          alCancelar={() => navegar('/matricula-en-linea')}
          alGuardar={(s) => {
            queryClient.invalidateQueries({ queryKey: ['mis-solicitudes'] });
            queryClient.setQueryData(['solicitud', s.id], s);
            navegar(`/matricula-en-linea/${s.id}`, { replace: true });
          }}
        />
      </Paper>
    </>
  );
}
