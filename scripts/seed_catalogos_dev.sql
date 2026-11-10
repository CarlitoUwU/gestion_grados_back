-- Catálogos de referencia para desarrollo local (sin API de alta).
-- Idempotente: no borra datos ni usuarios; omite lo que ya existe.
-- Uso: docker exec -i grado-db psql -U root -d grades_management < scripts/seed_catalogos_dev.sql
SET client_encoding = 'UTF8';

INSERT INTO schools (name) VALUES
  ('Ingeniería de Sistemas'),
  ('Ingeniería Civil'),
  ('Administración'),
  ('Economía'),
  ('Educación'),
  ('Contabilidad')
ON CONFLICT (name) DO NOTHING;

INSERT INTO degree_modalities (name) VALUES
  ('Tesis'),
  ('Artículo de investigación')
ON CONFLICT (name) DO NOTHING;

INSERT INTO expedient_statuses (name) VALUES
  ('Registrado'),
  ('En trámite'),
  ('Observado'),
  ('Sustentado'),
  ('Completado')
ON CONFLICT (name) DO NOTHING;

-- Docentes ficticios; el correo identifica el registro para no duplicarlo.
INSERT INTO teachers (document_number, full_name, email)
SELECT v.document_number, v.full_name, v.email
FROM (VALUES
  ('29481023', 'Elena Vargas Quispe', 'evargasq.demo@unsa.edu.pe'),
  ('40192834', 'Rafael Montes Ccori', 'rmontesc.demo@unsa.edu.pe'),
  ('31928374', 'Patricia León Huamaní', 'pleonh.demo@unsa.edu.pe'),
  ('28371928', 'Jorge Rivas Apaza', 'jrivasa.demo@unsa.edu.pe'),
  ('41029384', 'Adriana Soto Mamani', 'asotom.demo@unsa.edu.pe'),
  ('29384719', 'Daniel Fuentes Cárdenas', 'dfuentesc.demo@unsa.edu.pe'),
  ('30192837', 'Mariana Vega Condori', 'mvegac.demo@unsa.edu.pe'),
  ('42918273', 'Héctor Salinas Torres', 'hsalinast.demo@unsa.edu.pe'),
  ('29018374', 'Rosa Medina Pari', 'rmedinap.demo@unsa.edu.pe'),
  ('40381726', 'Luis Zegarra Benavente', 'lzegarrab.demo@unsa.edu.pe'),
  ('31827364', 'Carmen Delgado Ayala', 'cdelgadoa.demo@unsa.edu.pe'),
  ('28192736', 'Víctor Chávez Linares', 'vchavezl.demo@unsa.edu.pe')
) AS v(document_number, full_name, email)
WHERE NOT EXISTS (SELECT 1 FROM teachers t WHERE t.email = v.email);
