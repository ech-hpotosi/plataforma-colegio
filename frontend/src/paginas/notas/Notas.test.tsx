import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };
const rector = { id: 5, nombreUsuario: 'rector', nombreCompleto: 'Raul Rector', roles: ['RECTOR'] };

const configuracion = {
  anioLectivoId: 1,
  anio: 2027,
  notaMinima: 1,
  notaMaxima: 5,
  notaAprobatoria: 3,
  limiteAlto: 4,
  limiteSuperior: 4.6,
  topeRecuperacion: 3,
  pesoSaber: 40,
  pesoHacer: 40,
  pesoSer: 20,
  editable: true,
};

const periodos = [
  { id: 11, numero: 1, fechaInicio: '2000-01-01', fechaFin: '2099-12-31', porcentaje: 100, cerrado: false },
];

const cargas = [
  {
    cargaId: 7,
    grupoId: 3,
    anio: 2027,
    sede: 'Colegio El Encano',
    grupo: 'Sexto 01',
    asignatura: 'Ciencias',
    docente: 'Ana Docente',
    cualitativa: false,
    periodos,
  },
];

const planilla = {
  cargaId: 7,
  grupo: 'Sexto 01',
  asignatura: 'Ciencias',
  periodoId: 11,
  periodo: 1,
  editable: true,
  admiteRecuperacion: true,
  configuracion,
  actividades: [{ id: 21, dimension: 'SABER', nombre: 'Taller 1', fecha: null, porcentaje: null, porcentajeEfectivo: 100 }],
  estudiantes: [
    {
      matriculaId: 30,
      nombres: 'Valentina',
      apellidos: 'Jojoa',
      notas: { '21': 4 },
      saber: 4,
      hacer: null,
      ser: null,
      notaPeriodo: 4,
      recuperacion: null,
      observacionRecuperacion: null,
      notaDefinitiva: 4,
      desempeno: 'ALTO',
      completa: false,
    },
    {
      matriculaId: 31,
      nombres: 'Mateo',
      apellidos: 'Botina',
      notas: {},
      saber: null,
      hacer: null,
      ser: null,
      notaPeriodo: null,
      recuperacion: null,
      observacionRecuperacion: null,
      notaDefinitiva: null,
      desempeno: null,
      completa: false,
    },
  ],
};

