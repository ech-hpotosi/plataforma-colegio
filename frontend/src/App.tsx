import { Route, Routes } from 'react-router';
import Plantilla from './componentes/Plantilla';
import RutaProtegida from './componentes/RutaProtegida';
import Inicio from './paginas/Inicio';
import Login from './paginas/Login';
import NoEncontrada from './paginas/NoEncontrada';
import SinPermiso from './paginas/SinPermiso';
import ListaUsuarios from './paginas/usuarios/ListaUsuarios';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<RutaProtegida />}>
        <Route element={<Plantilla />}>
          <Route path="/" element={<Inicio />} />
          <Route path="/sin-permiso" element={<SinPermiso />} />
          <Route element={<RutaProtegida roles={['ADMINISTRADOR']} />}>
            <Route path="/usuarios" element={<ListaUsuarios />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
