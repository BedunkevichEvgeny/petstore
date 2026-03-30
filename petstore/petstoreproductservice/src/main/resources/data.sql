-- Insert categories
INSERT INTO category (id, name) VALUES
    (1, 'Dog Toy'),
    (2, 'Dog Food'),
    (3, 'Cat Toy'),
    (4, 'Cat Food'),
    (5, 'Fish Toy'),
    (6, 'Fish Food')
ON CONFLICT (id) DO NOTHING;

-- Insert tags
INSERT INTO tag (id, name) VALUES
    (1, 'small'),
    (2, 'large')
ON CONFLICT (id) DO NOTHING;

-- Insert products (status must be uppercase: AVAILABLE, PENDING, SOLD)
INSERT INTO product (id, name, photo_url, status, category_id) VALUES
    (1, 'Ball', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-toys/ball.jpg?raw=true', 'AVAILABLE', 1),
    (2, 'Ball Launcher', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-toys/ball-launcher.jpg?raw=true', 'AVAILABLE', 1),
    (3, 'Plush Lamb', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-toys/plush-lamb.jpg?raw=true', 'AVAILABLE', 1),
    (4, 'Plush Moose', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-toys/plush-moose.jpg?raw=true', 'AVAILABLE', 1),
    (5, 'Large Breed Dry Food', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-food/large-dog.jpg?raw=true', 'AVAILABLE', 2),
    (6, 'Small Breed Dry Food', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/dog-food/small-dog.jpg?raw=true', 'AVAILABLE', 2),
    (7, 'Mouse', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-toys/mouse.jpg?raw=true', 'AVAILABLE', 3),
    (8, 'Scratcher', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-toys/scratcher.jpg?raw=true', 'AVAILABLE', 3),
    (9, 'All Sizes Cat Dry Food', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/cat-food/cat.jpg?raw=true', 'AVAILABLE', 4),
    (10, 'Mangrove Ornament', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/fish-toys/mangrove.jpg?raw=true', 'AVAILABLE', 5),
    (11, 'All Sizes Fish Food', 'https://raw.githubusercontent.com/chtrembl/staticcontent/master/fish-food/fish.jpg?raw=true', 'AVAILABLE', 6)
ON CONFLICT (id) DO NOTHING;

-- Insert product-tag relationships
INSERT INTO product_tag (product_id, tag_id) VALUES
    (1, 1), (1, 2),   -- Ball: small, large
    (2, 2),           -- Ball Launcher: large
    (3, 1), (3, 2),   -- Plush Lamb: small, large
    (4, 1), (4, 2),   -- Plush Moose: small, large
    (5, 2),           -- Large Breed Dry Food: large
    (6, 1),           -- Small Breed Dry Food: small
    (7, 1), (7, 2),   -- Mouse: small, large
    (8, 1), (8, 2),   -- Scratcher: small, large
    (9, 1), (9, 2),   -- All Sizes Cat Dry Food: small, large
    (10, 1), (10, 2), -- Mangrove Ornament: small, large
    (11, 1), (11, 2)  -- All Sizes Fish Food: small, large
ON CONFLICT DO NOTHING;

-- Reset sequences to ensure auto-increment starts after our initial data
SELECT setval('category_id_seq', (SELECT MAX(id) FROM category));
SELECT setval('tag_id_seq', (SELECT MAX(id) FROM tag));
SELECT setval('product_id_seq', (SELECT MAX(id) FROM product));
