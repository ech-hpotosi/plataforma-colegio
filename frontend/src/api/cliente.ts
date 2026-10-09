// Cliente HTTP comun para la API.
// Envia la cookie de sesion y el token CSRF que Spring Security deja en la cookie XSRF-TOKEN.

function leerCookie(nombre: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(nombre + '='))
    ?.split('=')[1];
}

/** Error devuelto por la API, con el mensaje y los errores por campo que envia el backend. */
export class ErrorApi extends Error {
  constructor(
    public readonly estado: number,
    mensaje: string,
    public readonly errores: Record<string, string> = {},
  ) {
    super(mensaje);
  }
}

const METODOS_SEGUROS = ['GET', 'HEAD', 'OPTIONS'];

export async function llamarApi<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const metodo = (opciones.method ?? 'GET').toUpperCase();
  // Al cerrar sesion el servidor borra la cookie CSRF. Antes de un POST, PUT o DELETE sin cookie,
  // una peticion GET cualquiera hace que el servidor entregue un token nuevo.
  if (!METODOS_SEGUROS.includes(metodo) && !leerCookie('XSRF-TOKEN')) {
    await fetch('/api/salud', { credentials: 'same-origin' });
  }
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
    throw await errorDe(respuesta);
  }
  if (respuesta.status === 204) {
    return undefined as T;
  }
  return respuesta.json() as Promise<T>;
}

/** Descarga un archivo de la API (por ejemplo un PDF) con el nombre que envia el servidor. */
export async function descargarArchivo(ruta: string, nombrePorDefecto: string): Promise<void> {
  const respuesta = await fetch('/api' + ruta, {
    headers: { Accept: 'application/pdf, application/json' },
    credentials: 'same-origin',
  });
  if (!respuesta.ok) {
    throw await errorDe(respuesta);
  }
  const disposicion = respuesta.headers.get('Content-Disposition') ?? '';
  const nombre = /filename="?([^";]+)"?/.exec(disposicion)?.[1] ?? nombrePorDefecto;
  const url = URL.createObjectURL(await respuesta.blob());
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function errorDe(respuesta: Response): Promise<ErrorApi> {
  let mensaje = `Error ${respuesta.status}`;
  let errores: Record<string, string> = {};
  try {
    const cuerpo = await respuesta.json();
    mensaje = cuerpo.mensaje ?? mensaje;
    errores = cuerpo.errores ?? {};
  } catch {
    // La respuesta no trae JSON; se deja el mensaje generico
  }
  return new ErrorApi(respuesta.status, mensaje, errores);
}
