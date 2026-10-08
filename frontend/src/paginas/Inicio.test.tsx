import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Inicio from './Inicio';

function renderizar() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <Inicio />
    </QueryClientProvider>,
  );
}

describe('Inicio', () => {
  afterEach(() => vi.restoreAllMocks());

  it('muestra servidor en linea cuando la API responde', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ estado: 'OK', aplicacion: 'plataforma-colegio', fecha: '' }), { status: 200 }),
    );
    renderizar();
    expect(await screen.findByText('Servidor en linea')).toBeInTheDocument();
  });

  it('muestra error cuando la API no responde', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 500 }));
    renderizar();
    expect(await screen.findByText('No hay conexion con el servidor')).toBeInTheDocument();
  });
});
