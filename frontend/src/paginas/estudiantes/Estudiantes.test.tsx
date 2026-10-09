import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { renderizarEn, simularApi } from '../../pruebas/utilidades';

const secretaria = { id: 4, nombreUsuario: 'secretaria', nombreCompleto: 'Sara Secretaria', roles: ['SECRETARIA'] };
const rector = { id: 5, nombreUsuario: 'rector', nombreCompleto: 'Raul Rector', roles: ['RECTOR'] };
const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };

const ficha = {
  id: 10,
  tipoDocumento: 'TI',
  numeroDocumento: '1085123456',
  nombres: 'Valentina',
  apellidos: 'Jojoa Botina',
  telefono: null,
  correo: null,
  codigo: 'E-001',
  fechaNacimiento: '2014-03-15',
  genero: 'FEMENINO',
  direccion: 'Vereda Santa Lucía',
  eps: 'Emssanar',
  grupoSanguineo: 'O+',
  condicionDiscapacidad: null,
  tienePiar: false,
  condicionesEspeciales: null,
  estado: 'ACTIVO',
  acudientes: [
    {
      id: 20,
      tipoDocumento: 'CC',
      numeroDocumento: '27123456',
      nombres: 'Rosa',
      apellidos: 'Botina',
      telefono: '3101234567',
      correo: null,
      ocupacion: 'Agricultora',
      parentesco: 'MADRE',
      principal: true,
    },
  ],
};

const matriculas = [
  {
    id: 30,
    estudianteId: 10,
    anioLectivoId: 1,
    anio: 2027,
    grupoId: 5,
    grupo: 'Sexto 6-01',
    sede: 'Colegio El Encano',
    fechaMatricula: '2027-01-20',
    estado: 'ACTIVA',
    fechaRetiro: null,
    motivoRetiro: null,
  },
];

const pagina = {
  contenido: [
    { id: 10, tipoDocumento: 'TI', numeroDocumento: '1085123456', nombres: 'Valentina', apellidos: 'Jojoa Botina', codigo: 'E-001', estado: 'ACTIVO' },
  ],
  pagina: 0,
  tamano: 20,
  totalElementos: 1,
  totalPaginas: 1,
};

describe('Estudiantes', () => {
  afterEach(() => vi.restoreAllMocks());

  it('la secretaria busca, abre la ficha y ve acudientes y matrícula', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: secretaria },
      'GET /api/estudiantes': { estado: 200, cuerpo: pagina },
      'GET /api/estudiantes/10': { estado: 200, cuerpo: ficha },
      'GET /api/estudiantes/10/matriculas': { estado: 200, cuerpo: matriculas },
    });
    renderizarEn('/estudiantes', <App />);

    await userEvent.click(await screen.findByText('Jojoa Botina Valentina'));

    expect(await screen.findByRole('heading', { name: 'Valentina Jojoa Botina' })).toBeInTheDocument();
    expect(screen.getByText('Vereda Santa Lucía')).toBeInTheDocument();
    const filaMadre = screen.getByText(/Rosa Botina/).closest('tr')!;
    expect(within(filaMadre).getByText('Madre')).toBeInTheDocument();
    expect(within(filaMadre).getByText('Principal')).toBeInTheDocument();
    expect(await screen.findByText('Sexto 6-01')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Editar ficha' })).toBeInTheDocument();
  });

  it('el rector consulta pero no ve botones para modificar', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: rector },
      'GET /api/estudiantes/10': { estado: 200, cuerpo: ficha },
      'GET /api/estudiantes/10/matriculas': { estado: 200, cuerpo: matriculas },
    });
    renderizarEn('/estudiantes/10', <App />);

    expect(await screen.findByRole('heading', { name: 'Valentina Jojoa Botina' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Editar ficha' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Agregar acudiente' })).not.toBeInTheDocument();
  });

  it('un docente no puede ver estudiantes', async () => {
    simularApi({ 'GET /api/yo': { estado: 200, cuerpo: docente } });
    renderizarEn('/estudiantes', <App />);

    expect(await screen.findByText('No tiene permiso para ver esta página')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Estudiantes' })).not.toBeInTheDocument();
  });

  it('el formulario de nuevo estudiante pide género y fecha de nacimiento', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: secretaria },
      'GET /api/estudiantes': { estado: 200, cuerpo: { ...pagina, contenido: [], totalElementos: 0 } },
    });
    renderizarEn('/estudiantes', <App />);

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo estudiante' }));
    await userEvent.type(screen.getByLabelText('Número de documento'), '1085999999');
    await userEvent.type(screen.getByLabelText('Nombres'), 'Juan');
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Chicunque');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByText('Seleccione el género')).toBeInTheDocument();
    expect(screen.getByText('Ingrese la fecha de nacimiento')).toBeInTheDocument();
  });
});
