import { createTheme } from '@mui/material';

// Colores de la bandera de la IEM El Encano: azul (riqueza hidrica, Laguna de La Cocha),
// blanco (paz) y verde (riqueza natural). Tonos provisionales hasta tener el escudo original
// para tomar los valores exactos.
export const tema = createTheme({
  palette: {
    primary: { main: '#1d4f91' },
    secondary: { main: '#2e7d32' },
    background: { default: '#f5f7fa' },
  },
});
