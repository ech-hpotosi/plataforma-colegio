import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material';
import { guardarConfiguracion, obtenerConfiguracion, type DatosConfiguracion } from '../../api/notas';
import { mensajeDeError } from '../academico/mensajes';
import SelectorAnio from '../academico/SelectorAnio';

type Campo = keyof DatosConfiguracion;

const CAMPOS_ESCALA: { campo: Campo; texto: string }[] = [
  { campo: 'notaMinima', texto: 'Nota mínima' },
  { campo: 'notaMaxima', texto: 'Nota máxima' },
  { campo: 'notaAprobatoria', texto: 'Aprueba con' },
  { campo: 'limiteAlto', texto: 'Alto desde' },
  { campo: 'limiteSuperior', texto: 'Superior desde' },
];

const CAMPOS_PESO: { campo: Campo; texto: string }[] = [
  { campo: 'pesoSaber', texto: 'Saber (%)' },
  { campo: 'pesoHacer', texto: 'Hacer (%)' },
  { campo: 'pesoSer', texto: 'Ser (%)' },
];

const una = (n: number) => n.toFixed(1);
const antes = (n: number) => una(Math.round(n * 10 - 1) / 10);

/**
 * Escala numerica y pesos de las dimensiones del anio. El SIEE solo fija la escala nacional,
 * por eso estos valores los define el consejo academico y los registra coordinacion.
 */
export default function EscalaValoracion() {
  const queryClient = useQueryClient();
  const [anioId, setAnioId] = useState<number | null>(null);
  const [valores, setValores] = useState<Record<Campo, string> | null>(null);
  const [mensaje, setMensaje] = useState<{ tipo: 'success' | 'error'; texto: string } | null>(null);
  const [guardando, setGuardando] = useState(false);
  const config = useQuery({
    queryKey: ['notas-configuracion', anioId],
    queryFn: () => obtenerConfiguracion(anioId!),
    enabled: anioId !== null,
  });

  useEffect(() => {
    if (config.data) {
      const d = config.data;
      setValores({
        notaMinima: una(d.notaMinima),
        notaMaxima: una(d.notaMaxima),
        notaAprobatoria: una(d.notaAprobatoria),
        limiteAlto: una(d.limiteAlto),
        limiteSuperior: una(d.limiteSuperior),
        pesoSaber: String(d.pesoSaber),
        pesoHacer: String(d.pesoHacer),
        pesoSer: String(d.pesoSer),
      });
      setMensaje(null);
    }
  }, [config.data]);

  const numeros = valores
    ? (Object.fromEntries(Object.entries(valores).map(([k, v]) => [k, Number(v.replace(',', '.'))])) as Record<Campo, number>)
    : null;
  const sumaPesos = numeros ? numeros.pesoSaber + numeros.pesoHacer + numeros.pesoSer : 0;

  const guardar = async () => {
    if (!numeros || anioId === null) return;
    setGuardando(true);
    setMensaje(null);
    try {
      const guardada = await guardarConfiguracion(anioId, numeros);
      queryClient.setQueryData(['notas-configuracion', anioId], guardada);
      setMensaje({ tipo: 'success', texto: 'Escala guardada. Las notas del año se recalculan con estos valores.' });
    } catch (e) {
      setMensaje({ tipo: 'error', texto: mensajeDeError(e) });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Stack spacing={2} sx={{ maxWidth: 720 }}>
      <SelectorAnio valor={anioId} alCambiar={setAnioId} />
      {config.isFetching && !valores && <CircularProgress />}
      {config.isError && <Alert severity="error">{mensajeDeError(config.error)}</Alert>}
      {valores && numeros && config.data && (
        <>
          <Typography variant="body2" color="text.secondary">
            El SIEE adopta la escala nacional (Bajo, Básico, Alto y Superior) pero no fija los números. Mientras el consejo
            académico los confirma, se usan valores de referencia que puede cambiar aquí.
          </Typography>
          {mensaje && <Alert severity={mensaje.tipo}>{mensaje.texto}</Alert>}
          {!config.data.editable && <Alert severity="info">El año está cerrado y solo se puede consultar.</Alert>}
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" component="h2" sx={{ mb: 2 }}>
              Escala numérica
            </Typography>
            <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr', sm: 'repeat(5, 1fr)' } }}>
              {CAMPOS_ESCALA.map(({ campo, texto }) => (
                <TextField
                  key={campo}
                  label={texto}
                  value={valores[campo]}
                  onChange={(e) => setValores({ ...valores, [campo]: e.target.value })}
                  slotProps={{ htmlInput: { inputMode: 'decimal' } }}
                  disabled={!config.data.editable}
                />
              ))}
            </Box>
            <Typography variant="body2" sx={{ mt: 2 }}>
              Bajo: {una(numeros.notaMinima)} a {antes(numeros.notaAprobatoria)}. Básico: {una(numeros.notaAprobatoria)} a{' '}
              {antes(numeros.limiteAlto)}. Alto: {una(numeros.limiteAlto)} a {antes(numeros.limiteSuperior)}. Superior:{' '}
              {una(numeros.limiteSuperior)} a {una(numeros.notaMaxima)}.
            </Typography>
          </Paper>
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" component="h2" sx={{ mb: 2 }}>
              Peso de cada dimensión en la nota del periodo
            </Typography>
            <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr 1fr 1fr', sm: 'repeat(3, 160px)' } }}>
              {CAMPOS_PESO.map(({ campo, texto }) => (
                <TextField
                  key={campo}
                  label={texto}
                  type="number"
                  value={valores[campo]}
                  onChange={(e) => setValores({ ...valores, [campo]: e.target.value })}
                  disabled={!config.data.editable}
                />
              ))}
            </Box>
            <Typography variant="body2" sx={{ mt: 2 }} color={sumaPesos === 100 ? 'text.secondary' : 'error'}>
              Suman {sumaPesos} %{sumaPesos === 100 ? '.' : '; deben sumar 100 %.'}
            </Typography>
          </Paper>
          {config.data.editable && (
            <Box>
              <Button variant="contained" onClick={guardar} disabled={guardando || sumaPesos !== 100}>
                Guardar escala
              </Button>
            </Box>
          )}
        </>
      )}
    </Stack>
  );
}
