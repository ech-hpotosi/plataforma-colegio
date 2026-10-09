-- Nombres con tildes en los datos iniciales de grados y sedes (V3 y V4 se escribieron sin tildes).

UPDATE grado SET nombre = 'Transición' WHERE nombre = 'Transicion';
UPDATE grado SET nombre = 'Séptimo' WHERE nombre = 'Septimo';
UPDATE grado SET nombre = 'Décimo' WHERE nombre = 'Decimo';
UPDATE grado SET nombre = 'Undécimo' WHERE nombre = 'Undecimo';

UPDATE sede SET nombre = 'Escuela Rural Mixta El Motilón' WHERE nombre = 'Escuela Rural Mixta El Motilon';
UPDATE sede SET nombre = 'Escuela Rural Mixta Santa Lucía' WHERE nombre = 'Escuela Rural Mixta Santa Lucia';
