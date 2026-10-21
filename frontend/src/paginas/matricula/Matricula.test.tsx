import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const acudiente = { id: 7, nombreUsuario: '27123456', nombreCompleto: 'Rosa Jojoa', roles: ['ACUDIENTE'] };
const secretaria = { id: 4, nombreUsuario: 'secretaria', nombreCompleto: 'Sara Secretaria', roles: ['SECRETARIA'] };

const oferta = [
  {
    anioLectivoId: 3,
    anio: 2027,
    sedes: [{ id: 1, nombre: 'Colegio El Encano' }],
    grados: [{ id: 7, nombre: 'Sexto' }],
  },
];

const documento = (id: number | null, nombre: string, estado: string | null, observacion: string | null = null) => ({
  tipoDocumentoId: (id ?? 90) + 100,
  nombre,
  descripcion: null,
  obligatorio: true,
  id,
  nombreArchivo: id ? 'archivo.pdf' : null,
  tipoMime: id ? 'application/pdf' : null,
  tamanoBytes: id ? 204800 : null,
  fechaCarga: id ? '2026-11-02T10:15:00' : null,
  estado,
  observacion,
});

const solicitud = {
  id: 50,
  anioLectivoId: 3,
  anio: 2027,
  sedeId: 1,
  sede: 'Colegio El Encano',
  gradoId: 7,
  grado: 'Sexto',
  tipo: 'NUEVO',
  parentesco: 'MADRE',
  estado: 'CON_OBSERVACIONES',
  editable: true,
  tipoDocumento: 'TI',
  numeroDocumento: '1085123456',
  nombres: 'Luna',
  apellidos: 'Jojoa Botina',
  fechaNacimiento: '2015-04-02',
  genero: 'FEMENINO',
  direccion: 'Vereda Santa Lucía',
  telefono: null,
  correo: null,
  eps: 'Emssanar',
  grupoSanguineo: null,
  condicionDiscapacidad: null,
  condicionesEspeciales: null,
  institucionAnterior: 'Escuela El Puerto',
  autorizaDatos: true,
  fechaCreacion: '2026-11-02T10:00:00',
  fechaEnvio: '2026-11-02T10:20:00',
  observacion: 'Vuelva a subir la tarjeta de identidad',
  solicitante: { nombre: 'Rosa Jojoa', tipoDocumento: 'CC', numeroDocumento: '27123456', telefono: '3101234567', correo: null },
  estudianteId: null,
  grupo: null,
  documentos: [
    documento(1, 'Documento de identidad del estudiante', 'RECHAZADO', 'La foto está borrosa'),
    documento(2, 'Certificado de afiliación a salud', 'APROBADO'),
  ],
  revisiones: [],
};

