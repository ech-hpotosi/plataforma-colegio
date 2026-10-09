import { Navigate, Route, Routes } from 'react-router';
import Plantilla from './componentes/Plantilla';
import RutaProtegida from './componentes/RutaProtegida';
import AniosLectivos from './paginas/academico/AniosLectivos';
import Areas from './paginas/academico/Areas';
import Docentes from './paginas/academico/Docentes';
import EstructuraAcademica from './paginas/academico/EstructuraAcademica';
import Grados from './paginas/academico/Grados';
import Grupos from './paginas/academico/Grupos';
import PlanEstudio from './paginas/academico/PlanEstudio';
import Sedes from './paginas/academico/Sedes';
import Asistencia from './paginas/asistencia/Asistencia';
import ConsolidadoAsistencia from './paginas/asistencia/ConsolidadoAsistencia';
import TomarAsistencia from './paginas/asistencia/TomarAsistencia';
import DetalleEstudiante from './paginas/estudiantes/DetalleEstudiante';
import ListaEstudiantes from './paginas/estudiantes/ListaEstudiantes';
import Inicio from './paginas/Inicio';
import Login from './paginas/Login';
import ConsolidadoNotas from './paginas/notas/ConsolidadoNotas';
import EscalaValoracion from './paginas/notas/EscalaValoracion';
import Notas from './paginas/notas/Notas';
import PlanillaNotas from './paginas/notas/PlanillaNotas';
import NoEncontrada from './paginas/NoEncontrada';
import SinPermiso from './paginas/SinPermiso';
import ListaUsuarios from './paginas/usuarios/ListaUsuarios';
import Seguimiento from './paginas/seguimiento/Seguimiento';

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
          <Route
            element={<RutaProtegida roles={['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA']} />}
          >
            <Route path="/estudiantes" element={<ListaEstudiantes />} />
            <Route path="/estudiantes/:id" element={<DetalleEstudiante />} />
            <Route path="/seguimiento" element={<Seguimiento />} />
          </Route>
          <Route
            element={
              <RutaProtegida roles={['ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE']} />
            }
          >
            <Route path="/asistencia" element={<Asistencia />}>
              <Route path="tomar" element={<TomarAsistencia />} />
              <Route path="consolidado" element={<ConsolidadoAsistencia />} />
            </Route>
            <Route path="/notas" element={<Notas />}>
              <Route path="planilla" element={<PlanillaNotas />} />
              <Route path="consolidado" element={<ConsolidadoNotas />} />
              <Route element={<RutaProtegida roles={['ADMINISTRADOR', 'COORDINADOR_ACADEMICO']} />}>
                <Route path="escala" element={<EscalaValoracion />} />
              </Route>
            </Route>
          </Route>
          <Route element={<RutaProtegida roles={['ADMINISTRADOR', 'COORDINADOR_ACADEMICO']} />}>
            <Route path="/academico" element={<EstructuraAcademica />}>
              <Route index element={<Navigate to="sedes" replace />} />
              <Route path="sedes" element={<Sedes />} />
              <Route path="anios" element={<AniosLectivos />} />
              <Route path="grados" element={<Grados />} />
              <Route path="areas" element={<Areas />} />
              <Route path="plan" element={<PlanEstudio />} />
              <Route path="docentes" element={<Docentes />} />
              <Route path="grupos" element={<Grupos />} />
            </Route>
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<NoEncontrada />} />
    </Routes>
  );
}
