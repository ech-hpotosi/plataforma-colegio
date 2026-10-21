import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const acudiente = { id: 9, nombreUsuario: '27123456', nombreCompleto: 'Rosa Botina', roles: ['ACUDIENTE'] };

const estudiantes = [
  {
    id: 10,
    nombres: 'Valentina',
    apellidos: 'Jojoa Botina',
    parentesco: 'MADRE',
    matriculas: [{ id: 30, anio: 2027, grupo: 'Sexto 01', sede: 'Sede Central' }],
  },
  {
    id: 11,
    nombres: 'Samuel',
    apellidos: 'Jojoa Botina',
    parentesco: 'MADRE',
    matriculas: [],
  },
];

const periodos = [
  { id: 1, numero: 1, fechaInicio: '2027-01-25', fechaFin: '2027-04-30', porcentaje: 50, cerrado: true },
  { id: 2, numero: 2, fechaInicio: '2027-05-03', fechaFin: '2027-11-26', porcentaje: 50, cerrado: false },
];

const resumen = {
  estudianteId: 10,
  estudiante: 'Valentina Jojoa Botina',
  matriculaId: 30,
  anio: 2027,
  grupo: 'Sexto 01',
  sede: 'Sede Central',
  director: 'Ana Docente',
  escala: null,
  periodos,
  asignaturas: [
    {
      nombre: 'Matemáticas',
      docente: 'Luis Pérez',
      periodos: [
        { periodoId: 1, nota: 2.5, desempeno: 'BAJO', completa: true },
        { periodoId: 2, nota: 4.2, desempeno: 'ALTO', completa: false },
      ],
      definitiva: 3.4,
      desempeno: 'BASICO',
      completa: false,
      faltasJustificadas: 0,
      faltasSinJustificar: 2,
      retardos: 0,
    },
  ],
  novedades: [
    {
      detalleId: 1,
      fecha: '2027-03-04',
      asignatura: 'Matemáticas',
      horas: 2,
      estado: 'FALTA',
      observacion: null,
      justificacion: null,
      fechaLimite: '2027-03-09',
    },
  ],
  boletinDisponible: true,
};

describe('Consulta de la familia', () => {
  afterEach(() => vi.restoreAllMocks());

  it('el acudiente ve notas por periodo, faltas y el boletín de los periodos cerrados', async () => {
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: acudiente },
      'GET /api/familia/estudiantes': { estado: 200, cuerpo: estudiantes },
      'GET /api/familia/estudiantes/10/resumen': { estado: 200, cuerpo: resumen },
    });
    const usuario = userEvent.setup();
    renderizarEn('/', <App />);

    await usuario.click(await screen.findByRole('link', { name: 'Ver notas y asistencia' }));
    expect(await screen.findByRole('heading', { name: 'Valentina Jojoa Botina' })).toBeInTheDocument();
    expect(screen.getByText('Sexto 01 · Sede Central · Año 2027')).toBeInTheDocument();
    expect(screen.getByText('2.5')).toBeInTheDocument();
    expect(screen.getByText('4.2')).toBeInTheDocument();
    expect(screen.getByText('parcial')).toBeInTheDocument();
    expect(screen.getByText('Faltas: 2 h sin justificar')).toBeInTheDocument();
    expect(screen.getByText('Sin justificar')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Boletín periodo 1' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Boletín periodo 2' })).not.toBeInTheDocument();
    expect(String(fetch.mock.calls.at(-1)?.[0])).toContain('matriculaId=30');

    await usuario.click(screen.getByRole('button', { name: 'Samuel Jojoa Botina' }));
    expect(await screen.findByText(/Samuel no tiene una matrícula activa/)).toBeInTheDocument();
  });

  it('sin estudiantes explica cómo se activa la consulta', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: acudiente },
      'GET /api/familia/estudiantes': { estado: 200, cuerpo: [] },
    });
    renderizarEn('/familia', <App />);

    expect(await screen.findByText('Todavía no tiene estudiantes para consultar')).toBeInTheDocument();
  });
});
