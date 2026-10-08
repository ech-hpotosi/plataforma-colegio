import { Route, Routes } from 'react-router-dom';
import Inicio from './paginas/Inicio';
import NoEncontrada from './paginas/NoEncontrada';

// Las rutas protegidas por rol se agregan con el modulo de usuarios (login).
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
