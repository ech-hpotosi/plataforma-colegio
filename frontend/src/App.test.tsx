import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { renderizarEn, simularApi } from './pruebas/utilidades';

const admin = { id: 1, nombreUsuario: 'admin', nombreCompleto: 'Administrador Plataforma', roles: ['ADMINISTRADOR'] };
const docente = { id: 2, nombreUsuario: 'docente', nombreCompleto: 'Ana Docente', roles: ['DOCENTE'] };

describe('Sesion y rutas protegidas', () => {
  afterEach(() => vi.restoreAllMocks());

  it('sin sesion redirige al login', async () => {
    simularApi({ 'GET /api/yo': { estado: 401 } });
    renderizarEn('/', <App />);
    expect(await screen.findByRole('button', { name: 'Ingresar' })).toBeInTheDocument();
  });

  it('el login correcto muestra la bienvenida', async () => {
    let conSesion = false;
    simularApi({
      'GET /api/yo': () => (conSesion ? { estado: 200, cuerpo: admin } : { estado: 401 }),
      'POST /api/auth/login': () => {
        conSesion = true;
        return { estado: 200, cuerpo: admin };
      },
    });
    renderizarEn('/', <App />);

    await userEvent.type(await screen.findByLabelText('Usuario'), 'admin');
    await userEvent.type(screen.getByLabelText('Contrasena'), 'clave-de-prueba');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Bienvenido, Administrador Plataforma')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Usuarios' })).toBeInTheDocument();
  });

  it('el login fallido muestra el mensaje del servidor', async () => {
    simularApi({
      'GET /api/yo': { estado: 401 },
      'POST /api/auth/login': { estado: 401, cuerpo: { mensaje: 'Usuario o contrasena incorrectos', errores: {} } },
    });
    renderizarEn('/login', <App />);

    await userEvent.type(await screen.findByLabelText('Usuario'), 'admin');
    await userEvent.type(screen.getByLabelText('Contrasena'), 'mala');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Usuario o contrasena incorrectos')).toBeInTheDocument();
  });

  it('un docente no ve el menu de usuarios ni puede entrar a esa pagina', async () => {
    simularApi({ 'GET /api/yo': { estado: 200, cuerpo: docente } });
    renderizarEn('/usuarios', <App />);

    expect(await screen.findByText('No tiene permiso para ver esta pagina')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument();
  });

  it('el administrador ve la lista de usuarios', async () => {
    simularApi({
      'GET /api/yo': { estado: 200, cuerpo: admin },
      'GET /api/usuarios': {
        estado: 200,
        cuerpo: {
          contenido: [
            {
              id: 1,
              nombreUsuario: 'admin',
              activo: true,
              bloqueado: false,
              ultimoAcceso: null,
              roles: ['ADMINISTRADOR'],
              tipoDocumento: 'CC',
              numeroDocumento: '0',
              nombres: 'Administrador',
              apellidos: 'Plataforma',
              telefono: null,
              correo: null,
            },
          ],
          pagina: 0,
          tamano: 20,
          totalElementos: 1,
          totalPaginas: 1,
        },
      },
    });
    renderizarEn('/usuarios', <App />);

    expect(await screen.findByText('Plataforma Administrador')).toBeInTheDocument();
    expect(screen.getByText('Activo')).toBeInTheDocument();
  });
});
