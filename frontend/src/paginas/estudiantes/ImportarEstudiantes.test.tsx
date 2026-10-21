import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const secretaria = { id: 4, nombreUsuario: 'secretaria', nombreCompleto: 'Sara Secretaria', roles: ['SECRETARIA'] };
const coordinador = { id: 6, nombreUsuario: 'coord', nombreCompleto: 'Carlos Coord', roles: ['COORDINADOR_ACADEMICO'] };
const anio2027 = { id: 1, anio: 2027, fechaInicio: '2027-01-25', fechaFin: '2027-11-26', estado: 'MATRICULA', periodos: [] };

const filas = [
  {
    fila: 2,
    documento: '1085123456',
    estudiante: 'Luna Sofía Jojoa Chasoy',
    grado: 'Sexto',
    grupo: '6-01',
    accion: 'CREAR',
    errores: [],
    avisos: [],
  },
  {
    fila: 3,
    documento: '1085000002',
    estudiante: null,
    grado: 'Doceavo',
    grupo: null,
    accion: 'ERROR',
    errores: ['No existe el grado Doceavo'],
    avisos: [],
  },
];
const revision = { anio: 2027, total: 2, creados: 1, actualizados: 0, conErrores: 1, aplicado: false, filas };

describe('Importar estudiantes', () => {
  afterEach(() => vi.restoreAllMocks());

  it('la secretaria revisa el archivo, ve los errores y confirma la carga', async () => {
    let llamadas = 0;
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: secretaria },
      'GET /api/anios': { estado: 200, cuerpo: [anio2027] },
      'GET /api/estudiantes': { estado: 200, cuerpo: { contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 } },
      'POST /api/importacion-estudiantes': () => {
        llamadas++;
        return { estado: 200, cuerpo: llamadas === 1 ? revision : { ...revision, aplicado: true } };
      },
    });
    const usuario = userEvent.setup();
    renderizarEn('/estudiantes', <App />);

    await usuario.click(await screen.findByRole('link', { name: 'Importar desde Excel' }));
    expect(await screen.findByRole('heading', { name: 'Importar estudiantes' })).toBeInTheDocument();
    const revisar = screen.getByRole('button', { name: 'Revisar archivo' });
    expect(revisar).toBeDisabled();

    const archivo = new File(['contenido'], 'estudiantes.xlsx');
    await usuario.upload(screen.getByLabelText('Archivo de estudiantes'), archivo);
    expect(screen.getByText('estudiantes.xlsx')).toBeInTheDocument();
    await screen.findByRole('combobox', { name: 'Año lectivo' });
    await usuario.click(screen.getByRole('button', { name: 'Revisar archivo' }));

    expect(await screen.findByText('No existe el grado Doceavo')).toBeInTheDocument();
    expect(screen.getByText(/Todavía no se ha guardado nada/)).toBeInTheDocument();
    expect(screen.getByText('No se carga')).toBeInTheDocument();
    const [url] = fetch.mock.calls.find(([u]) => String(u).startsWith('/api/importacion-estudiantes'))!;
    expect(String(url)).toContain('anioId=1&confirmar=false');

    await usuario.click(screen.getByLabelText('Ver solo filas con errores o avisos'));
    expect(screen.queryByText('Luna Sofía Jojoa Chasoy')).not.toBeInTheDocument();

    await usuario.click(screen.getByRole('button', { name: 'Cargar 1 estudiantes en 2027' }));
    expect(await screen.findByText(/Carga terminada en el año 2027/)).toBeInTheDocument();
    expect(screen.getByText('No se cargó')).toBeInTheDocument();
    expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('confirmar=true');
  });

  it('el coordinador no ve la opción de importar', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: coordinador },
      'GET /api/estudiantes': { estado: 200, cuerpo: { contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 } },
    });
    renderizarEn('/estudiantes', <App />);

    expect(await screen.findByRole('heading', { name: 'Estudiantes' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Importar desde Excel' })).not.toBeInTheDocument();
  });
});
