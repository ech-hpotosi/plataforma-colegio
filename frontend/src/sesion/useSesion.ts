import { useQuery } from '@tanstack/react-query';
import { cerrarSesion, consultarUsuarioActual, type UsuarioActual } from '../api/auth';
import type { Rol } from '../tipos';

export const CLAVE_SESION = ['sesion'];

/** Usuario con sesion abierta y utilidades para revisar roles y cerrar sesion. */
export function useSesion() {
  const consulta = useQuery({
    queryKey: CLAVE_SESION,
    queryFn: consultarUsuarioActual,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
  const usuario: UsuarioActual | null = consulta.data ?? null;

  return {
    usuario,
    cargando: consulta.isPending,
    tieneAlgunRol: (roles: Rol[]) => usuario !== null && roles.some((rol) => usuario.roles.includes(rol)),
    /**
     * Cierra la sesion y recarga la aplicacion en /login. La recarga borra de memoria
     * todos los datos del usuario anterior, por si otra persona usa el mismo equipo.
     */
    salir: async () => {
      await cerrarSesion();
      window.location.assign('/login');
    },
  };
}
