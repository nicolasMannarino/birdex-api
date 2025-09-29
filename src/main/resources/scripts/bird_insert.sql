-- 001_all.sql  (PostgreSQL)
-- Crea extensión, esquema y datos iniciales para birds/colors/rarities + tablas puente
-- + Provinces y Migratory Waves (mes/provincia por ave)

-- ====== EXTENSION ======
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ====== ESQUEMA ======
CREATE TABLE IF NOT EXISTS birds (
    bird_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    common_name TEXT NOT NULL,
    size TEXT NOT NULL, -- categoría (Grande, Mediano, etc.)
    description TEXT NOT NULL,
    characteristics TEXT NOT NULL,
    image TEXT NOT NULL,

    -- Nuevo: rangos numéricos
    length_min_mm INT,
    length_max_mm INT,
    weight_min_g  INT,
    weight_max_g  INT,

    -- Unicidad por nombre científico
    CONSTRAINT birds_name_uk UNIQUE (name),

    -- Integridad de rangos
    CONSTRAINT chk_len_pos   CHECK (length_min_mm IS NULL OR length_min_mm > 0),
    CONSTRAINT chk_len_pos2  CHECK (length_max_mm IS NULL OR length_max_mm > 0),
    CONSTRAINT chk_len_range CHECK (
        (length_min_mm IS NULL AND length_max_mm IS NULL) OR
        (length_min_mm IS NOT NULL AND length_max_mm IS NOT NULL AND length_min_mm <= length_max_mm)
    ),
    CONSTRAINT chk_w_pos     CHECK (weight_min_g IS NULL OR weight_min_g > 0),
    CONSTRAINT chk_w_pos2    CHECK (weight_max_g IS NULL OR weight_max_g > 0),
    CONSTRAINT chk_w_range   CHECK (
        (weight_min_g IS NULL AND weight_max_g IS NULL) OR
        (weight_min_g IS NOT NULL AND weight_max_g IS NOT NULL AND weight_min_g <= weight_max_g)
    )
);

DO $$
BEGIN
    -- Si existía una columna vieja 'migratory_wave_url', la removemos (compat)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'birds' AND column_name = 'migratory_wave_url'
    ) THEN
        EXECUTE 'ALTER TABLE birds DROP COLUMN IF EXISTS migratory_wave_url';
    END IF;
END $$;

-- ====== TABLA USERS ======
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    points INT DEFAULT 0,
    level INT DEFAULT 1,
    level_name TEXT DEFAULT 'Novato'
);

