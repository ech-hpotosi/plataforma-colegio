import { Box } from '@mui/material';

export type Tono = 'positivo' | 'alerta' | 'negativo' | 'neutro' | 'informativo';

const COLOR_TONO: Record<Tono, string> = {
  positivo: '#2f6b3a',
  alerta: '#c98a12',
  negativo: '#b3261e',
  neutro: '#8a8f94',
  informativo: '#1b4a72',
};

/** Estado de un registro como texto con un punto de color, sin fondo, para que la tabla se lea limpia. */
export default function Estado({ tono, texto }: { tono: Tono; texto: string }) {
  return (
    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75, whiteSpace: 'nowrap' }}>
      <Box component="span" sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: COLOR_TONO[tono], flexShrink: 0 }} />
      {texto}
    </Box>
  );
}
