-- V044: trim leading/trailing spaces off house names (Barnet, 2026-11-11). Every region.
--
-- Why: live has "Alexandra Flat B - LC" twice, one with a trailing space, so the rota grid and
-- Service Stats show two houses and split the shifts between them. The solver and the services
-- lists already compare trimmed names; the templates themselves were never cleaned, and the
-- template screens saved whatever was typed. ShiftTemplate.setLocation now trims on save.
--
-- Only the ends are trimmed. Spaces inside a name (Kent "SHERIDEN  5") are left alone, because
-- carers' service lists hold the same spelling; fix those with Rename on the Services screen.
-- Rotas, pins and history link to templates by id, so nothing else moves.
BEGIN;

UPDATE shift_templates SET location = btrim(location) WHERE location <> btrim(location);

UPDATE service_location SET staffrota_location = btrim(staffrota_location)
 WHERE staffrota_location <> btrim(staffrota_location);

UPDATE employee_schedule_patterns SET location = btrim(location) WHERE location <> btrim(location);

COMMIT;
