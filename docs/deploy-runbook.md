# Deploy runbook — this version

Deploying onto a **fresh copy of production** (the recommended path). Migrations
are manual (no Flyway; `ddl-auto=none`, so the app never creates tables itself).

## What ships in the code vs what you must apply

| Improvement | Ships in code? | Action on deploy |
|---|---|---|
| Solver in REPRODUCIBLE mode + union move-selector (`solverConfig.xml`) | ✅ | deploy + restart |
| Weekly cap / minimum demoted to SOFT | ✅ (code default) | auto-seeded |
| Widened thresholds (max non-floating 10, max locations 6) | ✅ (`SolverTuning`) | auto-seeded |
| Affinity dial = 1,000,000 (`Location preferences`) | ✅ (code default, was 10,000) | auto-seeded |
| Continuity seeding + its soft constraint | ✅ | works from period 2 |
| New-joiner `active = true` on create | ✅ | — |
| Leave/unavailability table + two-source sync | ✅ (code) | run V010; set props to enable |
| **Mined employee `preferred_service` weights** | ❌ **data** | **apply after migrations (below)** |
| Manager pins (`pinned_template_assignment`) | ❌ data | intact on the fresh prod copy — nothing to do |

The two SQL-only tunings from the experiments — the affinity dial and the widened
thresholds — are now **baked into the code defaults**, so a fresh DB seeds them
correctly. The one remaining data step is applying the mined weights.

## Steps (in order)

1. **Snapshot prod** → create the fresh target DB from it. (Pins come across intact.)

2. **Run the new migrations** against the target:
   ```bash
   export PATH="/c/Program Files/PostgreSQL/17/bin:$PATH"
   psql -U postgres -d <target_db> -f src/main/resources/db/migration/V009__solver_constraint_settings.sql
   psql -U postgres -d <target_db> -f src/main/resources/db/migration/V010__employee_availability.sql
   psql -U postgres -d <target_db> -f src/main/resources/db/migration/V011__employee_external_ids.sql
   ```
   (V001–V008 too only if the copy predates them — a real prod copy already has them.)

3. **Deploy the app and start it** (JDK 21). On first startup `SolverConfigService`
   seeds the empty tables from the code defaults:
   - `constraint_setting` — 42 rows (incl. `Location preferences` = 1,000,000, the
     cap/minimum as SOFT, the 13 never-run constraints seeded `enabled = false`).
   - `solver_tuning` — 8 rows.
   Verify in the log: `Seeded constraint_setting ...`, `Seeded solver_tuning ...`.

4. **Apply the mined weights** — the one data step that gives the affinity benefit.
   Run the pipeline's generated `apply_to_live.sql` against the target (it backs up
   `preferred_service` first and updates active employees). Without this, carers have
   no affinity and rotas fall back to ~50% familiar.
   ```bash
   psql -U postgres -d <target_db> -f <path>/apply_to_live.sql
   ```

5. **(Optional) Enable leave sync** — set per environment; disabled by default:
   `peopleplanner.enabled/base-url/api-key` and/or `peoplehr.enabled/base-url/api-key`.
   Confirm the two APIs' response field names match `PpLeaveRecord` / `PeopleHrLeaveRecord`
   before switching on.
   - **Populate the match keys** so leave resolves by stable id, not email:
     `employee.pp_employee_id` (People Planner id, zero-hours carers) and
     `employee.peoplehr_employee_id` (PeopleHR id, contracted staff). One-time
     bulk-populate `pp_employee_id` from the affinity email/name bridge; set
     `peoplehr_employee_id` when PeopleHR is connected. Until an id is stored the
     sync falls back to email, and logs any employee it can't resolve.

## Timing / restart notes

- Read **per solve** (no restart to change): `constraint_setting` weights,
  `employee.preferred_service`, pins, availability rows.
- Needs a **restart**: `solverConfig.xml`, and the `solver_tuning` thresholds
  (read when the constraint streams are built).

## Verify it's working

- First solve log reads `environment mode (REPRODUCIBLE)` and ends `0hard` (feasible).
- `SELECT constraint_name, score_weight FROM constraint_setting WHERE constraint_name='Location preferences (reward only)';` → 1000000.
- Spot-check a solved region's familiar-house rate (~70%+ once weights are applied).
- Continuity: reproduces the prior period from the 2nd solved period onward
  (the first solve of any region has no prior published rota to carry forward).
