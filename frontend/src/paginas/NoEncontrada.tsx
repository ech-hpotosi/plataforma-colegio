import { Box, Button, Container, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export default function NoEncontrada() {
  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, textAlign: 'center' }}>
        <Typography variant="h5" gutterBottom>
          Pagina no encontrada
        </Typography>
        <Button component={Link} to="/" variant="contained">
          Volver al inicio
        </Button>
      </Box>
    </Container>
  );
}
