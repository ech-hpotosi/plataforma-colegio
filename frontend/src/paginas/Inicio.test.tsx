import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import { renderizarEn, simularApi } from '../pruebas/utilidades';

const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };
const coordinador = { id: 4, nombreUsuario: 'coord', nombreCompleto: 'Carlos Coordinador', roles: ['COORDINADOR_ACADEMICO'] };
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

const sinPendientes = { porcentajeMaximo: 15, clasesHoy: [], faltasPorJustificar: [], estudiantesEnRiesgo: [] };

const clase = {
  cargaId: 7,
  anio: 2027,
  sede: 'Colegio El Encano',
  grupo: 'Sexto 01',
  asignatura: 'Ética',
  docente: 'Ana Docente',
  periodoId: 11,
  periodoNumero: 1,
  inicioPeriodo: '2027-01-25',
  cierrePeriodo: '2027-04-30',
  periodoCerrado: false,
  estudiantes: 30,
  cualitativa: false,
  actividades: 2,
  dimensionesSinActividad: ['SER'],
  notasRegistradas: 45,
  notasEsperadas: 60,
  porcentajeNotas: 75,
  diasConAsistencia: 20,
  ultimaAsistencia: '2027-04-01',
  diasSinAsistencia: 9,
};
const completa = { ...clase, cargaId: 8, asignatura: 'Ciencias', dimensionesSinActividad: [], notasRegistradas: 60, porcentajeNotas: 100, diasSinAsistencia: 1 };
const avance = { hoy: '2027-04-10', clases: [clase, completa] };
const sinAvance = { hoy: '2027-04-10', clases: [] };

describe('Inicio', () => {
  afterEach(() => vi.restoreAllMocks());

  it('muestra los pendientes de asistencia con enlaces a la clase y al grupo', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/asistencia/pendientes': { estado: 200, cuerpo: pendientes },
      'GET /api/seguimiento/avance': { estado: 200, cuerpo: sinAvance },
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
      'GET /api/asistencia/pendientes': { estado: 200, cuerpo: sinPendientes },
      'GET /api/seguimiento/avance': { estado: 200, cuerpo: { hoy: '2027-04-10', clases: [completa] } },
    });
    renderizarEn('/', <App />);
    expect(await screen.findByText('No tiene pendientes por ahora.')).toBeInTheDocument();
  });

  it('el docente ve el avance de sus clases con pendientes', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/asistencia/pendientes': { estado: 200, cuerpo: sinPendientes },
      'GET /api/seguimiento/avance': { estado: 200, cuerpo: avance },
    });
    renderizarEn('/', <App />);

    expect(await screen.findByText(/Periodo 1: cierra el .*, en 20 días\./)).toBeInTheDocument();
    expect(screen.getByText('45 de 60 notas. Falta crear actividades de Ser. Asistencia: última hace 9 días.')).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Notas de Sexto 01 - Ética' })).toHaveAttribute('aria-valuenow', '75');
    expect(screen.getByRole('link', { name: 'Planilla' })).toHaveAttribute('href', '/notas/planilla?carga=7');
    // La clase completa y al dia no aparece
    expect(screen.queryByText(/Ciencias/)).not.toBeInTheDocument();
  });

  it('coordinación ve el resumen y el enlace al seguimiento', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: coordinador },
      'GET /api/asistencia/pendientes': { estado: 200, cuerpo: sinPendientes },
      'GET /api/seguimiento/avance': { estado: 200, cuerpo: avance },
    });
    renderizarEn('/', <App />);

    expect(await screen.findByText('Notas completas en 1 de 2 clases')).toBeInTheDocument();
    expect(screen.getByText('Asistencia atrasada en 1 clase (más de 7 días sin registrar).')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver seguimiento' })).toHaveAttribute('href', '/seguimiento');
  });

  it('un perfil sin asistencia no consulta pendientes', async () => {
    const fetch = simularApi({ 'GET /api/yo': { estado: 200, cuerpo: acudiente } });
    renderizarEn('/', <App />);
    expect(await screen.findByText('Por ahora no hay información disponible para su perfil.')).toBeInTheDocument();
    expect(fetch.mock.calls.some(([url]) => String(url).includes('pendientes'))).toBe(false);
  });
});
