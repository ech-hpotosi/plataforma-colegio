import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import { renderizarEn, simularApi } from '../pruebas/utilidades';

const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };
const acudiente = { id: 9, nombreUsuario: 'acudiente', nombreCompleto: 'Rosa Acudiente', roles: ['ACUDIENTE'] };

const pendientes = {
  porcentajeMaximo: 15,
  clasesHoy: [
    { cargaId: 7, sede: 'Colegio El Encano', grupo: 'Sexto 01', asignatura: 'Ética', registrada: false },
    { cargaId: 8, sede: 'Colegio El Encano', grupo: 'Sexto 01', asignatura: 'Ciencias', registrada: true },
  ],
  faltasPorJustificar: [
    { matriculaId: 31, grupoId: 3, estudiante: 'Mateo Botina', grupo: 'Sexto 01', fecha: '2027-03-01', horas: 2, fechaLimite: '2027-03-04' },
  ],
  estudiantesEnRiesgo: [
    { matriculaId: 30, grupoId: 3, estudiante: 'Valentina Jojoa', grupo: 'Sexto 01', asignatura: 'Ética', porcentaje: 17.5, superaLimite: true },
  ],
};

describe('Inicio', () => {
  afterEach(() => vi.restoreAllMocks());

  it('muestra los pendientes de asistencia con enlaces a la clase y al grupo', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/asistencia/pendientes': { estado: 200, cuerpo: pendientes },
    });
    renderizarEn('/', <App />);

    expect(await screen.findByText('Le falta registrar 1 de 2 clases. Registre solo las que dictó hoy.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Tomar' })).toHaveAttribute('href', '/asistencia/tomar?carga=7');
    expect(screen.getByRole('link', { name: 'Corregir' })).toHaveAttribute('href', '/asistencia/tomar?carga=8');
    expect(screen.getByText('Mateo Botina')).toBeInTheDocument();
    expect(screen.getByText('Supera el límite')).toBeInTheDocument();
    expect(screen.getByText('Sexto 01. Ética: 17.5 %')).toBeInTheDocument();
  });

  it('sin pendientes lo dice en lugar de repetir el menú', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/asistencia/pendientes': {
        estado: 200,
        cuerpo: { porcentajeMaximo: 15, clasesHoy: [], faltasPorJustificar: [], estudiantesEnRiesgo: [] },
      },
    });
    renderizarEn('/', <App />);
    expect(await screen.findByText('No tiene pendientes por ahora.')).toBeInTheDocument();
  });

  it('un perfil sin asistencia no consulta pendientes', async () => {
    const fetch = simularApi({ 'GET /api/yo': { estado: 200, cuerpo: acudiente } });
    renderizarEn('/', <App />);
    expect(await screen.findByText('Por ahora no hay información disponible para su perfil.')).toBeInTheDocument();
    expect(fetch.mock.calls.some(([url]) => String(url).includes('pendientes'))).toBe(false);
  });
});
