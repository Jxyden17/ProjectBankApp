INSERT INTO example_users (id, first_name, last_name, email)
SELECT 1, 'Alice', 'Example', 'alice@example.com'
WHERE NOT EXISTS (
    SELECT 1 FROM example_users WHERE id = 1
);

INSERT INTO example_users (id, first_name, last_name, email)
SELECT 2, 'Bob', 'Example', 'bob@example.com'
WHERE NOT EXISTS (
    SELECT 1 FROM example_users WHERE id = 2
);
