-- Align with outbox_events: store payload as an app-owned JSON string (TEXT) rather than
-- JSONB. Neither table is ever queried via SQL JSON operators, so JSONB buys nothing here
-- and only adds Hibernate mapping friction.
ALTER TABLE notifications ALTER COLUMN payload_json TYPE TEXT USING payload_json::TEXT;
ALTER TABLE notifications RENAME COLUMN payload_json TO payload;