describe('Notas', () => {
  afterEach(() => vi.restoreAllMocks());

  it('el docente escribe notas con coma y guarda solo las que cambió', async () => {
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/notas/cargas': { estado: 200, cuerpo: cargas },
      'GET /api/notas/cargas/7/periodos/11': { estado: 200, cuerpo: planilla },
      'PUT /api/notas/cargas/7/periodos/11': { estado: 200, cuerpo: planilla },
    });
    renderizarEn('/notas', <App />);

    const celda = await screen.findByLabelText('Taller 1 de Mateo Botina');
    expect(screen.getByLabelText('Taller 1 de Valentina Jojoa')).toHaveValue('4.0');
    expect(screen.getByText('4.0*')).toBeInTheDocument();

    await userEvent.type(celda, '3,5');
    expect(screen.getByText(/Tiene 1 cambio sin guardar/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Guardar notas' }));
    expect(await screen.findByText('Notas guardadas')).toBeInTheDocument();

    const envio = fetch.mock.calls.find(([, opciones]) => opciones?.method === 'PUT');
    expect(JSON.parse(String(envio?.[1]?.body))).toEqual({ notas: [{ actividadId: 21, matriculaId: 31, valor: 3.5 }] });
  });

  it('a quien queda en Bajo se le registra la recuperación del periodo', async () => {
    const enBajo = {
      ...planilla,
      estudiantes: [
        { ...planilla.estudiantes[0], notaPeriodo: 2.4, notaDefinitiva: 2.4, desempeno: 'BAJO', completa: true },
        planilla.estudiantes[1],
      ],
    };
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/notas/cargas': { estado: 200, cuerpo: cargas },
      'GET /api/notas/cargas/7/periodos/11': { estado: 200, cuerpo: enBajo },
      'PUT /api/notas/cargas/7/periodos/11/recuperaciones': { estado: 200, cuerpo: enBajo },
    });
    renderizarEn('/notas/planilla', <App />);

    await userEvent.type(await screen.findByLabelText('Recuperación de Valentina Jojoa'), '4,5');
    expect(screen.queryByLabelText('Recuperación de Mateo Botina')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Guardar notas' }));
    expect(await screen.findByText('Notas guardadas')).toBeInTheDocument();

    const envio = fetch.mock.calls.find(([, opciones]) => opciones?.method === 'PUT');
    expect(String(envio?.[0])).toContain('/recuperaciones');
    expect(JSON.parse(String(envio?.[1]?.body))).toEqual({
      recuperaciones: [{ matriculaId: 30, nota: 4.5, observacion: null }],
    });
  });

  it('la nueva actividad se crea con su porcentaje dentro de la dimensión', async () => {
    const conPorcentaje = {
      ...planilla,
      actividades: [{ ...planilla.actividades[0], porcentaje: 60, porcentajeEfectivo: 60 }],
    };
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/notas/cargas': { estado: 200, cuerpo: cargas },
      'GET /api/notas/cargas/7/periodos/11': { estado: 200, cuerpo: conPorcentaje },
      'POST /api/notas/cargas/7/periodos/11/actividades': { estado: 200, cuerpo: conPorcentaje },
    });
    renderizarEn('/notas/planilla', <App />);

    expect(await screen.findByText('60 % del Saber')).toBeInTheDocument();
    expect(screen.getByText('24 % del periodo')).toBeInTheDocument();
    expect(screen.getByText(/suman 60 %/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Nueva actividad' }));
    expect(screen.getByText(/ya tienen 60 %/)).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Nombre'), 'Evaluación');
    await userEvent.type(screen.getByLabelText(/Porcentaje dentro del Saber/), '40');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    const envio = fetch.mock.calls.find(([, opciones]) => opciones?.method === 'POST');
    expect(JSON.parse(String(envio?.[1]?.body))).toEqual({
      dimension: 'SABER',
      nombre: 'Evaluación',
      fecha: null,
      porcentaje: 40,
    });
  });

  it('el informe del periodo guarda el concepto de cada desempeño y el comportamiento', async () => {
    const informe = {
      cargaId: 7,
      grupo: 'Sexto 01',
      asignatura: 'Ciencias',
      periodoId: 11,
      periodo: 1,
      editable: true,
      configuracion,
      descriptores: { SUPERIOR: 'Explica con autonomía.' },
      estudiantes: [
        { matriculaId: 30, nombres: 'Valentina', apellidos: 'Jojoa', notaDefinitiva: 4.8, desempeno: 'SUPERIOR', completa: true, comportamiento: null, observacion: null },
        { matriculaId: 31, nombres: 'Mateo', apellidos: 'Botina', notaDefinitiva: 2.1, desempeno: 'BAJO', completa: true, comportamiento: 4, observacion: null },
      ],
    };
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/notas/cargas': { estado: 200, cuerpo: cargas },
      'GET /api/notas/cargas/7/periodos/11/informe': { estado: 200, cuerpo: informe },
      'PUT /api/notas/cargas/7/periodos/11/informe': { estado: 200, cuerpo: informe },
    });
    renderizarEn('/notas/informe', <App />);

    expect(await screen.findByText(/Hay estudiantes en Bajo y aún no tiene el concepto/)).toBeInTheDocument();
    expect(screen.getByLabelText('Comportamiento de Mateo Botina')).toHaveValue('4.0');
    await userEvent.type(screen.getByLabelText('Bajo (1 estudiante)'), 'Debe reforzar.');
    await userEvent.type(screen.getByLabelText('Comportamiento de Valentina Jojoa'), '4,5');
    await userEvent.type(screen.getByLabelText('Observación de Valentina Jojoa'), 'Participa.');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar informe' }));
    expect(await screen.findByText('Informe guardado')).toBeInTheDocument();

    const envio = fetch.mock.calls.find(([, opciones]) => opciones?.method === 'PUT');
    expect(JSON.parse(String(envio?.[1]?.body))).toEqual({
      descriptores: { BAJO: 'Debe reforzar.' },
      estudiantes: [{ matriculaId: 30, comportamiento: 4.5, observacion: 'Participa.' }],
    });
  }, 15000);

  it('una nota fuera de la escala no se envía', async () => {
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/notas/cargas': { estado: 200, cuerpo: cargas },
      'GET /api/notas/cargas/7/periodos/11': { estado: 200, cuerpo: planilla },
    });
    renderizarEn('/notas/planilla', <App />);

    await userEvent.type(await screen.findByLabelText('Taller 1 de Mateo Botina'), '7');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar notas' }));
    expect(await screen.findByText(/Hay notas que no son válidas/)).toBeInTheDocument();
    expect(fetch.mock.calls.some(([, opciones]) => opciones?.method === 'PUT')).toBe(false);
  });

  it('el rector ve el consolidado del grupo con las asignaturas en Bajo', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: rector },
      'GET /api/asistencia/grupos': {
        estado: 200,
        cuerpo: [{ grupoId: 3, anio: 2027, sede: 'Colegio El Encano', grupo: 'Sexto 01' }],
      },
      'GET /api/notas/grupos/3/consolidado': {
        estado: 200,
        cuerpo: {
          grupoId: 3,
          grupo: 'Sexto 01',
          periodoId: null,
          periodos,
          configuracion,
          asignaturas: [{ cargaId: 7, nombre: 'Ciencias', docente: 'Ana Docente' }],
          estudiantes: [
            {
              matriculaId: 31,
              nombres: 'Mateo',
              apellidos: 'Botina',
              notas: [{ cargaId: 7, nota: 2.4, desempeno: 'BAJO', completa: true }],
              asignaturasEnBajo: 1,
            },
          ],
        },
      },
    });
    renderizarEn('/notas', <App />);

    expect(await screen.findByText('2.4')).toBeInTheDocument();
    expect(screen.getByText(/1 estudiante tiene alguna asignatura en Bajo/)).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Planilla de notas' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Escala de valoración' })).not.toBeInTheDocument();
  });

  it('desde el consolidado de un periodo se piden los boletines en PDF', async () => {
    const consolidado = {
      grupoId: 3,
      grupo: 'Sexto 01',
      periodoId: 11,
      periodos,
      configuracion,
      asignaturas: [{ cargaId: 7, nombre: 'Ciencias', docente: 'Ana Docente' }],
      estudiantes: [
        { matriculaId: 31, nombres: 'Mateo', apellidos: 'Botina', notas: [{ cargaId: 7, nota: 3.5, desempeno: 'BASICO', completa: true }], asignaturasEnBajo: 0 },
      ],
    };
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: rector },
      'GET /api/asistencia/grupos': {
        estado: 200,
        cuerpo: [{ grupoId: 3, anio: 2027, sede: 'Colegio El Encano', grupo: 'Sexto 01' }],
      },
      'GET /api/notas/grupos/3/consolidado': { estado: 200, cuerpo: consolidado },
      'GET /api/notas/grupos/3/periodos/11/boletines': {
        estado: 409,
        cuerpo: { mensaje: 'Transición se evalúa de forma cualitativa' },
      },
    });
    renderizarEn('/notas/consolidado', <App />);

    await screen.findByText('3.5');
    expect(screen.queryByRole('button', { name: 'Descargar boletines (PDF)' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('combobox', { name: 'Periodo' }));
    await userEvent.click(screen.getByRole('option', { name: /^Periodo 1/ }));
    await userEvent.click(await screen.findByRole('button', { name: 'Boletín de Mateo Botina' }));
    expect(await screen.findByText('Transición se evalúa de forma cualitativa')).toBeInTheDocument();
    const pedido = fetch.mock.calls.find(([url]) => String(url).includes('/boletines'));
    expect(String(pedido?.[0])).toBe('/api/notas/grupos/3/periodos/11/boletines?matriculaId=31');
    expect(screen.getByRole('button', { name: 'Descargar boletines (PDF)' })).toBeEnabled();
  });
});
