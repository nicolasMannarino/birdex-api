-- 001_all.sql  (PostgreSQL)

-- ====== EXTENSION ======
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ====== ESQUEMA ======

-- ---------- BIRDS ----------
CREATE TABLE IF NOT EXISTS birds (
    bird_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    common_name TEXT NOT NULL,
    size TEXT NOT NULL, -- categoría (Grande, Mediano, etc.)
    description TEXT NOT NULL,
    characteristics TEXT NOT NULL,
    image TEXT NOT NULL,

    -- Rangos numéricos
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
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'birds' AND column_name = 'migratory_wave_url'
    ) THEN
        EXECUTE 'ALTER TABLE birds DROP COLUMN IF EXISTS migratory_wave_url';
    END IF;
END $$;


CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER',
    email TEXT NOT NULL UNIQUE,
    points INT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    level_name TEXT NOT NULL DEFAULT 'Novato',
    CONSTRAINT users_username_uk UNIQUE (username)
);

ALTER TABLE users ADD COLUMN profile_photo_key varchar(512);

-- Bloque de "autocuración" para instalaciones previas
DO $$
BEGIN
    -- Si aún existe 'password' (texto plano), renombrar a 'password_hash'
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='password'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='password_hash'
    ) THEN
        EXECUTE 'ALTER TABLE users RENAME COLUMN password TO password_hash';
    END IF;

    -- Agregar 'password_hash' si no existe (para DB muy vieja)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='password_hash'
    ) THEN
        EXECUTE 'ALTER TABLE users ADD COLUMN password_hash TEXT';
        EXECUTE 'UPDATE users SET password_hash = '''' WHERE password_hash IS NULL';
        EXECUTE 'ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL';
    END IF;

    -- Agregar 'role' con default USER si no existe
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='users' AND column_name='role'
    ) THEN
        EXECUTE 'ALTER TABLE users ADD COLUMN role TEXT';
        EXECUTE 'UPDATE users SET role = ''USER'' WHERE role IS NULL';
        EXECUTE 'ALTER TABLE users ALTER COLUMN role SET NOT NULL';
        EXECUTE 'ALTER TABLE users ALTER COLUMN role SET DEFAULT ''USER''';
    END IF;

    -- Agregar constraint de unicidad por username si no existe
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_username_uk'
    ) THEN
        -- Eliminar duplicados en username si los hubiera (conserva el primero)
        -- Nota: sólo si querés "forzar", de lo contrario quitá este bloque.
        -- Aquí asumimos que no hay duplicados o que no importa conservar seeds.
        EXECUTE 'ALTER TABLE users ADD CONSTRAINT users_username_uk UNIQUE (username)';
    END IF;
END $$;

-- Hash automático en seed vieja: si hay filas que NO están en BCrypt, las encripta.
-- Detecta por prefijo distinto de $2a/$2b/$2y.
UPDATE users
SET password_hash = crypt(password_hash, gen_salt('bf'))
WHERE password_hash IS NOT NULL
  AND password_hash !~ '^\$2[aby]\$';

