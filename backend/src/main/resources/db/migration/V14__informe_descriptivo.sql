-- En Transicion (evaluacion cualitativa, SIEE 4.3.1) la observacion de cada dimension es el informe descriptivo
-- del periodo: avances, dificultades y recomendaciones. Necesita mas espacio que una observacion corta.
ALTER TABLE informe_estudiante ALTER COLUMN observacion TYPE VARCHAR(1500);
