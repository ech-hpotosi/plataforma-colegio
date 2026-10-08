import { Chip, Stack, Typography } from '@mui/material';
import { useSesion } from '../sesion/useSesion';
import { NOMBRE_ROL } from '../tipos';

export default function Inicio() {
  const { usuario } = useSesion();

  return (
    <>
      <Typography variant="h5" component="h1" gutterBottom>
        Bienvenido, {usuario?.nombreCompleto}
      </Typography>
      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
        {usuario?.roles.map((rol) => <Chip key={rol} label={NOMBRE_ROL[rol]} color="primary" variant="outlined" />)}
      </Stack>
    </>
  );
}