-- ---------- SIGHTINGS ----------
CREATE TABLE IF NOT EXISTS sightings (
    sighting_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    latitude    NUMERIC(9,6),
    longitude   NUMERIC(9,6),
    location_text TEXT,
    date_time  TIMESTAMP NOT NULL,
    user_id     UUID NOT NULL,
    bird_id     UUID,
    deleted     BOOLEAN    NOT NULL DEFAULT FALSE,
    state       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_sight_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_sight_lon CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_sight_state CHECK (state IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT fk_user  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_birdS FOREIGN KEY (bird_id) REFERENCES birds(bird_id) ON DELETE CASCADE
);

ALTER TABLE sightings
    ALTER COLUMN bird_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sightings_lat_lon ON sightings (latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_sightings_datetime ON sightings (date_time);

-- ---------- REPORTS ----------
CREATE TABLE IF NOT EXISTS reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sighting_id         UUID        NOT NULL,
    reported_by_user_id UUID        NOT NULL,
    reported_at         TIMESTAMP   NOT NULL DEFAULT NOW(),
    read_at             TIMESTAMP,
    status              TEXT        NOT NULL DEFAULT 'pending',
    description         TEXT,

    CONSTRAINT fk_reports_sighting
        FOREIGN KEY (sighting_id) REFERENCES sightings(sighting_id) ON DELETE CASCADE,

    CONSTRAINT fk_reports_user
        FOREIGN KEY (reported_by_user_id) REFERENCES users(user_id) ON DELETE RESTRICT,

    CONSTRAINT chk_reports_status
        CHECK (status IN ('pending','in_review','resolved','dismissed'))
);

CREATE INDEX IF NOT EXISTS idx_reports_sighting      ON reports (sighting_id);
CREATE INDEX IF NOT EXISTS idx_reports_status        ON reports (status);
CREATE INDEX IF NOT EXISTS idx_reports_reported_at   ON reports (reported_at);
CREATE INDEX IF NOT EXISTS idx_reports_reported_by   ON reports (reported_by_user_id);
-- ---------- COLORS / RARITIES / PUENTES ----------
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

-- ---------- PROVINCES ----------
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
    claimed BOOLEAN DEFAULT FALSE,
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
    progress JSONB DEFAULT '{}'::jsonb,
    claimed BOOLEAN DEFAULT FALSE,
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

-- MISSIONS
INSERT INTO missions (name, description, type, objective, reward_points)
VALUES
('Primer Avistamiento del dia', 'Registra tu primer avistamiento del dia', 'daily', '{"sightings":1}', 10),
('Explorador Común', 'Registra 5 aves comunes', 'daily', '{"rarity":"Común","count":5}', 20),
('Cazador de Raros', 'Encuentra 1 ave rara', 'unique', '{"rarity":"Raro","count":1}', 50),
('Observador Persistente', 'Realiza 3 avistamientos en un mismo día', 'daily', '{"sightings":3}', 25),
('Fanático del Raro', 'Encuentra 2 aves raras en la semana', 'weekly', '{"rarity":"Raro","count":2}', 70),
('Semana Productiva', 'Realiza 10 avistamientos en la semana', 'weekly', '{"sightings":10}', 60),
('Maestro del Vuelo', 'Registra 50 avistamientos totales', 'unique', '{"sightings":50}', 200),
('Leyenda del Avistamiento', 'Registra 100 avistamientos totales', 'unique', '{"sightings":100}', 300),
('Cazador de Legendarios', 'Encuentra un ave de rareza Legendaria', 'unique', '{"rarity":"Legendario","count":1}', 500)
ON CONFLICT DO NOTHING;

-- ACHIEVEMENTS
INSERT INTO achievements (name, description, criteria, icon_url)
VALUES
('Novato', 'Completa tu primer avistamiento', '{"sightings":1}', '/icons/novato.png'),
('Ornitólogo', 'Registra 50 especies diferentes', '{"unique_species":50}', '/icons/ornitologo.png'),
('Legendario', 'Avista un ave de rareza Legendaria', '{"rarity":"Legendario"}', '/icons/legendario.png'),
('Naturalista', 'Registrá 10 especies diferentes', '{"unique_species":10}', '/icons/naturalista.png'),
('Épico', 'Avistá un ave de rareza Épico', '{"rarity":"Épico"}', '/icons/epico.png'),
('Maestro de Raros', 'Registrá 5 avistamientos de aves raras', '{"rarity":"Raro","count":5}', '/icons/maestro_raros.png'),
('Coleccionista', 'Registrá 100 avistamientos en total', '{"total_sightings":100}', '/icons/coleccionista.png')
ON CONFLICT DO NOTHING;



INSERT INTO rarities (rarity_id, name) VALUES
  (gen_random_uuid(), 'Común'),
  (gen_random_uuid(), 'Poco común'),
  (gen_random_uuid(), 'Raro'),
  (gen_random_uuid(), 'Épico'),
  (gen_random_uuid(), 'Legendario')
ON CONFLICT (name) DO NOTHING;


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


-- ====== MIGRATORY WAVES ======
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

-- ====== SEED: PROVINCES ======

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

-- ====== SEED: AVES (completo) ======


-- USERS (hash BCrypt con pgcrypto + role)
INSERT INTO users (user_id, username, password_hash, role, email)
VALUES
  (gen_random_uuid(), 'lucas',  crypt('pass123', gen_salt('bf')), 'USER', 'lucas@example.com'),
  (gen_random_uuid(), 'maria',  crypt('pass123', gen_salt('bf')), 'USER',  'maria@example.com'),
  (gen_random_uuid(), 'juan',   crypt('pass123', gen_salt('bf')), 'USER',  'juan@example.com'),
  (gen_random_uuid(), 'sofia',  crypt('pass123', gen_salt('bf')), 'USER',  'sofia@example.com'),
  (gen_random_uuid(), 'martin', crypt('pass123', gen_salt('bf')), 'ADMIN',  'martin@example.com')
ON CONFLICT (email) DO NOTHING;



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



-- BIRDS (con rangos numéricos)
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

-- Asociaciones BIRD -> RARITY
INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Furnarius rufus' AND r.name = 'Común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Paroaria coronata' AND r.name = 'Común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Vanellus chilensis' AND r.name = 'Común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Turdus rufiventris' AND r.name = 'Común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Cathartes aura' AND r.name = 'Común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Chauna torquata' AND r.name = 'Poco común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Phoenicopterus chilensis' AND r.name = 'Poco común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Thraupis sayaca' AND r.name = 'Poco común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Pipraeidea bonariensis' AND r.name = 'Poco común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Cygnus melancoryphus' AND r.name = 'Poco común' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Rhea americana' AND r.name = 'Raro' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Rhea pennata' AND r.name = 'Raro' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Ramphastos toco' AND r.name = 'Raro' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Cyanocompsa brissonii' AND r.name = 'Raro' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Buteogallus coronatus' AND r.name = 'Épico' ON CONFLICT DO NOTHING;

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id FROM birds b, rarities r
WHERE b.name = 'Gubernatrix cristata' AND r.name = 'Legendario' ON CONFLICT DO NOTHING;

-- Asociaciones BIRD -> COLORS
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

-- ====== SEED: SIGHTINGS (igual a tu versión) ======
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

-- Completa rarezas faltantes con "Común"

INSERT INTO bird_rarity (bird_id, rarity_id)
SELECT b.bird_id, r.rarity_id
FROM birds b
JOIN rarities r ON r.name = 'Común'
LEFT JOIN bird_rarity br ON br.bird_id = b.bird_id
WHERE br.bird_id IS NULL
ON CONFLICT DO NOTHING;

-- Seeds de provincias
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

-- Waves: ejemplos
-- 1. Cóndor Andino (Vultur gryphus)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Vultur gryphus'),
     p_ju AS (SELECT province_id FROM provinces WHERE name = 'Jujuy'),
     p_sl AS (SELECT province_id FROM provinces WHERE name = 'Salta'),
     p_ne AS (SELECT province_id FROM provinces WHERE name = 'Neuquén')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 5, p_ju.province_id FROM bird b, p_ju
UNION ALL
SELECT b.bird_id, 9, p_sl.province_id FROM bird b, p_sl
UNION ALL
SELECT b.bird_id, 11, p_ne.province_id FROM bird b, p_ne
ON CONFLICT DO NOTHING;

-- 2. Hornero (Furnarius rufus) - residente, leves movimientos locales
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Furnarius rufus'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires'),
     p_sf AS (SELECT province_id FROM provinces WHERE name = 'Santa Fe')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 10, p_ba.province_id FROM bird b, p_ba