-- ====== TABLA SIGHTINGS ======
CREATE TABLE IF NOT EXISTS sightings (
    sighting_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    latitude    NUMERIC(9,6) NOT NULL,
    longitude   NUMERIC(9,6) NOT NULL,
    location_text TEXT,
    date_time  TIMESTAMP NOT NULL,
    user_id     UUID NOT NULL,
    bird_id     UUID NOT NULL,
    CONSTRAINT chk_sight_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_sight_lon CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT fk_user  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_birdS FOREIGN KEY (bird_id) REFERENCES birds(bird_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sightings_lat_lon ON sightings (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_sightings_datetime ON sightings (date_time);

CREATE TABLE IF NOT EXISTS colors (
    color_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rarities (
    rarity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS bird_color (
    bird_id UUID NOT NULL,
    color_id UUID NOT NULL,
    PRIMARY KEY (bird_id, color_id),
    CONSTRAINT fk_bird FOREIGN KEY (bird_id) REFERENCES birds(bird_id) ON DELETE CASCADE,
    CONSTRAINT fk_color FOREIGN KEY (color_id) REFERENCES colors(color_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bird_rarity (
    bird_id UUID NOT NULL,
    rarity_id UUID NOT NULL,
    PRIMARY KEY (bird_id, rarity_id),
    CONSTRAINT fk_bird_r FOREIGN KEY (bird_id) REFERENCES birds(bird_id) ON DELETE CASCADE,
    CONSTRAINT fk_rarity FOREIGN KEY (rarity_id) REFERENCES rarities(rarity_id) ON DELETE CASCADE
);

-- ====== PROVINCES ======
CREATE TABLE IF NOT EXISTS provinces (
    province_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS missions (
    mission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    type TEXT NOT NULL, -- daily, weekly, unique
    objective JSONB NOT NULL, -- flexible: {"sightings": 5}, {"species": 3}, {"rarity": "Raro"}
    reward_points INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_missions (
    user_mission_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES missions(mission_id) ON DELETE CASCADE,
    progress JSONB DEFAULT '{}'::jsonb,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS achievements (
    achievement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    criteria JSONB NOT NULL, -- {"total_sightings":100}, {"rarity":"Legendario"}
    icon_url TEXT
);

CREATE TABLE IF NOT EXISTS user_achievements (
    user_achievement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES achievements(achievement_id) ON DELETE CASCADE,
    obtained_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS rarity_points (
    rarity_id UUID PRIMARY KEY REFERENCES rarities(rarity_id) ON DELETE CASCADE,
    points INT NOT NULL
);

CREATE TABLE IF NOT EXISTS levels (
    level INT PRIMARY KEY,
    name TEXT NOT NULL,
    xp_required INT NOT NULL
);

INSERT INTO levels (level, name, xp_required) VALUES
(1, 'Novato', 0),
(2, 'Aprendiz', 50),
(3, 'Aventurero', 150),
(4, 'Explorador', 300),
(5, 'Ornitólogo', 500),
(6, 'Maestro Ornitólogo', 800),
(7, 'Legendario', 1200)
ON CONFLICT (level) DO NOTHING;

INSERT INTO missions (name, description, type, objective, reward_points)
VALUES
('Primer Avistamiento del dia', 'Registra tu primer avistamiento del dia', 'daily', '{"sightings":1}', 10),
('Explorador Común', 'Registra 5 aves comunes', 'daily', '{"rarity":"Común","count":5}', 20),
('Cazador de Raros', 'Encuentra 1 ave rara', 'unique', '{"rarity":"Raro","count":1}', 50)
ON CONFLICT DO NOTHING;

INSERT INTO achievements (name, description, criteria, icon_url)
VALUES
('Novato', 'Completa tu primer avistamiento', '{"sightings":1}', '/icons/novato.png'),
('Ornitólogo', 'Registra 50 especies diferentes', '{"unique_species":50}', '/icons/ornitologo.png'),
('Legendario', 'Avista un ave de rareza Legendaria', '{"rarity":"Legendario"}', '/icons/legendario.png'),
('Naturalista', 'Registrá 10 especies diferentes', '{"unique_species":10}', '/icons/naturalista.png'),
('Épico', 'Avistá un ave de rareza Épico', '{"rarity":"Épico"}', '/icons/epico.png'),
('Maestro de Raros', 'Registrá 5 avistamientos de aves raras', '{"rarity":"Raro","count":5}', '/icons/maestro_raros.png'),
('Madrugador', 'Hacé tu primer avistaje del día antes de las 08:00', '{"first_of_day_before_hour":8}', '/icons/madrugador.png'),
('Coleccionista', 'Registrá 100 avistamientos en total', '{"total_sightings":100}', '/icons/coleccionista.png')
ON CONFLICT DO NOTHING;

INSERT INTO rarity_points (rarity_id, points)
SELECT r.rarity_id, CASE r.name
    WHEN 'Común' THEN 5
    WHEN 'Poco común' THEN 10
    WHEN 'Raro' THEN 20
    WHEN 'Épico' THEN 40
    WHEN 'Legendario' THEN 80
END
FROM rarities r
ON CONFLICT (rarity_id) DO NOTHING;

INSERT INTO provinces (name) VALUES
  ('Buenos Aires'),
  ('Ciudad Autónoma de Buenos Aires'),
  ('Catamarca'),
  ('Chaco'),
  ('Chubut'),
  ('Córdoba'),
  ('Corrientes'),
  ('Entre Ríos'),
  ('Formosa'),
  ('Jujuy'),
  ('La Pampa'),
  ('La Rioja'),
  ('Mendoza'),
  ('Misiones'),
  ('Neuquén'),
  ('Río Negro'),
  ('Salta'),
  ('San Juan'),
  ('San Luis'),
  ('Santa Cruz'),
  ('Santa Fe'),
  ('Santiago del Estero'),
  ('Tierra del Fuego'),
  ('Tucumán')
ON CONFLICT (name) DO NOTHING;

-- ====== MIGRATORY WAVES (mes/provincia por ave) ======
CREATE TABLE IF NOT EXISTS migratory_waves (
    bird_id     UUID     NOT NULL,
    month       SMALLINT NOT NULL CHECK (month BETWEEN 1 AND 12),
    province_id UUID     NOT NULL,

    CONSTRAINT migratory_waves_pk PRIMARY KEY (bird_id, month, province_id),
    CONSTRAINT fk_wave_bird     FOREIGN KEY (bird_id)     REFERENCES birds(bird_id)     ON DELETE CASCADE,
    CONSTRAINT fk_wave_province FOREIGN KEY (province_id) REFERENCES provinces(province_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_wave_bird_month   ON migratory_waves (bird_id, month);
CREATE INDEX IF NOT EXISTS idx_wave_month        ON migratory_waves (month);
CREATE INDEX IF NOT EXISTS idx_wave_province     ON migratory_waves (province_id);

-- ====== SEED: RAREZAS ======
INSERT INTO rarities (rarity_id, name) VALUES
  (gen_random_uuid(), 'Común'),
  (gen_random_uuid(), 'Poco común'),
  (gen_random_uuid(), 'Raro'),
  (gen_random_uuid(), 'Épico'),
  (gen_random_uuid(), 'Legendario')
ON CONFLICT (name) DO NOTHING;

-- ====== SEED: COLORES ======
INSERT INTO colors (color_id, name) VALUES
  (gen_random_uuid(), 'Marrón'),
  (gen_random_uuid(), 'Canela'),
  (gen_random_uuid(), 'Ocre'),
  (gen_random_uuid(), 'Pardo'),
  (gen_random_uuid(), 'Blanco'),
  (gen_random_uuid(), 'Negro'),
  (gen_random_uuid(), 'Gris'),
  (gen_random_uuid(), 'Gris azulado'),
  (gen_random_uuid(), 'Rojo'),
  (gen_random_uuid(), 'Anaranjado'),
  (gen_random_uuid(), 'Amarillo'),
  (gen_random_uuid(), 'Celeste'),
  (gen_random_uuid(), 'Azul'),
  (gen_random_uuid(), 'Rosado'),
  (gen_random_uuid(), 'Blanquecino')
ON CONFLICT (name) DO NOTHING;

-- ====== SEED: USERS ======
INSERT INTO users (user_id, username, password, email) VALUES
  (gen_random_uuid(), 'lucas', 'pass123', 'lucas@example.com'),
  (gen_random_uuid(), 'maria', 'pass123', 'maria@example.com'),
  (gen_random_uuid(), 'juan', 'pass123', 'juan@example.com'),
  (gen_random_uuid(), 'sofia', 'pass123', 'sofia@example.com'),
  (gen_random_uuid(), 'martin', 'pass123', 'martin@example.com')
ON CONFLICT (email) DO NOTHING;

-- ====== SEED: AVES (con rangos numéricos) ======
INSERT INTO birds (bird_id, name, common_name, size, description, characteristics, image,
                   length_min_mm, length_max_mm, weight_min_g, weight_max_g) VALUES
  (gen_random_uuid(), 'Furnarius rufus', 'Hornero', 'Mediano',
   'Ave emblemática que construye nidos de barro con forma de horno.',
   'Nido de barro; territorial; frecuente en ciudades y áreas rurales.',
   'https://example.com/img/furnarius_rufus.jpg', 180, 200, 45, 56),

  (gen_random_uuid(), 'Gubernatrix cristata', 'Cardenal amarillo', 'Mediano',
   'Passeriforme amarillo con máscara negra y cresta conspicua.',
   'Canto potente; matorrales y bosques abiertos; muy presionado por captura.',
   'https://example.com/img/gubernatrix_cristata.jpg', 200, 230, 40, 65),

  (gen_random_uuid(), 'Paroaria coronata', 'Cardenal común', 'Mediano',
   'Cresta roja, dorso gris, partes inferiores blancas.',
   'Común en parques y jardines; gregario; omnívoro.',
   'https://example.com/img/paroaria_coronata.jpg', 180, 200, 35, 45),

  (gen_random_uuid(), 'Chauna torquata', 'Chajá', 'Grande',
   'Ave grande de humedales con potente vocalización y espolones alares.',
   'Parejas estables; vuela bien; frecuente en lagunas.',
   'https://example.com/img/chauna_torquata.jpg', 800, 950, 3000, 5000),

  (gen_random_uuid(), 'Rhea americana', 'Ñandú grande', 'Muy grande',
   'Ratite corredora de pastizales abiertos.',
   'Gregario; gran corredor; nidos comunales.',
   'https://example.com/img/rhea_americana.jpg', 1300, 1700, 20000, 40000),

  (gen_random_uuid(), 'Rhea pennata', 'Ñandú petiso', 'Muy grande',
   'Ratite patagónica, más pequeño y críptico que R. americana.',
   'Adaptado a ambientes fríos y ventosos; baja densidad.',
   'https://example.com/img/rhea_pennata.jpg', 900, 1000, 15000, 28000),

  (gen_random_uuid(), 'Phoenicopterus chilensis', 'Flamenco austral', 'Grande',
   'Flamenco rosado pálido; filtra alimento en aguas someras.',
   'Colonial; movimientos locales amplios; sensible a disturbios.',
   'https://example.com/img/phoenicopterus_chilensis.jpg', 1000, 1200, 2000, 4000),

  (gen_random_uuid(), 'Ramphastos toco', 'Tucán toco', 'Grande',
   'Mayor de los tucanes, con gran pico anaranjado.',
   'Principalmente frugívoro; conspicuo; usa cavidades para nidificar.',
   'https://example.com/img/ramphastos_toco.jpg', 550, 650, 500, 860),

  (gen_random_uuid(), 'Turdus rufiventris', 'Zorzal colorado', 'Mediano',
   'Zorzal de vientre rufo y dorso pardo; canto melodioso.',
   'Común en jardines; dieta variada; anida en arbustos.',
   'https://example.com/img/turdus_rufiventris.jpg', 230, 250, 60, 75),

  (gen_random_uuid(), 'Cyanocompsa brissonii', 'Reinamora grande', 'Mediano',
   'Macho azul intenso con tonos negros; robusto.',
   'Consume semillas y frutos; discreto en matorrales.',
   'https://example.com/img/cyanocompsa_brissonii.jpg', 160, 190, 25, 40),

  (gen_random_uuid(), 'Cygnus melancoryphus', 'Cisne de cuello negro', 'Grande',
   'Cuerpo blanco y cuello negro; lagunas y estuarios.',
   'Fidelidad de pareja; nidifica en juncales flotantes.',
   'https://example.com/img/cygnus_melancoryphus.jpg', 1020, 1240, 3000, 6000),

  (gen_random_uuid(),'Desconocida','Ave no identificada','N/A',
  'Esta entrada representa aves que no pudieron ser identificadas por el modelo.',
  'Sin características registradas.','https://example.com/img/desconocida.jpg',
  NULL,NULL,NULL,NULL),

  (gen_random_uuid(), 'Vanellus chilensis', 'Tero', 'Mediano',
   'Ave de pastizal muy vocal; antifaz negro característico.',
   'Defiende el nido activamente; común en praderas y parques.',
   'https://example.com/img/vanellus_chilensis.jpg', 320, 380, 190, 400),

  (gen_random_uuid(), 'Buteogallus coronatus', 'Águila coronada', 'Grande',
   'Rapaz grande de zonas áridas; penacho corto.',
   'Muy baja densidad; amenazas por pérdida de hábitat y persecución.',
   'https://example.com/img/buteogallus_coronatus.jpg', 730, 850, 2300, 3000),

  (gen_random_uuid(), 'Cathartes aura', 'Jote cabeza colorada', 'Grande',
   'Buitre planeador de cabeza roja desnuda.',
   'Carroñero; excelente olfato; frecuente en campo abierto.',
   'https://example.com/img/cathartes_aura.jpg', 640, 810, 800, 2400),

  (gen_random_uuid(), 'Pipraeidea bonariensis', 'Frutero azul', 'Mediano',
   'Macho azul oscuro con vientre amarillo; hembra verdosa.',
   'Consume frutos y artrópodos; visita arboledas.',
   'https://example.com/img/pipraeidea_bonariensis.jpg', 140, 160, 20, 35),

  (gen_random_uuid(), 'Asio clamator', 'Lechuzón orejudo', 'Grande',
   'Búho con penachos conspicuos; plumaje críptico.',
   'Crepuscular/nocturno; bordes de bosque y pastizales.',
   'https://example.com/img/asio_clamator.jpg', 300, 380, 320, 700),

  (gen_random_uuid(), 'Thraupis sayaca', 'Celestino', 'Mediano',
   'Tangara celeste-grisácea; común en el NE de Sudamérica.',
   'Frugívoro; frecuenta jardines y arboledas.',
   'https://example.com/img/thraupis_sayaca.jpg', 160, 180, 25, 40)
ON CONFLICT (name) DO NOTHING;

-- ====== ASOCIACIONES: BIRD -> RARITY ======
INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Furnarius rufus' AND r.name = 'Común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Paroaria coronata' AND r.name = 'Común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Vanellus chilensis' AND r.name = 'Común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Turdus rufiventris' AND r.name = 'Común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Cathartes aura' AND r.name = 'Común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Chauna torquata' AND r.name = 'Poco común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Phoenicopterus chilensis' AND r.name = 'Poco común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Thraupis sayaca' AND r.name = 'Poco común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Pipraeidea bonariensis' AND r.name = 'Poco común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Cygnus melancoryphus' AND r.name = 'Poco común'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Rhea americana' AND r.name = 'Raro'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Rhea pennata' AND r.name = 'Raro'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Ramphastos toco' AND r.name = 'Raro'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Cyanocompsa brissonii' AND r.name = 'Raro'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Buteogallus coronatus' AND r.name = 'Épico'
ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Gubernatrix cristata' AND r.name = 'Legendario'
ON CONFLICT DO NOTHING;

-- ====== ASOCIACIONES: BIRD -> COLORS ======
INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Furnarius rufus' AND c.name IN ('Marrón','Canela','Ocre')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Gubernatrix cristata' AND c.name IN ('Amarillo','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Paroaria coronata' AND c.name IN ('Rojo','Gris','Blanco')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Chauna torquata' AND c.name IN ('Gris','Blanco','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Rhea americana' AND c.name IN ('Pardo','Gris')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Rhea pennata' AND c.name IN ('Pardo','Gris','Blanquecino')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Phoenicopterus chilensis' AND c.name IN ('Rosado','Blanco','Gris')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Ramphastos toco' AND c.name IN ('Negro','Blanco','Anaranjado')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Turdus rufiventris' AND c.name IN ('Pardo','Marrón','Anaranjado')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Cyanocompsa brissonii' AND c.name IN ('Azul','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Cygnus melancoryphus' AND c.name IN ('Blanco','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Vanellus chilensis' AND c.name IN ('Gris','Blanco','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Buteogallus coronatus' AND c.name IN ('Pardo','Gris','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Cathartes aura' AND c.name IN ('Negro','Pardo','Rojo')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Pipraeidea bonariensis' AND c.name IN ('Azul','Amarillo','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Asio clamator' AND c.name IN ('Pardo','Blanco','Negro')
ON CONFLICT DO NOTHING;

INSERT INTO bird_color (bird_id, color_id)
SELECT b.bird_id, c.color_id FROM birds b, colors c
WHERE b.name = 'Thraupis sayaca' AND c.name IN ('Celeste','Gris')
ON CONFLICT DO NOTHING;

-- ====== SEED: SIGHTINGS ======
INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -34.594900, -58.375700, 'Plaza San Martín, Buenos Aires (CABA)', NOW() - interval '10 days',
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'lucas@example.com' AND b.name = 'Furnarius rufus'
ON CONFLICT DO NOTHING;

INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -34.617000, -58.360000, 'Reserva Ecológica Costanera Sur, CABA', NOW() - interval '7 days',
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'maria@example.com' AND b.name = 'Phoenicopterus chilensis'
ON CONFLICT DO NOTHING;

INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -35.577000, -58.016000, 'Laguna de Chascomús, Buenos Aires', NOW() - interval '5 days',
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'juan@example.com' AND b.name = 'Cygnus melancoryphus'
ON CONFLICT DO NOTHING;

INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -32.890000, -68.865000, 'Parque General San Martín, Mendoza', NOW() - interval '3 days',
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'sofia@example.com' AND b.name = 'Vanellus chilensis'
ON CONFLICT DO NOTHING;

INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -35.240000, -57.329000, 'Reserva El Destino, Magdalena (Bs As)', NOW() - interval '1 day',
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'martin@example.com' AND b.name = 'Buteogallus coronatus'
ON CONFLICT DO NOTHING;

INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -34.587300, -58.416500, 'Jardín Botánico Carlos Thays, CABA', NOW() - interval '2 days',
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'lucas@example.com' AND b.name = 'Paroaria coronata'
ON CONFLICT DO NOTHING;

INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -34.608000, -58.371000, 'Plaza de Mayo, CABA', NOW(),
       u.user_id, b.bird_id
FROM users u, birds b
WHERE u.email = 'lucas@example.com' AND b.name = 'Turdus rufiventris'
ON CONFLICT DO NOTHING;

-- Completa rarezas faltantes con "Común" por default
INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id
FROM birds b
JOIN rarities r ON r.name = 'Común'
LEFT JOIN bird_rarity br ON br.bird_id = b.bird_id
WHERE br.bird_id IS NULL
ON CONFLICT DO NOTHING;

-- ====== SEED: MIGRATORY WAVES (EJEMPLOS) ======
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Paroaria coronata'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires'),
     p_ju AS (SELECT province_id FROM provinces WHERE name = 'Jujuy'),
     p_rn AS (SELECT province_id FROM provinces WHERE name = 'Río Negro')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 3, p_ba.province_id FROM bird b, p_ba
UNION ALL
SELECT b.bird_id, 4, p_ju.province_id FROM bird b, p_ju
UNION ALL
SELECT b.bird_id, 5, p_rn.province_id FROM bird b, p_rn
ON CONFLICT DO NOTHING;

WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Ramphastos toco'),
     p_mi AS (SELECT province_id FROM provinces WHERE name = 'Misiones'),
     p_co AS (SELECT province_id FROM provinces WHERE name = 'Corrientes')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 3, p_mi.province_id FROM bird b, p_mi
UNION ALL
SELECT b.bird_id, 3, p_co.province_id FROM bird b, p_co
ON CONFLICT DO NOTHING;



-- =========================================
-- SIGHTINGS de prueba para job de limpieza
-- =========================================

-- ---------- Cluster A: Turdus rufiventris (Zorzal colorado) cerca del Obelisco ----------
INSERT INTO sightings (sighting_id, latitude, longitude, location_text, date_time, user_id, bird_id)
SELECT gen_random_uuid(), -34.603700, -58.381600, 'Obelisco CABA', NOW() - interval '9 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'lucas@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603900, -58.381500, 'Obelisco CABA', NOW() - interval '8 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'maria@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603400, -58.381700, 'Obelisco CABA', NOW() - interval '7 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'juan@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603800, -58.381800, 'Obelisco CABA', NOW() - interval '6 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'sofia@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603500, -58.381400, 'Obelisco CABA', NOW() - interval '5 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'martin@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603650, -58.381550, 'Obelisco CABA', NOW() - interval '4 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'lucas@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603720, -58.381480, 'Obelisco CABA', NOW() - interval '3 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'maria@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603780, -58.381620, 'Obelisco CABA', NOW() - interval '2 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'juan@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.603830, -58.381560, 'Obelisco CABA', NOW() - interval '1 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'sofia@example.com' AND b.name = 'Turdus rufiventris' ON CONFLICT DO NOTHING;

-- Resultado esperado del job en este cluster: conservar 5 más recientes, borrar el resto.

-- ---------- Cluster B: Vanellus chilensis (Tero) cerca de Parque Centenario ----------
INSERT INTO sightings SELECT gen_random_uuid(), -34.606500, -58.436000, 'Parque Centenario', NOW() - interval '12 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'lucas@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.606420, -58.435900, 'Parque Centenario', NOW() - interval '11 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'maria@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.606600, -58.436120, 'Parque Centenario', NOW() - interval '10 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'juan@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.606520, -58.436180, 'Parque Centenario', NOW() - interval '9 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'sofia@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.606480, -58.435960, 'Parque Centenario', NOW() - interval '8 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'martin@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.606550, -58.436040, 'Parque Centenario', NOW() - interval '7 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'lucas@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.606580, -58.436090, 'Parque Centenario', NOW() - interval '6 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'maria@example.com' AND b.name = 'Vanellus chilensis' ON CONFLICT DO NOTHING;

-- ---------- Cluster C: Paroaria coronata (Cardenal común) cerca del Jardín Botánico ----------
INSERT INTO sightings SELECT gen_random_uuid(), -34.587300, -58.416500, 'Jardín Botánico', NOW() - interval '20 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'juan@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587360, -58.416420, 'Jardín Botánico', NOW() - interval '18 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'sofia@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587240, -58.416560, 'Jardín Botánico', NOW() - interval '16 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'martin@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587330, -58.416470, 'Jardín Botánico', NOW() - interval '14 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'lucas@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587280, -58.416520, 'Jardín Botánico', NOW() - interval '12 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'maria@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587310, -58.416450, 'Jardín Botánico', NOW() - interval '10 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'juan@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587350, -58.416530, 'Jardín Botánico', NOW() - interval '8 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'sofia@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;

INSERT INTO sightings SELECT gen_random_uuid(), -34.587270, -58.416490, 'Jardín Botánico', NOW() - interval '6 hours', u.user_id, b.bird_id
FROM users u, birds b WHERE u.email = 'martin@example.com' AND b.name = 'Paroaria coronata' ON CONFLICT DO NOTHING;
