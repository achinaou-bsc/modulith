ALTER TABLE jobs
  ADD COLUMN temperature_predicate TEXT NOT NULL,
  ADD COLUMN aridity_predicate     TEXT NOT NULL;
