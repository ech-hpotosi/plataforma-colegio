import { alpha, createTheme } from '@mui/material';
import '@fontsource-variable/source-sans-3';
import '@fontsource-variable/source-serif-4';

// Colores tomados del escudo y la bandera de la IEM El Encano:
// azul de la Laguna de La Cocha, verde de las parcelas y el ocre del sol.
export const COLORES = {
  laguna: '#1b4a72',
  lagunaOscuro: '#12324f',
  parcela: '#3f6e3a',
  sol: '#c98a12',
  papel: '#f4f2ec',
  linea: '#e2ddd2',
  encabezado: '#f8f6f1',
  tinta: '#1e2a33',
  tintaSuave: '#5b6670',
};

const FUENTE_TEXTO = "'Source Sans 3 Variable', 'Segoe UI', sans-serif";
const FUENTE_TITULOS = "'Source Serif 4 Variable', Georgia, serif";

export const tema = createTheme({
  palette: {
    primary: { main: COLORES.laguna, dark: COLORES.lagunaOscuro },
    secondary: { main: COLORES.parcela },
    warning: { main: '#a8650b' },
    success: { main: '#2f6b3a' },
    error: { main: '#b3261e' },
    info: { main: COLORES.laguna },
    background: { default: COLORES.papel, paper: '#ffffff' },
    text: { primary: COLORES.tinta, secondary: COLORES.tintaSuave },
    divider: COLORES.linea,
  },
  shape: { borderRadius: 6 },
  typography: {
    fontFamily: FUENTE_TEXTO,
    fontSize: 15,
    h4: { fontFamily: FUENTE_TITULOS, fontWeight: 600, letterSpacing: '-0.01em' },
    h5: { fontFamily: FUENTE_TITULOS, fontWeight: 600, letterSpacing: '-0.01em' },
    h6: { fontFamily: FUENTE_TITULOS, fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600, letterSpacing: 0 },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { padding: '6px 16px' },
        sizeSmall: { padding: '2px 8px' },
        // Secundario: borde neutro y texto oscuro, para que no compita con la accion principal
        outlined: { borderColor: '#cfc8b8', color: COLORES.tinta, backgroundColor: '#fff' },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
        elevation1: { boxShadow: 'none', border: `1px solid ${COLORES.linea}` },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 10, boxShadow: '0 12px 40px rgba(18, 50, 79, 0.18)' },
      },
    },
    MuiDialogTitle: {
      styleOverrides: { root: { fontFamily: FUENTE_TITULOS, fontWeight: 600, fontSize: '1.25rem' } },
    },
    MuiMenu: {
      styleOverrides: {
        paper: { border: `1px solid ${COLORES.linea}`, boxShadow: '0 6px 20px rgba(18, 50, 79, 0.10)' },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: COLORES.linea },
        head: {
          fontWeight: 600,
          fontSize: '0.8125rem',
          color: COLORES.tintaSuave,
          backgroundColor: COLORES.encabezado,
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: { '&.MuiTableRow-hover:hover': { backgroundColor: alpha(COLORES.laguna, 0.04) } },
      },
    },
    MuiTabs: {
      styleOverrides: { indicator: { backgroundColor: COLORES.sol, height: 3 } },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          fontSize: '0.95rem',
          minHeight: 44,
          color: COLORES.tintaSuave,
          '&.Mui-selected': { color: COLORES.tinta },
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: { root: { backgroundColor: '#fff' } },
    },
    MuiChip: {
      defaultProps: { variant: 'outlined', size: 'small' },
      styleOverrides: { root: { borderRadius: 4, fontWeight: 600 } },
    },
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 6, border: '1px solid', borderLeftWidth: 4 },
        standardSuccess: { borderColor: alpha('#2f6b3a', 0.35) },
        standardError: { borderColor: alpha('#b3261e', 0.35) },
        standardWarning: { borderColor: alpha('#a8650b', 0.35) },
        standardInfo: { borderColor: alpha(COLORES.laguna, 0.3) },
      },
    },
    MuiToggleButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          color: COLORES.tintaSuave,
          backgroundColor: '#fff',
          '&.Mui-selected': { color: '#fff', backgroundColor: COLORES.laguna },
          '&.Mui-selected:hover': { backgroundColor: COLORES.lagunaOscuro },
          '&.Mui-selected.MuiToggleButton-warning': { backgroundColor: '#a8650b' },
        },
      },
    },
  },
});
