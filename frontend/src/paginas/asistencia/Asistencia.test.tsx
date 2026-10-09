import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };
const rector = { id: 5, nombreUsuario: 'rector', nombreCompleto: 'Raul Rector', roles: ['RECTOR'] };

const cargas = [
  { cargaId: 7, grupoId: 3, anio: 2027, sede: 'Colegio El Encano', grupo: 'Sexto 01', asignatura: 'Ética', docente: 'Ana Docente' },
];

const clase = {
  cargaId: 7,
  grupo: 'Sexto 01',
  asignatura: 'Ética',
  fecha: '2027-03-01',
  periodo: 1,
  horas: 1,
  registrada: false,
  estudiantes: [
    { matriculaId: 30, nombres: 'Valentina', apellidos: 'Jojoa', estado: 'ASISTIO', observacion: null, justificacion: null },
    { matriculaId: 31, nombres: 'Mateo', apellidos: 'Botina', estado: 'ASISTIO', observacion: null, justificacion: null },
  ],
};

const resumen = {
  grupoId: 3,
  grupo: 'Sexto 01',
  semanasLectivas: 40,
  porcentajeMaximo: 15,
  asignaturas: [{ asignaturaId: 9, nombre: 'Ética', horasAnuales: 40 }],
  estudiantes: [
    {
      matriculaId: 30,
      nombres: 'Valentina',
      apellidos: 'Jojoa',
      superaLimite: true,
      asignaturas: [
        { asignaturaId: 9, horasSinJustificar: 7, horasJustificadas: 0, horasRetardo: 0, porcentaje: 17.5, superaLimite: true },
      ],
    },
    { matriculaId: 31, nombres: 'Mateo', apellidos: 'Botina', superaLimite: false, asignaturas: [] },
  ],
};

describe('Asistencia', () => {
  afterEach(() => vi.restoreAllMocks());

  it('el docente marca una falta y guarda la asistencia de su clase', async () => {
    const fetch = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: docente },
      'GET /api/asistencia/cargas': { estado: 200, cuerpo: cargas },
      'GET /api/asistencia/cargas/7': { estado: 200, cuerpo: clase },
      'PUT /api/asistencia/cargas/7': { estado: 200, cuerpo: { ...clase, registrada: true } },
    });
    renderizarEn('/asistencia', <App />);

    const fila = (await screen.findByText(/Jojoa Valentina/)).closest('div.MuiPaper-root') as HTMLElement;
    await userEvent.click(within(fila).getByRole('button', { name: 'Falta' }));
    await userEvent.type(within(fila).getByLabelText('Observación'), 'No llegó');
    expect(screen.getByText(/Faltas: 1\./)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Guardar asistencia' }));
    expect(await screen.findByText('Asistencia guardada')).toBeInTheDocument();

    const envio = fetch.mock.calls.find(([, opciones]) => opciones?.method === 'PUT');
    const cuerpo = JSON.parse(String(envio?.[1]?.body));
    expect(cuerpo.horas).toBe(1);
    expect(cuerpo.estudiantes).toEqual([
      { matriculaId: 30, estado: 'FALTA', observacion: 'No llegó' },
      { matriculaId: 31, estado: 'ASISTIO', observacion: null },
    ]);
  });

  it('el rector solo ve el consolidado y quien pasa el limite del SIEE', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: rector },
      'GET /api/asistencia/grupos': {
        estado: 200,
        cuerpo: [{ grupoId: 3, anio: 2027, sede: 'Colegio El Encano', grupo: 'Sexto 01' }],
      },
      'GET /api/asistencia/grupos/3/resumen': { estado: 200, cuerpo: resumen },
    });
    renderizarEn('/asistencia', <App />);

    expect(await screen.findByText('7 h (17.5%)')).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Tomar asistencia' })).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Consolidado por grupo' })).toBeInTheDocument();
  });
});
