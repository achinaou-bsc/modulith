CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS jobs (
  id             UUID                        PRIMARY KEY DEFAULT uuid_generate_v4(),
  type           TEXT                        NOT NULL,
  status         TEXT                        NOT NULL,
  computation_id TEXT,
  submitted_at   TIMESTAMP WITHOUT TIME ZONE,
  completed_at   TIMESTAMP WITHOUT TIME ZONE
);