describe('Matrícula en línea', () => {
  afterEach(() => vi.restoreAllMocks());

  it('un acudiente crea su cuenta y llega a la matrícula en línea', async () => {
    let registrado = false;
    const api = simularApi({
      'GET /api/yo': () => (registrado ? { estado: 200, cuerpo: acudiente } : { estado: 401 }),
      'GET /api/salud': { estado: 200, cuerpo: {} },
      'POST /api/auth/registro': () => {
        registrado = true;
        return { estado: 201, cuerpo: acudiente };
      },
      'GET /api/matricula-en-linea/oferta': { estado: 200, cuerpo: oferta },
      'GET /api/solicitudes-matricula/mias': { estado: 200, cuerpo: [] },
    });
    renderizarEn('/login', <App />);

    await userEvent.click(await screen.findByRole('link', { name: 'cree su cuenta' }));
    await userEvent.type(screen.getByLabelText('Número de documento'), '27123456');
    await userEvent.type(screen.getByLabelText('Nombres'), 'Rosa');
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Jojoa');
    await userEvent.type(screen.getByLabelText('Celular'), '3101234567');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'clave-rosa-1');
    await userEvent.type(screen.getByLabelText('Repita la contraseña'), 'otra-clave');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(await screen.findByText('Las contraseñas no coinciden')).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText('Repita la contraseña'));
    await userEvent.type(screen.getByLabelText('Repita la contraseña'), 'clave-rosa-1');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(await screen.findByText(/La matrícula para el año 2027 está abierta/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Nueva solicitud' })).toBeInTheDocument();
    const cuerpo = JSON.parse(
      api.mock.calls.find(([url]) => String(url) === '/api/auth/registro')![1]!.body as string,
    );
    expect(cuerpo).toEqual({
      tipoDocumento: 'CC',
      numeroDocumento: '27123456',
      nombres: 'Rosa',
      apellidos: 'Jojoa',
      telefono: '3101234567',
      correo: '',
      contrasena: 'clave-rosa-1',
    });
  }, 15000);

  it('el acudiente ve qué corregir y no puede reenviar sin cambiar el documento rechazado', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: acudiente },
      'GET /api/solicitudes-matricula/50': { estado: 200, cuerpo: solicitud },
      'GET /api/matricula-en-linea/oferta': { estado: 200, cuerpo: oferta },
    });
    renderizarEn('/matricula-en-linea/50', <App />);

    expect(await screen.findByText(/Vuelva a subir la tarjeta de identidad/)).toBeInTheDocument();
    expect(screen.getByText('La foto está borrosa')).toBeInTheDocument();
    expect(screen.getByText(/Cambie los documentos rechazados/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('checkbox'));
    expect(screen.getByRole('button', { name: 'Volver a enviar' })).toBeDisabled();
  });

  it('el acudiente no entra a la bandeja de la secretaría', async () => {
    simularApi({ 'GET /api/yo': { estado: 200, cuerpo: acudiente } });
    renderizarEn('/solicitudes-matricula', <App />);

    expect(await screen.findByText('No tiene permiso para ver esta página')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Matrícula en línea' })).toBeInTheDocument();
  });

  it('la secretaría aprueba la solicitud y elige el grupo', async () => {
    const enviada = {
      ...solicitud,
      estado: 'ENVIADA',
      editable: false,
      observacion: null,
      documentos: [
        documento(1, 'Documento de identidad del estudiante', 'PENDIENTE'),
        documento(2, 'Certificado de afiliación a salud', 'APROBADO'),
      ],
    };
    const api = simularApi({
      'GET /api/yo': { estado: 200, cuerpo: secretaria },
      'GET /api/solicitudes-matricula/50': { estado: 200, cuerpo: enviada },
      'GET /api/grupos': {
        estado: 200,
        cuerpo: [
          { id: 31, anioLectivoId: 3, sedeId: 1, sede: 'Colegio El Encano', gradoId: 7, grado: 'Sexto', nombre: '6-01', jornada: 'MANANA', cupo: 35, directorId: null, director: null },
          { id: 32, anioLectivoId: 3, sedeId: 1, sede: 'Colegio El Encano', gradoId: 8, grado: 'Séptimo', nombre: '7-01', jornada: 'MANANA', cupo: 35, directorId: null, director: null },
        ],
      },
      'POST /api/solicitudes-matricula/50/aprobar': {
        estado: 200,
        cuerpo: { ...enviada, estado: 'APROBADA', estudianteId: 80, grupo: 'Sexto 6-01' },
      },
    });
    renderizarEn('/solicitudes-matricula/50', <App />);

    expect(await screen.findAllByRole('link', { name: /Ver archivo.pdf/ })).toHaveLength(2);
    await userEvent.click(screen.getByRole('button', { name: 'Aprobar' }));
    const dialogo = await screen.findByRole('dialog');
    await userEvent.click(within(dialogo).getByLabelText('Grupo (opcional)'));
    // Solo se ofrecen los grupos del grado solicitado
    expect(screen.queryByRole('option', { name: 'Séptimo 7-01' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('option', { name: 'Sexto 6-01' }));
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Aprobar y matricular' }));

    expect(await screen.findByText(/Matriculado en Sexto 6-01/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Abrir la ficha del estudiante' })).toHaveAttribute('href', '/estudiantes/80');
    const llamada = api.mock.calls.find(([url]) => String(url) === '/api/solicitudes-matricula/50/aprobar')!;
    expect(JSON.parse(llamada[1]!.body as string)).toEqual({ grupoId: 31 });
  });
});
