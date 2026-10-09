import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const coordinador = { id: 4, nombreUsuario: 'coord', nombreCompleto: 'Carlos Coordinador', roles: ['COORDINADOR_ACADEMICO'] };
const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };

const base = {
  anio: 2027,
  sede: 'Colegio El Encano',
  grupo: 'Sexto 01',
  docente: 'Ana Docente',
  periodoId: 11,
  periodoNumero: 1,
  inicioPeriodo: '2027-01-25',
  cierrePeriodo: '2027-04-30',
  periodoCerrado: false,
  estudiantes: 30,
  cualitativa: false,
  actividades: 2,
  dimensionesSinActividad: [],
  notasEsperadas: 60,
  diasConAsistencia: 20,
  ultimaAsistencia: '2027-04-08',
  diasSinAsistencia: 2,
};
const avance = {
  hoy: '2027-04-10',
  clases: [
    { ...base, cargaId: 1, asignatura: 'Ciencias', notasRegistradas: 60, porcentajeNotas: 100 },
    { ...base, cargaId: 2, asignatura: 'Ética', notasRegistradas: 45, porcentajeNotas: 75 },
    {
      ...base,
      cargaId: 3,
      sede: 'Escuela Santa Lucía',
      docente: 'Luis Docente',
      asignatura: 'Inglés',
      actividades: 0,
      dimensionesSinActividad: ['SABER', 'HACER', 'SER'],
      notasRegistradas: 0,
      notasEsperadas: 0,
      porcentajeNotas: 0,
      diasConAsistencia: 0,
      ultimaAsistencia: null,
      diasSinAsistencia: 75,
    },
  ],
};

describe('Seguimiento', () => {
  afterEach(() => vi.restoreAllMocks());

  it('coordinación ve primero las clases más atrasadas y puede filtrar por sede', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: coordinador },
      'GET /api/seguimiento/avance': { estado: 200, cuerpo: avance },
    });
    renderizarEn('/seguimiento', <App />);

    expect(await screen.findByText(/Notas completas en 1 de 3 clases\. Asistencia atrasada en 1 clase/)).toBeInTheDocument();
    const filas = screen.getAllByRole('row').slice(1);
    // La clase completa y al dia no aparece con el filtro de pendientes
    expect(filas).toHaveLength(2);
    expect(within(filas[0]).getByText('Luis Docente')).toBeInTheDocument();
    expect(within(filas[0]).getByText('Sin actividades todavía. Falta crear actividades de Saber, Hacer y Ser.')).toBeInTheDocument();
    expect(within(filas[0]).getByText('Sin registros en 75 días')).toBeInTheDocument();
    expect(within(filas[1]).getByText('45 de 60 notas.')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('combobox', { name: 'Sede' }));
    await userEvent.click(screen.getByRole('option', { name: 'Colegio El Encano' }));
    expect(screen.getAllByRole('row')).toHaveLength(2);

    await userEvent.click(screen.getByRole('combobox', { name: 'Mostrar' }));
    await userEvent.click(screen.getByRole('option', { name: 'Todas las clases' }));
    expect(screen.getAllByRole('row')).toHaveLength(3);
  });

  it('el docente no tiene acceso a la vista de seguimiento', async () => {
    simularApi({ 'GET /api/yo': { estado: 200, cuerpo: docente } });
    renderizarEn('/seguimiento', <App />);
    expect(await screen.findByText(/no tiene permiso/i)).toBeInTheDocument();
  });
});
