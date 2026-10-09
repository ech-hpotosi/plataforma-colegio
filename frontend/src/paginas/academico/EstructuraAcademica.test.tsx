import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const coordinador = {
  id: 3,
  nombreUsuario: 'coordinador',
  nombreCompleto: 'Carlos Coordinador',
  roles: ['COORDINADOR_ACADEMICO'],
};
const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };

const anio2027 = {
  id: 1,
  anio: 2027,
  fechaInicio: '2027-01-25',
  fechaFin: '2027-11-26',
  estado: 'PLANEACION',
  periodos: [
    { id: 1, numero: 1, fechaInicio: '2027-01-25', fechaFin: '2027-04-30', porcentaje: 30, cerrado: false },
    { id: 2, numero: 2, fechaInicio: '2027-05-03', fechaFin: '2027-08-13', porcentaje: 30, cerrado: false },
    { id: 3, numero: 3, fechaInicio: '2027-08-17', fechaFin: '2027-11-26', porcentaje: 40, cerrado: false },
  ],
};

describe('Estructura académica', () => {
  afterEach(() => vi.restoreAllMocks());

  it('el coordinador ve el menu y la lista de sedes', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: coordinador },
      'GET /api/sedes': {
        estado: 200,
        cuerpo: [{ id: 1, codigoDane: '152001000001', nombre: 'Sede Central', direccion: null, principal: true }],
      },
    });
    renderizarEn('/academico', <App />);

    expect(await screen.findByText('Sede Central')).toBeInTheDocument();
    expect(screen.getByText('Principal')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Estructura académica' })).toBeInTheDocument();
  });

  it('un docente no puede entrar a la estructura académica', async () => {
    simularApi({ 'GET /api/yo': { estado: 200, cuerpo: docente } });
    renderizarEn('/academico/sedes', <App />);

    expect(await screen.findByText('No tiene permiso para ver esta página')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Estructura académica' })).not.toBeInTheDocument();
  });

  it('muestra los periodos del año y el mensaje del servidor al avanzar de estado', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: coordinador },
      'GET /api/anios': { estado: 200, cuerpo: [anio2027] },
      'PUT /api/anios/1/estado': {
        estado: 409,
        cuerpo: { mensaje: 'Ya hay otro año lectivo en curso. Ciérrelo primero', errores: {} },
      },
    });
    renderizarEn('/academico/anios', <App />);

    const fila = (await screen.findByText('2027')).closest('tr')!;
    expect(within(fila).getByText('P3: 17/08/2027 a 26/11/2027 (40%)')).toBeInTheDocument();

    await userEvent.click(within(fila).getByRole('button', { name: 'Pasar a Matrícula' }));
    expect(await screen.findByText('Ya hay otro año lectivo en curso. Ciérrelo primero')).toBeInTheDocument();
  });

  it('el formulario de año nuevo trae tres periodos y suma los porcentajes', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: coordinador },
      'GET /api/anios': { estado: 200, cuerpo: [] },
    });
    renderizarEn('/academico/anios', <App />);

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo año lectivo' }));
    expect(screen.getByText('Periodo 3')).toBeInTheDocument();
    expect(screen.getByText('Suma de porcentajes: 100%')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Quitar' }));
    expect(screen.getByText('Suma de porcentajes: 60%')).toBeInTheDocument();
  });
});
