import { Box, Button, Typography } from '@mui/material';
import { Link } from 'react-router';

export default function SinPermiso() {
  return (
    <Box sx={{ mt: 4, textAlign: 'center' }}>
      <Typography variant="h5" gutterBottom>
        No tiene permiso para ver esta página
      </Typography>
      <Button component={Link} to="/" variant="contained">
        Volver al inicio
      </Button>
    </Box>
  );
}
