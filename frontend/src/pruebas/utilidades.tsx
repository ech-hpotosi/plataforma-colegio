import type { ReactNode } from 'react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { vi } from 'vitest';

type Respuesta = { estado: number; cuerpo?: unknown };

/**
 * Reemplaza fetch por respuestas fijas segun "METODO /ruta".
 * Las rutas no declaradas responden 404.
 */
export function simularApi(respuestas: Record<string, Respuesta | (() => Respuesta)>) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (entrada, opciones) => {
    const url = typeof entrada === 'string' ? entrada : entrada.toString();
    const clave = `${opciones?.method ?? 'GET'} ${url.split('?')[0]}`;
    const definida = respuestas[clave];
    const { estado, cuerpo } = typeof definida === 'function' ? definida() : (definida ?? { estado: 404 });
    return new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), { status: estado });
  });
}

export function renderizarEn(ruta: string, contenido: ReactNode) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[ruta]}>{contenido}</MemoryRouter>
    </QueryClientProvider>,
  );
}
