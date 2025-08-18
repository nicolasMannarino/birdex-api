-- 001_all.sql  (PostgreSQL)
-- Crea extensión, esquema y datos iniciales para birds/colors/rarities + tablas puente

-- ====== EXTENSION ======
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ====== ESQUEMA ======
CREATE TABLE IF NOT EXISTS birds (
    bird_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    common_name TEXT NOT NULL,
    size TEXT NOT NULL,
    description TEXT NOT NULL,
    characteristics TEXT NOT NULL,
    image TEXT NOT NULL,
    migratory_wave_url TEXT NOT NULL
);

-- Evita duplicados y habilita ON CONFLICT (name) DO NOTHING
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'birds_name_uk'
    ) THEN
        ALTER TABLE birds ADD CONSTRAINT birds_name_uk UNIQUE (name);
    END IF;
END $$;

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

-- ====== SEED: AVES ======
INSERT INTO birds (bird_id, name, common_name, size, description, characteristics, image, migratory_wave_url) VALUES
  (gen_random_uuid(), 'Furnarius rufus', 'Hornero', 'Mediano',
   'Ave emblemática que construye nidos de barro con forma de horno.',
   'Nido de barro; territorial; frecuente en ciudades y áreas rurales.',
   'https://example.com/img/furnarius_rufus.jpg', 'https://example.com/migratory/furnarius_rufus'),

  (gen_random_uuid(), 'Gubernatrix cristata', 'Cardenal amarillo', 'Mediano',
   'Passeriforme amarillo con máscara negra y cresta conspicua.',
   'Canto potente; matorrales y bosques abiertos; muy presionado por captura.',
   'https://example.com/img/gubernatrix_cristata.jpg', 'https://example.com/migratory/gubernatrix_cristata'),

  (gen_random_uuid(), 'Paroaria coronata', 'Cardenal común', 'Mediano',
   'Cresta roja, dorso gris, partes inferiores blancas.',
   'Común en parques y jardines; gregario; omnívoro.',
   'https://example.com/img/paroaria_coronata.jpg', 'https://example.com/migratory/paroaria_coronata'),

  (gen_random_uuid(), 'Chauna torquata', 'Chajá', 'Grande',
   'Ave grande de humedales con potente vocalización y espolones alares.',
   'Parejas estables; vuela bien; frecuente en lagunas.',
   'https://example.com/img/chauna_torquata.jpg', 'https://example.com/migratory/chauna_torquata'),

  (gen_random_uuid(), 'Rhea americana', 'Ñandú grande', 'Muy grande',
   'Ratite corredora de pastizales abiertos.',
   'Gregario; gran corredor; nidos comunales.',
   'https://example.com/img/rhea_americana.jpg', 'https://example.com/migratory/rhea_americana'),

  (gen_random_uuid(), 'Rhea pennata', 'Ñandú petiso', 'Muy grande',
   'Ratite patagónica, más pequeño y críptico que R. americana.',
   'Adaptado a ambientes fríos y ventosos; baja densidad.',
   'https://example.com/img/rhea_pennata.jpg', 'https://example.com/migratory/rhea_pennata'),

  (gen_random_uuid(), 'Phoenicopterus chilensis', 'Flamenco austral', 'Grande',
   'Flamenco rosado pálido; filtra alimento en aguas someras.',
   'Colonial; movimientos locales amplios; sensible a disturbios.',
   'https://example.com/img/phoenicopterus_chilensis.jpg', 'https://example.com/migratory/phoenicopterus_chilensis'),

  (gen_random_uuid(), 'Ramphastos toco', 'Tucán toco', 'Grande',
   'Mayor de los tucanes, con gran pico anaranjado.',
   'Principalmente frugívoro; conspicuo; usa cavidades para nidificar.',
   'https://example.com/img/ramphastos_toco.jpg', 'https://example.com/migratory/ramphastos_toco'),

  (gen_random_uuid(), 'Turdus rufiventris', 'Zorzal colorado', 'Mediano',
   'Zorzal de vientre rufo y dorso pardo; canto melodioso.',
   'Común en jardines; dieta variada; anida en arbustos.',
   'https://example.com/img/turdus_rufiventris.jpg', 'https://example.com/migratory/turdus_rufiventris'),

  (gen_random_uuid(), 'Cyanocompsa brissonii', 'Reinamora grande', 'Mediano',
   'Macho azul intenso con tonos negros; robusto.',
   'Consume semillas y frutos; discreto en matorrales.',
   'https://example.com/img/cyanocompsa_brissonii.jpg', 'https://example.com/migratory/cyanocompsa_brissonii'),

  (gen_random_uuid(), 'Cygnus melancoryphus', 'Cisne de cuello negro', 'Grande',
   'Cuerpo blanco y cuello negro; lagunas y estuarios.',
   'Fidelidad de pareja; nidifica en juncales flotantes.',
   'https://example.com/img/cygnus_melancoryphus.jpg', 'https://example.com/migratory/cygnus_melancoryphus'),

  (gen_random_uuid(), 'Vanellus chilensis', 'Tero', 'Mediano',
   'Ave de pastizal muy vocal; antifaz negro característico.',
   'Defiende el nido activamente; común en praderas y parques.',
   'https://example.com/img/vanellus_chilensis.jpg', 'https://example.com/migratory/vanellus_chilensis'),

  (gen_random_uuid(), 'Buteogallus coronatus', 'Águila coronada', 'Grande',
   'Rapaz grande de zonas áridas; penacho corto.',
   'Muy baja densidad; amenazas por pérdida de hábitat y persecución.',
   'https://example.com/img/buteogallus_coronatus.jpg', 'https://example.com/migratory/buteogallus_coronatus'),

  (gen_random_uuid(), 'Cathartes aura', 'Jote cabeza colorada', 'Grande',
   'Buitre planeador de cabeza roja desnuda.',
   'Carroñero; excelente olfato; frecuente en campo abierto.',
   'https://example.com/img/cathartes_aura.jpg', 'https://example.com/migratory/cathartes_aura'),

  (gen_random_uuid(), 'Pipraeidea bonariensis', 'Frutero azul', 'Mediano',
   'Macho azul oscuro con vientre amarillo; hembra verdosa.',
   'Consume frutos y artrópodos; visita arboledas.',
   'https://example.com/img/pipraeidea_bonariensis.jpg', 'https://example.com/migratory/pipraeidea_bonariensis'),

  (gen_random_uuid(), 'Asio clamator', 'Lechuzón orejudo', 'Grande',
   'Búho con penachos conspicuos; plumaje críptico.',
   'Crepuscular/nocturno; bordes de bosque y pastizales.',
   'https://example.com/img/asio_clamator.jpg', 'https://example.com/migratory/asio_clamator'),

  (gen_random_uuid(), 'Thraupis sayaca', 'Celestino', 'Mediano',
   'Tangara celeste-grisácea; común en el NE de Sudamérica.',
   'Frugívoro; frecuenta jardines y arboledas.',
   'https://example.com/img/thraupis_sayaca.jpg', 'https://example.com/migratory/thraupis_sayaca')
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
