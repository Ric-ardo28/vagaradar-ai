CREATE TABLE gmail_sync_state (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    last_successful_sync_at TIMESTAMP WITH TIME ZONE NOT NULL
);
