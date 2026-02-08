-- Active: 1770541785885@@dpg-d622fb4hg0os7385ja70-a.singapore-postgres.render.com@5432@ecommerce_db_a1c8@public
-- Pre-schema cleanup for legacy/orphan rows.
-- Runs before Hibernate schema update so FK creation does not fail on bad historical data.

DO $$
BEGIN
    -- refresh_tokens.user_id -> users.id (required)
    IF to_regclass('public.refresh_tokens') IS NOT NULL AND to_regclass('public.users') IS NOT NULL THEN
        EXECUTE '
            DELETE FROM refresh_tokens rt
            WHERE rt.user_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = rt.user_id)
        ';
    END IF;

    -- orders.delivery_address_id -> addresses.id (required)
    -- remove child rows first for orders that will be deleted
    IF to_regclass('public.orders') IS NOT NULL AND to_regclass('public.addresses') IS NOT NULL THEN
        IF to_regclass('public.order_items') IS NOT NULL THEN
            EXECUTE '
                DELETE FROM order_items oi
                WHERE oi.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.delivery_address_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
                )
            ';
        END IF;

        IF to_regclass('public.payments') IS NOT NULL THEN
            EXECUTE '
                DELETE FROM payments p
                WHERE p.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.delivery_address_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
                )
            ';
        END IF;

        IF to_regclass('public.deliveries') IS NOT NULL THEN
            EXECUTE '
                DELETE FROM deliveries d
                WHERE d.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.delivery_address_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
                )
            ';
        END IF;

        IF to_regclass('public.promotion_usage') IS NOT NULL THEN
            EXECUTE '
                DELETE FROM promotion_usage pu
                WHERE pu.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.delivery_address_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
                )
            ';
        END IF;

        IF to_regclass('public.wing_points_transactions') IS NOT NULL THEN
            EXECUTE '
                UPDATE wing_points_transactions wpt
                SET order_id = NULL
                WHERE wpt.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.delivery_address_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
                )
            ';
        END IF;

        IF to_regclass('public.reviews') IS NOT NULL THEN
            EXECUTE '
                UPDATE reviews r
                SET order_id = NULL
                WHERE r.order_id IN (
                    SELECT o.id
                    FROM orders o
                    WHERE o.delivery_address_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
                )
            ';
        END IF;

        EXECUTE '
            DELETE FROM orders o
            WHERE o.delivery_address_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.id = o.delivery_address_id)
        ';
    END IF;
END $$;

