-- Insert categories
INSERT INTO category (id, name) VALUES
    (1, 'Dog'),
    (2, 'Cat'),
    (3, 'Fish')
ON CONFLICT (id) DO NOTHING;

-- Insert tags
INSERT INTO tag (id, name) VALUES
    (1, 'doggie'),
    (2, 'large'),
    (3, 'small'),
    (4, 'kittie'),
    (5, 'fishy')
ON CONFLICT (id) DO NOTHING;

-- Insert pets (status must be uppercase: AVAILABLE, PENDING, SOLD)
INSERT INTO pet (id, name, photo_url, status, category_id) VALUES
    -- Dogs (1-20)
    (1, 'Afador', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/afador.jpg?raw=true', 'AVAILABLE', 1),
    (2, 'American Bulldog', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/american-bulldog.jpg?raw=true', 'AVAILABLE', 1),
    (3, 'Australian Retriever', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/australian-retriever.jpg?raw=true', 'AVAILABLE', 1),
    (4, 'Australian Shepherd', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/australian-shepherd.jpg?raw=true', 'AVAILABLE', 1),
    (5, 'Basset Hound', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/basset-hound.jpg?raw=true', 'AVAILABLE', 1),
    (6, 'Beagle', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/beagle.jpg?raw=true', 'AVAILABLE', 1),
    (7, 'Border Terrier', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/border-terrier.jpg?raw=true', 'AVAILABLE', 1),
    (8, 'Boston Terrier', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/boston-terrier.jpg?raw=true', 'AVAILABLE', 1),
    (9, 'Bulldog', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/bulldog.jpg?raw=true', 'AVAILABLE', 1),
    (10, 'Bullmastiff', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/bullmastiff.jpg?raw=true', 'AVAILABLE', 1),
    (11, 'Chihuahua', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/chihuahua.jpg?raw=true', 'AVAILABLE', 1),
    (12, 'Cocker Spaniel', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/cocker-spaniel.jpg?raw=true', 'AVAILABLE', 1),
    (13, 'German Sheperd', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/german-shepherd.jpg?raw=true', 'AVAILABLE', 1),
    (14, 'Labrador Retriever', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/labrador-retriever.jpg?raw=true', 'AVAILABLE', 1),
    (15, 'Pomeranian', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/pomeranian.jpg?raw=true', 'AVAILABLE', 1),
    (16, 'Pug', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/pug.jpg?raw=true', 'AVAILABLE', 1),
    (17, 'Rottweiler', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/rottweiler.jpg?raw=true', 'AVAILABLE', 1),
    (18, 'Shetland Sheepdog', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/shetland-sheepdog.jpg?raw=true', 'AVAILABLE', 1),
    (19, 'Shih Tzu', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/shih-tzu.jpg?raw=true', 'AVAILABLE', 1),
    (20, 'Toy Fox Terrier', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-breeds/toy-fox-terrier.jpg?raw=true', 'AVAILABLE', 1),
    -- Cats (21-30)
    (21, 'Abyssinian', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/abyssinian.jpg?raw=true', 'AVAILABLE', 2),
    (22, 'American Bobtail', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/american-bobtail.jpg?raw=true', 'AVAILABLE', 2),
    (23, 'American Shorthair', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/american-shorthair.jpg?raw=true', 'AVAILABLE', 2),
    (24, 'Balinese', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/balinese.jpg?raw=true', 'AVAILABLE', 2),
    (25, 'Birman', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/birman.jpg?raw=true', 'AVAILABLE', 2),
    (26, 'Bombay', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/bombay.jpg?raw=true', 'AVAILABLE', 2),
    (27, 'British Shorthair', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/british-shorthair.jpg?raw=true', 'AVAILABLE', 2),
    (28, 'Burmilla', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/burmilla.jpg?raw=true', 'AVAILABLE', 2),
    (29, 'Chartreux', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/chartreux.jpg?raw=true', 'AVAILABLE', 2),
    (30, 'Cornish Rex', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-breeds/cornish-rex.jpg?raw=true', 'AVAILABLE', 2),
    -- Fish (31)
    (31, 'Goldfish', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/fish-breeds/goldfish.jpg?raw=true', 'AVAILABLE', 3)
ON CONFLICT (id) DO NOTHING;

-- Insert pet-tag relationships
INSERT INTO pet_tag (pet_id, tag_id) VALUES
    -- Dogs: doggie + large or small
    (1, 1), (1, 2),   -- Afador: doggie, large
    (2, 1), (2, 2),   -- American Bulldog: doggie, large
    (3, 1), (3, 2),   -- Australian Retriever: doggie, large
    (4, 1), (4, 2),   -- Australian Shepherd: doggie, large
    (5, 1), (5, 3),   -- Basset Hound: doggie, small
    (6, 1), (6, 3),   -- Beagle: doggie, small
    (7, 1), (7, 3),   -- Border Terrier: doggie, small
    (8, 1), (8, 3),   -- Boston Terrier: doggie, small
    (9, 1), (9, 2),   -- Bulldog: doggie, large
    (10, 1), (10, 2), -- Bullmastiff: doggie, large
    (11, 1), (11, 3), -- Chihuahua: doggie, small
    (12, 1), (12, 3), -- Cocker Spaniel: doggie, small
    (13, 1), (13, 2), -- German Sheperd: doggie, large
    (14, 1), (14, 2), -- Labrador Retriever: doggie, large
    (15, 1), (15, 3), -- Pomeranian: doggie, small
    (16, 1), (16, 3), -- Pug: doggie, small
    (17, 1), (17, 2), -- Rottweiler: doggie, large
    (18, 1), (18, 2), -- Shetland Sheepdog: doggie, large
    (19, 1), (19, 3), -- Shih Tzu: doggie, small
    (20, 1), (20, 3), -- Toy Fox Terrier: doggie, small
    -- Cats: kittie + small
    (21, 4), (21, 3), -- Abyssinian: kittie, small
    (22, 4), (22, 3), -- American Bobtail: kittie, small
    (23, 4), (23, 3), -- American Shorthair: kittie, small
    (24, 4), (24, 3), -- Balinese: kittie, small
    (25, 4), (25, 3), -- Birman: kittie, small
    (26, 4), (26, 3), -- Bombay: kittie, small
    (27, 4), (27, 3), -- British Shorthair: kittie, small
    (28, 4), (28, 3), -- Burmilla: kittie, small
    (29, 4), (29, 3), -- Chartreux: kittie, small
    (30, 4), (30, 3), -- Cornish Rex: kittie, small
    -- Fish: fishy + small
    (31, 5), (31, 3)  -- Goldfish: fishy, small
ON CONFLICT DO NOTHING;

-- Reset sequences to ensure auto-increment starts after our initial data
SELECT setval('category_id_seq', (SELECT MAX(id) FROM category));
SELECT setval('tag_id_seq', (SELECT MAX(id) FROM tag));
SELECT setval('pet_id_seq', (SELECT MAX(id) FROM pet));
