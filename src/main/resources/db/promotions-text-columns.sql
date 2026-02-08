-- Ensure long promotion text values are storable.
DO $$
BEGIN
    IF to_regclass('public.promotions') IS NOT NULL THEN
        EXECUTE 'ALTER TABLE promotions ALTER COLUMN description TYPE TEXT';
        EXECUTE 'ALTER TABLE promotions ALTER COLUMN applicable_categories TYPE TEXT';
        EXECUTE 'ALTER TABLE promotions ALTER COLUMN applicable_merchants TYPE TEXT';
    END IF;
END $$;
