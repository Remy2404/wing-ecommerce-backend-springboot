DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT n.nspname AS schema_name, c.conname AS constraint_name
        FROM pg_constraint c
                 JOIN pg_class t ON t.oid = c.conrelid
                 JOIN pg_namespace n ON n.oid = t.relnamespace
                 JOIN pg_attribute a ON a.attrelid = t.oid
        WHERE t.relname = 'refresh_tokens'
          AND c.contype = 'u'
          AND array_length(c.conkey, 1) = 1
          AND a.attname = 'user_id'
          AND a.attnum = ANY (c.conkey)
    LOOP
        EXECUTE format(
                'ALTER TABLE %I.refresh_tokens DROP CONSTRAINT IF EXISTS %I',
                r.schema_name,
                r.constraint_name
                );
    END LOOP;

    IF to_regclass('refresh_tokens') IS NOT NULL THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id)';
    END IF;
END
$$;
