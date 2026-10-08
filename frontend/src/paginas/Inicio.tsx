import { useQuery } from '@tanstack/react-query';
import { Alert, Box, CircularProgress, Container, Typography } from '@mui/material';
import { consultarSalud } from '../api/salud';

export default function Inicio() {
  const salud = useQuery({ queryKey: ['salud'], queryFn: consultarSalud });

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, textAlign: 'center' }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Institucion Educativa Municipal El Encano
        </Typography>
        <Typography variant="subtitle1" color="text.secondary" gutterBottom>
          Plataforma escolar
        </Typography>
        <Box sx={{ mt: 4 }}>
          {salud.isPending && <CircularProgress />}
          {salud.isError && <Alert severity="error">No hay conexion con el servidor</Alert>}
          {salud.isSuccess && <Alert severity="success">Servidor en linea</Alert>}
        </Box>
      </Box>
    </Container>
  );
}
