import { Box, LinearProgress, Typography } from '@mui/material';
import { COLORES } from '../tema';

/** Barra delgada con el porcentaje al lado; verde cuando llega a 100. */
export default function BarraAvance({ porcentaje, etiqueta }: { porcentaje: number; etiqueta: string }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 140 }}>
      <LinearProgress
        variant="determinate"
        value={porcentaje}
        aria-label={etiqueta}
        sx={{
          flexGrow: 1,
          height: 6,
          borderRadius: 3,
          bgcolor: COLORES.linea,
          '& .MuiLinearProgress-bar': { borderRadius: 3, bgcolor: porcentaje === 100 ? COLORES.parcela : COLORES.laguna },
        }}
      />
      <Typography variant="body2" sx={{ width: 40, textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
        {porcentaje} %
      </Typography>
    </Box>
  );
}
