// Cliente HTTP comun para la API.
// Envia la cookie de sesion y el token CSRF que Spring Security deja en la cookie XSRF-TOKEN.

function leerCookie(nombre: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(nombre + '='))
    ?.split('=')[1];
}

export class ErrorApi extends Error {
  constructor(public readonly estado: number, mensaje: string) {
    super(mensaje);
  }
}

export async function llamarApi<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const encabezados = new Headers(opciones.headers);
  encabezados.set('Accept', 'application/json');
  if (opciones.body && !(opciones.body instanceof FormData)) {
    encabezados.set('Content-Type', 'application/json');
  }
  const csrf = leerCookie('XSRF-TOKEN');
  if (csrf) {
    encabezados.set('X-XSRF-TOKEN', decodeURIComponent(csrf));
  }

  const respuesta = await fetch('/api' + ruta, {
    ...opciones,
    headers: encabezados,
    credentials: 'same-origin',
  });
  if (!respuesta.ok) {
    throw new ErrorApi(respuesta.status, `Error ${respuesta.status} en ${ruta}`);
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  return respuesta.json() as Promise<T>;
}
