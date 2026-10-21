import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link as RouterLink } from 'react-router';
import {
  Alert,
  Box,
  Button,
  FormControlLabel,
  Paper,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  descargarPlantillaImportacion,
  importarEstudiantes,
  type AccionImportacion,
  type ResultadoImportacion,
} from '../../api/estudiantes';
import Encabezado from '../../componentes/Encabezado';
import Estado, { type Tono } from '../../componentes/Estado';
import SelectorAnio from '../academico/SelectorAnio';
import { mensajeDeError } from '../academico/mensajes';

const ACCION: Record<AccionImportacion, { tono: Tono; revision: string; aplicado: string }> = {
  CREAR: { tono: 'positivo', revision: 'Nuevo', aplicado: 'Creado' },
  ACTUALIZAR: { tono: 'informativo', revision: 'Se actualiza', aplicado: 'Actualizado' },
  ERROR: { tono: 'negativo', revision: 'No se carga', aplicado: 'No se cargó' },
};

/**
 * Carga inicial de estudiantes desde Excel. Primero se revisa el archivo y se muestra que pasara con cada
 * fila; solo al confirmar se guardan las fichas y matriculas.
 */
export default function ImportarEstudiantes() {
  const clienteConsultas = useQueryClient();
  const entrada = useRef<HTMLInputElement>(null);
  const [anioId, setAnioId] = useState<number | null>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [resultado, setResultado] = useState<ResultadoImportacion | null>(null);
  const [soloObservaciones, setSoloObservaciones] = useState(false);

  const importacion = useMutation({
    mutationFn: (confirmar: boolean) => importarEstudiantes(anioId!, archivo!, confirmar),
    onSuccess: (datos) => {
      setResultado(datos);
      if (datos.aplicado) {
        clienteConsultas.invalidateQueries({ queryKey: ['estudiantes'] });
      }
    },
  });
  const plantilla = useMutation({ mutationFn: descargarPlantillaImportacion });

  const reiniciar = () => {
    setResultado(null);
    importacion.reset();
  };

  const validas = resultado ? resultado.creados + resultado.actualizados : 0;
  const filas = (resultado?.filas ?? []).filter(
    (f) => !soloObservaciones || f.errores.length > 0 || f.avisos.length > 0,
  );

  return (
    <>
      <Encabezado
        titulo="Importar estudiantes"
        descripcion="Cargue los estudiantes desde un archivo de Excel, por ejemplo el que exporta OzetaRed o el SIMAT."
        acciones={
          <Button variant="outlined" component={RouterLink} to="/estudiantes">
            Volver a estudiantes
          </Button>
        }
      />

      <Paper sx={{ p: 2.5, mb: 2 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 760 }}>
          El archivo necesita como mínimo tipo y número de documento, nombres, apellidos, fecha de nacimiento,
          género y grado. La sede y el grupo se buscan por nombre en la estructura académica del año. Si el
          estudiante ya existe se actualiza su ficha; volver a cargar el mismo archivo no crea duplicados.
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
          <SelectorAnio
            valor={anioId}
            alCambiar={(id) => {
              setAnioId(id);
              reiniciar();
            }}
          />
          <input
            ref={entrada}
            type="file"
            hidden
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            aria-label="Archivo de estudiantes"
            onChange={(e) => {
              setArchivo(e.target.files?.[0] ?? null);
              reiniciar();
              e.target.value = '';
            }}
          />
          <Button variant="outlined" onClick={() => entrada.current?.click()}>
            {archivo ? 'Cambiar archivo' : 'Elegir archivo'}
          </Button>
          {archivo && <Typography variant="body2">{archivo.name}</Typography>}
          <Button
            variant="contained"
            disabled={!archivo || anioId === null || importacion.isPending}
            onClick={() => importacion.mutate(false)}
          >
            {importacion.isPending && !resultado ? 'Revisando...' : 'Revisar archivo'}
          </Button>
          <Box sx={{ flexGrow: 1 }} />
          <Button size="small" disabled={plantilla.isPending} onClick={() => plantilla.mutate()}>
            Descargar plantilla
          </Button>
        </Box>
      </Paper>

      {importacion.isError && <Alert severity="error" sx={{ mb: 2 }}>{mensajeDeError(importacion.error)}</Alert>}
      {plantilla.isError && <Alert severity="error" sx={{ mb: 2 }}>{mensajeDeError(plantilla.error)}</Alert>}

      {resultado && (
        <>
          {resultado.aplicado ? (
            <Alert severity="success" sx={{ mb: 2 }}>
              Carga terminada en el año {resultado.anio}: {resultado.creados} estudiantes nuevos y{' '}
              {resultado.actualizados} actualizados.
              {resultado.conErrores > 0 &&
                ` ${resultado.conErrores} filas no se cargaron; corríjalas en el archivo y vuelva a cargarlo.`}
            </Alert>
          ) : (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 2,
                mb: 2,
                flexWrap: 'wrap',
                justifyContent: 'space-between',
              }}
            >
              <Typography>
                {resultado.total} filas: <strong>{resultado.creados}</strong> nuevos,{' '}
                <strong>{resultado.actualizados}</strong> se actualizan y <strong>{resultado.conErrores}</strong>{' '}
                con errores. Todavía no se ha guardado nada.
              </Typography>
              <Button
                variant="contained"
                disabled={validas === 0 || importacion.isPending}
                onClick={() => importacion.mutate(true)}
              >
                {importacion.isPending ? 'Cargando...' : `Cargar ${validas} estudiantes en ${resultado.anio}`}
              </Button>
            </Box>
          )}

          <FormControlLabel
            control={<Switch checked={soloObservaciones} onChange={(e) => setSoloObservaciones(e.target.checked)} />}
            label="Ver solo filas con errores o avisos"
            sx={{ mb: 1 }}
          />
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Fila</TableCell>
                  <TableCell>Documento</TableCell>
                  <TableCell>Estudiante</TableCell>
                  <TableCell>Grado</TableCell>
                  <TableCell>Grupo</TableCell>
                  <TableCell>Resultado</TableCell>
                  <TableCell>Observaciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filas.map((f) => (
                  <TableRow key={f.fila}>
                    <TableCell>{f.fila}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{f.documento}</TableCell>
                    <TableCell>{f.estudiante}</TableCell>
                    <TableCell>{f.grado}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{f.grupo}</TableCell>
                    <TableCell>
                      <Estado
                        tono={ACCION[f.accion].tono}
                        texto={resultado.aplicado ? ACCION[f.accion].aplicado : ACCION[f.accion].revision}
                      />
                    </TableCell>
                    <TableCell>
                      {f.errores.map((e) => (
                        <Typography key={e} variant="body2" color="error">
                          {e}
                        </Typography>
                      ))}
                      {f.avisos.map((a) => (
                        <Typography key={a} variant="body2" color="text.secondary">
                          {a}
                        </Typography>
                      ))}
                    </TableCell>
                  </TableRow>
                ))}
                {filas.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      Ninguna fila tiene errores ni avisos
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </>
  );
}
