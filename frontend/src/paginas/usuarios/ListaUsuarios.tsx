import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { buscarUsuarios, desbloquearUsuario, type Usuario } from '../../api/usuarios';
import { NOMBRE_ROL } from '../../tipos';
import DialogoContrasena from './DialogoContrasena';
import DialogoUsuario from './DialogoUsuario';

/** Administracion de usuarios: busqueda, creacion, edicion, cambio de contrasena y desbloqueo. */
export default function ListaUsuarios() {
  const queryClient = useQueryClient();
  const [buscar, setBuscar] = useState('');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [editando, setEditando] = useState<Usuario | 'nuevo' | null>(null);
  const [cambiandoContrasena, setCambiandoContrasena] = useState<Usuario | null>(null);
  const [menu, setMenu] = useState<{ ancla: HTMLElement; usuario: Usuario } | null>(null);

  const consulta = useQuery({
    queryKey: ['usuarios', buscar, pagina, tamano],
    queryFn: () => buscarUsuarios(buscar, pagina, tamano),
    placeholderData: keepPreviousData,
  });

  const desbloquear = useMutation({
    mutationFn: (id: number) => desbloquearUsuario(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['usuarios'] }),
  });

  const recargar = () => queryClient.invalidateQueries({ queryKey: ['usuarios'] });

  return (
    <>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <Typography variant="h5" component="h1" sx={{ flexGrow: 1 }}>
          Usuarios
        </Typography>
        <TextField
          size="small"
          label="Buscar por nombre, usuario o documento"
          value={buscar}
          onChange={(e) => {
            setBuscar(e.target.value);
            setPagina(0);
          }}
          sx={{ minWidth: 280 }}
        />
        <Button variant="contained" onClick={() => setEditando('nuevo')}>
          Nuevo usuario
        </Button>
      </Box>

      {consulta.isError && <Alert severity="error">{consulta.error.message}</Alert>}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Documento</TableCell>
              <TableCell>Usuario</TableCell>
              <TableCell>Roles</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {consulta.data?.contenido.map((usuario) => (
              <TableRow key={usuario.id} hover>
                <TableCell>
                  {usuario.apellidos} {usuario.nombres}
                </TableCell>
                <TableCell>
                  {usuario.tipoDocumento} {usuario.numeroDocumento}
                </TableCell>
                <TableCell>{usuario.nombreUsuario}</TableCell>
                <TableCell>{usuario.roles.map((rol) => NOMBRE_ROL[rol]).join(', ')}</TableCell>
                <TableCell>
                  {!usuario.activo && <Chip size="small" label="Inactivo" />}
                  {usuario.bloqueado && <Chip size="small" color="warning" label="Bloqueado" />}
                  {usuario.activo && !usuario.bloqueado && <Chip size="small" color="success" label="Activo" />}
                </TableCell>
                <TableCell align="right">
                  <IconButton
                    aria-label={`Acciones de ${usuario.nombreUsuario}`}
                    onClick={(e) => setMenu({ ancla: e.currentTarget, usuario })}
                  >
                    ...
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {consulta.data?.contenido.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No hay usuarios que coincidan con la busqueda
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={consulta.data?.totalElementos ?? 0}
          page={pagina}
          rowsPerPage={tamano}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_, nueva) => setPagina(nueva)}
          onRowsPerPageChange={(e) => {
            setTamano(Number(e.target.value));
            setPagina(0);
          }}
          labelRowsPerPage="Filas por pagina"
          labelDisplayedRows={({ from, to, count }) => `${from} a ${to} de ${count}`}
        />
      </TableContainer>

      <Menu anchorEl={menu?.ancla} open={menu !== null} onClose={() => setMenu(null)}>
        <MenuItem
          onClick={() => {
            setEditando(menu!.usuario);
            setMenu(null);
          }}
        >
          Editar
        </MenuItem>
        <MenuItem
          onClick={() => {
            setCambiandoContrasena(menu!.usuario);
            setMenu(null);
          }}
        >
          Cambiar contrasena
        </MenuItem>
        {menu?.usuario.bloqueado && (
          <MenuItem
            onClick={() => {
              desbloquear.mutate(menu.usuario.id);
              setMenu(null);
            }}
          >
            Desbloquear
          </MenuItem>
        )}
      </Menu>

      {editando !== null && (
        <DialogoUsuario
          usuario={editando === 'nuevo' ? null : editando}
          alCerrar={() => setEditando(null)}
          alGuardar={() => {
            setEditando(null);
            recargar();
          }}
        />
      )}
      {cambiandoContrasena && (
        <DialogoContrasena usuario={cambiandoContrasena} alCerrar={() => setCambiandoContrasena(null)} />
      )}
    </>
  );
}
