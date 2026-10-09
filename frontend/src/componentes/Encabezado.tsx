import type { ReactNode } from 'react';
import { Box, Typography } from '@mui/material';

interface Props {
  titulo: ReactNode;
  descripcion?: ReactNode;
  /** Botones o filtros que van a la derecha del titulo. */
  acciones?: ReactNode;
}

/** Titulo de cada pantalla, con una descripcion corta opcional y las acciones principales. */
export default function Encabezado({ titulo, descripcion, acciones }: Props) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 2, mb: 3, flexWrap: 'wrap' }}>
      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        <Typography variant="h5" component="h1">
          {titulo}
        </Typography>
        {descripcion && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            {descripcion}
          </Typography>
        )}
      </Box>
      {acciones && <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', flexWrap: 'wrap' }}>{acciones}</Box>}
    </Box>
  );
}
