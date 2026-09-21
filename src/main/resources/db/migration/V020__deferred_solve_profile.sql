-- V020: a solve carries an objective PROFILE — which side of the
-- continuity-vs-hours-fairness trade-off to favour. SPREAD (even hours, whole team)
-- or CONTINUITY (keep carers in their prior-period slots / stable weekly patterns).
-- The solver applies the profile's trade-off weight overrides for that run, so the
-- same period can be generated both ways and compared.
ALTER TABLE deferred_solve_request ADD COLUMN IF NOT EXISTS profile VARCHAR(20) NOT NULL DEFAULT 'SPREAD';
