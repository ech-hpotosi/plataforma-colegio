-- Sedes de la IEM El Encano segun la pagina del colegio (confirmadas por el usuario: 11 sedes).
-- El codigo DANE y la direccion se completan desde la pantalla de sedes.
-- Si ya existe una sede con el mismo nombre no se duplica.
INSERT INTO sede (nombre, principal)
SELECT nombre, principal
FROM (VALUES
    ('Colegio El Encano', TRUE),
    ('Escuela Integrada El Encano', FALSE),
    ('Escuela Rural Mixta Campo Alegre', FALSE),
    ('Escuela Rural Mixta El Puerto', FALSE),
    ('Escuela Rural Mixta El Carrizo', FALSE),
    ('Escuela Rural Mixta El Motilon', FALSE),
    ('Escuela Rural Mixta El Romerillo', FALSE),
    ('Escuela Rural Mixta Ramos', FALSE),
    ('Escuela Rural Mixta Santa Lucia', FALSE),
    ('Escuela Rural Mixta Santa Isabel', FALSE),
    ('Escuela Rural Mixta El Naranjal', FALSE)
) AS nuevas (nombre, principal)
WHERE NOT EXISTS (SELECT 1 FROM sede s WHERE lower(s.nombre) = lower(nuevas.nombre));

-- Solo una sede principal: si ya habia otra marcada, se conserva esa
UPDATE sede SET principal = FALSE
WHERE nombre = 'Colegio El Encano'
  AND EXISTS (SELECT 1 FROM sede s WHERE s.principal AND s.nombre <> 'Colegio El Encano');