UNION ALL
SELECT b.bird_id, 11, p_sf.province_id FROM bird b, p_sf
ON CONFLICT DO NOTHING;

-- 3. Cardenal Amarillo (Gubernatrix cristata)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Gubernatrix cristata'),
     p_sf AS (SELECT province_id FROM provinces WHERE name = 'Santa Fe'),
     p_er AS (SELECT province_id FROM provinces WHERE name = 'Entre Ríos'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 9, p_sf.province_id FROM bird b, p_sf
UNION ALL
SELECT b.bird_id, 10, p_er.province_id FROM bird b, p_er
UNION ALL
SELECT b.bird_id, 11, p_ba.province_id FROM bird b, p_ba
ON CONFLICT DO NOTHING;

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

-- 4. Chajá (Chauna torquata)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Chauna torquata'),
     p_co AS (SELECT province_id FROM provinces WHERE name = 'Corrientes'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 8, p_co.province_id FROM bird b, p_co
UNION ALL
SELECT b.bird_id, 10, p_ba.province_id FROM bird b, p_ba
ON CONFLICT DO NOTHING;

-- 5. Ñandú (Rhea americana)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Rhea americana'),
     p_cn AS (SELECT province_id FROM provinces WHERE name = 'Chaco'),
     p_lp AS (SELECT province_id FROM provinces WHERE name = 'La Pampa')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 4, p_cn.province_id FROM bird b, p_cn
UNION ALL
SELECT b.bird_id, 9, p_lp.province_id FROM bird b, p_lp
ON CONFLICT DO NOTHING;

-- 6. Flamenco Austral (Phoenicopterus chilensis)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Phoenicopterus chilensis'),
     p_sa AS (SELECT province_id FROM provinces WHERE name = 'Salta'),
     p_md AS (SELECT province_id FROM provinces WHERE name = 'Mendoza'),
     p_ch AS (SELECT province_id FROM provinces WHERE name = 'Chubut')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 3, p_sa.province_id FROM bird b, p_sa
UNION ALL
SELECT b.bird_id, 5, p_md.province_id FROM bird b, p_md
UNION ALL
SELECT b.bird_id, 9, p_ch.province_id FROM bird b, p_ch
ON CONFLICT DO NOTHING;

WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Ramphastos toco'),
     p_mi AS (SELECT province_id FROM provinces WHERE name = 'Misiones'),
     p_co AS (SELECT province_id FROM provinces WHERE name = 'Corrientes')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 3, p_mi.province_id FROM bird b, p_mi
UNION ALL
SELECT b.bird_id, 3, p_co.province_id FROM bird b, p_co
ON CONFLICT DO NOTHING;

-- 7. Zorzal Colorado (Turdus rufiventris)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Turdus rufiventris'),
     p_mi AS (SELECT province_id FROM provinces WHERE name = 'Misiones'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 9, p_mi.province_id FROM bird b, p_mi
UNION ALL
SELECT b.bird_id, 3, p_ba.province_id FROM bird b, p_ba
ON CONFLICT DO NOTHING;

-- 8. Azulito (Cyanocompsa brissonii)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Cyanocompsa brissonii'),
     p_mi AS (SELECT province_id FROM provinces WHERE name = 'Misiones'),
     p_er AS (SELECT province_id FROM provinces WHERE name = 'Entre Ríos')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 10, p_mi.province_id FROM bird b, p_mi
UNION ALL
SELECT b.bird_id, 11, p_er.province_id FROM bird b, p_er
ON CONFLICT DO NOTHING;

-- 9. Cisne Cuello Negro (Cygnus melancoryphus)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Cygnus melancoryphus'),
     p_rn AS (SELECT province_id FROM provinces WHERE name = 'Río Negro'),
     p_tf AS (SELECT province_id FROM provinces WHERE name = 'Tierra del Fuego'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 2, p_tf.province_id FROM bird b, p_tf
UNION ALL
SELECT b.bird_id, 5, p_rn.province_id FROM bird b, p_rn
UNION ALL
SELECT b.bird_id, 9, p_ba.province_id FROM bird b, p_ba
ON CONFLICT DO NOTHING;

-- 10. Tero (Vanellus chilensis)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Vanellus chilensis'),
     p_co AS (SELECT province_id FROM provinces WHERE name = 'Corrientes'),
     p_ba AS (SELECT province_id FROM provinces WHERE name = 'Buenos Aires')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 8, p_co.province_id FROM bird b, p_co
UNION ALL
SELECT b.bird_id, 9, p_ba.province_id FROM bird b, p_ba
ON CONFLICT DO NOTHING;

-- 11. Águila Coronada (Buteogallus coronatus)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Buteogallus coronatus'),
     p_sl AS (SELECT province_id FROM provinces WHERE name = 'San Luis'),
     p_lar AS (SELECT province_id FROM provinces WHERE name = 'La Rioja'),
     p_ch AS (SELECT province_id FROM provinces WHERE name = 'Chaco')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 4, p_ch.province_id FROM bird b, p_ch
UNION ALL
SELECT b.bird_id, 7, p_lar.province_id FROM bird b, p_lar
UNION ALL
SELECT b.bird_id, 10, p_sl.province_id FROM bird b, p_sl
ON CONFLICT DO NOTHING;

-- 12. Jote Cabeza Roja (Cathartes aura)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Cathartes aura'),
     p_co AS (SELECT province_id FROM provinces WHERE name = 'Córdoba'),
     p_rn AS (SELECT province_id FROM provinces WHERE name = 'Río Negro')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 3, p_co.province_id FROM bird b, p_co
UNION ALL
SELECT b.bird_id, 9, p_rn.province_id FROM bird b, p_rn
ON CONFLICT DO NOTHING;

-- 13. Frutero Azul (Pipraeidea bonariensis)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Pipraeidea bonariensis'),
     p_mi AS (SELECT province_id FROM provinces WHERE name = 'Misiones'),
     p_md AS (SELECT province_id FROM provinces WHERE name = 'Mendoza')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 9, p_mi.province_id FROM bird b, p_mi
UNION ALL
SELECT b.bird_id, 3, p_md.province_id FROM bird b, p_md
ON CONFLICT DO NOTHING;

-- 14. Lechuzón Orejudo (Asio clamator)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Asio clamator'),
     p_er AS (SELECT province_id FROM provinces WHERE name = 'Entre Ríos'),
     p_sf AS (SELECT province_id FROM provinces WHERE name = 'Santa Fe')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 8, p_er.province_id FROM bird b, p_er
UNION ALL
SELECT b.bird_id, 10, p_sf.province_id FROM bird b, p_sf
ON CONFLICT DO NOTHING;

-- 15. Celestino (Thraupis sayaca)
WITH bird AS (SELECT bird_id FROM birds WHERE name = 'Thraupis sayaca'),
     p_mi AS (SELECT province_id FROM provinces WHERE name = 'Misiones'),
     p_co AS (SELECT province_id FROM provinces WHERE name = 'Corrientes'),
     p_er AS (SELECT province_id FROM provinces WHERE name = 'Entre Ríos')
INSERT INTO migratory_waves (bird_id, month, province_id)
SELECT b.bird_id, 9, p_mi.province_id FROM bird b, p_mi
UNION ALL
SELECT b.bird_id, 10, p_co.province_id FROM bird b, p_co
UNION ALL
SELECT b.bird_id, 11, p_er.province_id FROM bird b, p_er
ON CONFLICT DO NOTHING;


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

-- ===========================================================
-- NUEVO: ZONAS DE APARICIÓN + RELACIÓN AVE/ZONA
-- ===========================================================
CREATE TABLE IF NOT EXISTS zones (
    zone_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL UNIQUE,
    latitude   NUMERIC(9,6) NOT NULL,
    longitude  NUMERIC(9,6) NOT NULL,
    CONSTRAINT chk_zone_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_zone_lon CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX IF NOT EXISTS idx_zones_name ON zones (name);

CREATE TABLE IF NOT EXISTS bird_zone (
    bird_id UUID NOT NULL,
    zone_id UUID NOT NULL,
    PRIMARY KEY (bird_id, zone_id),
    CONSTRAINT fk_bird_zone_bird FOREIGN KEY (bird_id) REFERENCES birds(bird_id) ON DELETE CASCADE,
    CONSTRAINT fk_bird_zone_zone FOREIGN KEY (zone_id) REFERENCES zones(zone_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_bird_zone_bird ON bird_zone (bird_id);
CREATE INDEX IF NOT EXISTS idx_bird_zone_zone ON bird_zone (zone_id);

-- Seeds de zonas (11 del screenshot)
INSERT INTO zones (name, latitude, longitude) VALUES
  ('Buenos Aires',               -34.921450, -57.954530),
  ('Entre Ríos',                 -31.744560, -60.517000),
  ('Corrientes',                 -27.469170, -58.830000),
  ('Misiones',                   -27.367080, -55.896080),
  ('Chaco',                      -27.451000, -58.986000),
  ('Formosa',                    -26.184890, -58.175410),
  ('Santa Fe',                   -31.633330, -60.700000),
  ('La Pampa',                   -36.620000, -64.290000),
  ('Córdoba',                    -31.416670, -64.183330),
  ('Mendoza',                    -32.889000, -68.845000),
  ('Delta del Paraná',           -34.216000, -58.500000)
ON CONFLICT (name) DO NOTHING;

-- Reseed seguro: borra asociaciones existentes SOLO de estas especies
DELETE FROM bird_zone bz
USING birds b
WHERE bz.bird_id = b.bird_id AND b.name IN (
  'Furnarius rufus','Gubernatrix cristata','Paroaria coronata','Chauna torquata',
  'Rhea americana','Rhea pennata','Phoenicopterus chilensis','Ramphastos toco',
  'Turdus rufiventris','Cyanocompsa brissonii','Cygnus melancoryphus','Vanellus chilensis',
  'Buteogallus coronatus','Cathartes aura','Pipraeidea bonariensis','Asio clamator',
  'Thraupis sayaca','Desconocida'
);

-- 1) Hornero
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','Entre Ríos','Corrientes','Misiones','Chaco','Formosa',
 'Santa Fe','La Pampa','Córdoba','Mendoza','Delta del Paraná')
WHERE b.name = 'Furnarius rufus'
ON CONFLICT DO NOTHING;

-- Cardenal amarillo
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Buenos Aires','La Pampa','Mendoza','Córdoba')
WHERE b.name = 'Gubernatrix cristata'
ON CONFLICT DO NOTHING;

-- Cardenal común
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','Entre Ríos','Santa Fe','Córdoba','Corrientes','Misiones','Chaco','Formosa','Delta del Paraná','Mendoza')
WHERE b.name = 'Paroaria coronata'
ON CONFLICT DO NOTHING;

-- Chajá
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','Entre Ríos','Santa Fe','Corrientes','Chaco','Formosa','Misiones','Córdoba','La Pampa','Delta del Paraná')
WHERE b.name = 'Chauna torquata'
ON CONFLICT DO NOTHING;

-- Ñandú grande
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','La Pampa','Córdoba','Santa Fe','Entre Ríos','Corrientes','Chaco')
WHERE b.name = 'Rhea americana'
ON CONFLICT DO NOTHING;

-- Ñandú petiso
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Mendoza')
WHERE b.name = 'Rhea pennata'
ON CONFLICT DO NOTHING;

-- Flamenco austral
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Mendoza','La Pampa','Buenos Aires','Córdoba')
WHERE b.name = 'Phoenicopterus chilensis'
ON CONFLICT DO NOTHING;

-- Tucán toco
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Misiones','Corrientes','Formosa','Chaco','Santa Fe')
WHERE b.name = 'Ramphastos toco'
ON CONFLICT DO NOTHING;

-- Zorzal colorado
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','Entre Ríos','Santa Fe','Córdoba','Corrientes','Misiones','Chaco','Formosa','Mendoza','Delta del Paraná','La Pampa')
WHERE b.name = 'Turdus rufiventris'
ON CONFLICT DO NOTHING;

-- Reinamora grande
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Santa Fe','Córdoba','Entre Ríos','Corrientes','Misiones','Chaco','Formosa')
WHERE b.name = 'Cyanocompsa brissonii'
ON CONFLICT DO NOTHING;

-- Cisne de cuello negro
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Buenos Aires','La Pampa','Mendoza','Santa Fe')
WHERE b.name = 'Cygnus melancoryphus'
ON CONFLICT DO NOTHING;

-- Tero (todas las zonas definidas)
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
CROSS JOIN zones z
WHERE b.name = 'Vanellus chilensis'
ON CONFLICT DO NOTHING;

-- Águila coronada
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('La Pampa','Mendoza','Córdoba')
WHERE b.name = 'Buteogallus coronatus'
ON CONFLICT DO NOTHING;

-- Jote cabeza colorada (todas las zonas definidas)
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
CROSS JOIN zones z
WHERE b.name = 'Cathartes aura'
ON CONFLICT DO NOTHING;

-- Frutero azul
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','Entre Ríos','Santa Fe','Córdoba','Corrientes','Misiones','Chaco','Formosa','Delta del Paraná')
WHERE b.name = 'Pipraeidea bonariensis'
ON CONFLICT DO NOTHING;

-- Lechuzón orejudo
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN
('Buenos Aires','Entre Ríos','Santa Fe','Córdoba','Corrientes','Misiones','Chaco','Formosa','Delta del Paraná')
WHERE b.name = 'Asio clamator'
ON CONFLICT DO NOTHING;

-- Celestino (principal NE/Litoral)
INSERT INTO bird_zone (bird_id, zone_id)
SELECT b.bird_id, z.zone_id
FROM birds b
JOIN zones z ON z.name IN ('Misiones','Corrientes','Chaco','Formosa','Santa Fe','Entre Ríos')
WHERE b.name = 'Thraupis sayaca'
ON CONFLICT DO NOTHING;
-- 18) Desconocida: sin zonas

-- ====== Vista helper JSON ======
CREATE OR REPLACE VIEW bird_zones_json AS
SELECT
  b.bird_id,
  b.name AS scientific_name,
  b.common_name,
  jsonb_build_object(
    'zonas', COALESCE(jsonb_agg(z.name ORDER BY z.name) FILTER (WHERE z.zone_id IS NOT NULL), '[]'::jsonb)
  ) AS zonas_json
FROM birds b
LEFT JOIN bird_zone bz ON bz.bird_id = b.bird_id
LEFT JOIN zones z      ON z.zone_id = bz.zone_id
GROUP BY b.bird_id, b.name, b.common_name;

-- Ejemplos:
-- SELECT zonas_json FROM bird_zones_json WHERE scientific_name = 'Paroaria coronata';
-- SELECT zonas_json FROM bird_zones_json WHERE bird_id = '<uuid>';

--insercion de misiones por usuario
INSERT INTO user_missions (user_id, mission_id, progress, completed)
SELECT
    u.user_id,
    m.mission_id,
    '{}'::jsonb AS progress,
    FALSE AS completed
FROM users u
CROSS JOIN missions m
ON CONFLICT DO NOTHING;

--insercion de logros por usuario
INSERT INTO user_achievements (user_id, achievement_id, progress, obtained_at)
SELECT
    u.user_id,
    a.achievement_id,
    '{}'::jsonb AS progress,
    NULL AS obtained_at
FROM users u
CROSS JOIN achievements a
ON CONFLICT DO NOTHING;



---avance de logros y misiones para lucas@example.com
-- 1️⃣ Obtener el ID del usuario
DO $$
DECLARE
    v_user_id UUID;
BEGIN
    -- Obtener el ID del usuario
    SELECT user_id INTO v_user_id FROM users WHERE email = 'lucas@example.com';

    IF v_user_id IS NULL THEN
        RAISE NOTICE 'Usuario no encontrado: lucas@example.com';
        RETURN;
    END IF;

    ----------------------------------------------------------------
    -- 🕹️ MISIONES
    ----------------------------------------------------------------

    -- Primer Avistamiento del día → completada y reclamada
    UPDATE user_missions
    SET progress = '{"sightings":1}',
        completed = TRUE,
        claimed = TRUE,
        completed_at = NOW()
    WHERE user_id = v_user_id
      AND mission_id = (SELECT mission_id FROM missions WHERE name = 'Primer Avistamiento del dia');

    -- Explorador Común → progreso parcial
    UPDATE user_missions
    SET progress = '{"rarity":"Común","count":3}',
        completed = FALSE,
        claimed = FALSE,
        completed_at = NULL
    WHERE user_id = v_user_id
      AND mission_id = (SELECT mission_id FROM missions WHERE name = 'Explorador Común');

    -- Maestro del Vuelo → progreso parcial
    UPDATE user_missions
    SET progress = '{"sightings":30}',
        completed = FALSE,
        claimed = FALSE,
        completed_at = NULL
    WHERE user_id = v_user_id
      AND mission_id = (SELECT mission_id FROM missions WHERE name = 'Maestro del Vuelo');

    -- Cazador de Raros → completada, no reclamada
    UPDATE user_missions
    SET progress = '{"rarity":"Raro","count":1}',
        completed = TRUE,
        claimed = FALSE,
        completed_at = NOW()
    WHERE user_id = v_user_id
      AND mission_id = (SELECT mission_id FROM missions WHERE name = 'Cazador de Raros');

    -- Semana Productiva → progreso medio
    UPDATE user_missions
    SET progress = '{"sightings":6}',
        completed = FALSE,
        claimed = FALSE,
        completed_at = NULL
    WHERE user_id = v_user_id
      AND mission_id = (SELECT mission_id FROM missions WHERE name = 'Semana Productiva');


    ----------------------------------------------------------------
    -- 🏆 LOGROS
    ----------------------------------------------------------------

    -- Novato → completo
    UPDATE user_achievements
    SET progress = '{"sightings":1}',
        obtained_at = NOW()
    WHERE user_id = v_user_id
      AND achievement_id = (SELECT achievement_id FROM achievements WHERE name = 'Novato');

    -- Naturalista → progreso parcial
    UPDATE user_achievements
    SET progress = '{"unique_species":6}',
        obtained_at = NULL
    WHERE user_id = v_user_id
      AND achievement_id = (SELECT achievement_id FROM achievements WHERE name = 'Naturalista');

    -- Ornitólogo → progreso parcial
    UPDATE user_achievements
    SET progress = '{"unique_species":25}',
        obtained_at = NULL
    WHERE user_id = v_user_id
      AND achievement_id = (SELECT achievement_id FROM achievements WHERE name = 'Ornitólogo');

END $$;

