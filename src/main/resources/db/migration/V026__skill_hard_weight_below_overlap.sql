-- V026: fix V025's weighting. V025 made "Missing required skill" HARD but left its weight at
-- 200000, while "Overlapping shifts" is HARD 1000 - so the solver double-booked leads (two
-- lead shifts at once, or a LONG_DAY over a lead shift) rather than accept a non-lead on a
-- lead shift. A physical impossibility must outrank eligibility: put skill below overlap.
UPDATE constraint_setting SET score_weight = 100
 WHERE constraint_name = 'Missing required skill' AND constraint_type = 'HARD';
