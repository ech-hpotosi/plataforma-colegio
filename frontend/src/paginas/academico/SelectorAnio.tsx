import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { MenuItem, TextField } from '@mui/material';
import { anioPorDefecto, listarAnios, NOMBRE_ESTADO_ANIO } from '../../api/academico';

interface Props {
  valor: number | null;
  alCambiar: (anioId: number) => void;
}

/** Lista de anios lectivos. Si no hay uno elegido, elige el que esta en curso o el mas reciente. */
export default function SelectorAnio({ valor, alCambiar }: Props) {
  const consulta = useQuery({ queryKey: ['anios'], queryFn: listarAnios });
  const anios = consulta.data ?? [];

  useEffect(() => {
    const porDefecto = consulta.data && anioPorDefecto(consulta.data);
    if (valor === null && porDefecto) {
      alCambiar(porDefecto.id);
    }
  }, [valor, consulta.data, alCambiar]);

  return (
    <TextField
      select
      size="small"
      label="Año lectivo"
      value={anios.some((a) => a.id === valor) ? valor : ''}
      onChange={(e) => alCambiar(Number(e.target.value))}
      sx={{ minWidth: 180 }}
      helperText={consulta.isSuccess && anios.length === 0 ? 'Primero cree un año lectivo' : undefined}
    >
      {anios.map((a) => (
        <MenuItem key={a.id} value={a.id}>
          {a.anio} ({NOMBRE_ESTADO_ANIO[a.estado]})
        </MenuItem>
      ))}
    </TextField>
  );
}
