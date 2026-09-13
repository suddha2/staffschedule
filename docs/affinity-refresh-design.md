# Affinity Refresh — in-app design

Status: draft for review · Owner: sudha@midco-care.co.uk · 2026-09

## 1. Purpose

Keep each carer's `employee.preferred_service` affinity weights current with
People Planner (PP) history, **automatically and inside the app**, so the solver
keeps reproducing how managers concentrate carers.

Validated already (SQL prototype, three regions): mined affinity at solver weight
500k–1M reproduces manager concentration — ~1.5–2.4 houses/carer/week and a
68–74% familiar-house rate, with coverage that *improved* rather than degraded.
This design turns that one-off prototype into a maintained feature.

Reference implementation: the SQL pipeline in `Downloads/pp_import/pipeline/`
(`p2_canonicalise`, `p3_reconcile`, `p4_mine`, `p5_generate_apply`). The Java
services below reproduce it exactly and are tested against the same numbers
(216 employees / 815 location-weights).

## 2. Cadence

Two rhythms, matching the two kinds of data:

| Rhythm | Runs | What |
|---|---|---|
| **Monthly** (after pay period close) | automatic | pull recent PP data → **auto-bridge** confident matches → mine weights → apply |
| **Quarterly** | human-in-the-loop | clear the reconciliation queue: unmatched carers/houses a person must confirm or mark out-of-scope |

Rationale: a carer's *identity* and a house's *identity* never change, so the
bridge is build-once + occasional additions — but *weights* drift as work
patterns shift, so they refresh monthly. The auto-match half of bridging runs
every month (so a new starter isn't unweighted for a quarter); only the
ambiguous leftovers wait for the quarterly human review.

## 3. Architecture

```
                 ┌──────────────────────── monthly @Scheduled ───────────────────────┐
 PP Data Engine  │  DataEngineClient → StagingService → BridgeService(auto) →         │
 API  ──────────►│  AffinityMiningService → apply to employee.preferred_service       │
                 │                          ↑                         ↓ (backup)       │
                 │                   bridge_* tables            AffinityRun (audit)     │
                 └───────────────────────────────────────────────────────────────────┘
                    quarterly: BridgeService exposes unmatched queue → Admin UI → confirm
```

All new persistence lives in the app DB (`staffrota_live`), following the
`ConstraintSetting` / `SolverConfigService` pattern already in the codebase.

## 4. Data model (new entities)

### Durable bridges (the permanent asset)

```
bridge_employee    pp_id (PK) · employee_id (FK employee) · method · confirmed · updated_at
bridge_location    pp_loc (PK) · canonical · method · confirmed · updated_at
bridge_canonical   canonical · live_location   (PK: canonical+live_location)
```

`bridge_canonical` is the split-house fan-out: PP "Alexandra Road No. 8"
(one canonical) → many live flats. A 1:1 house maps its canonical to itself.

### Run audit + rollback

```
affinity_run       id · type(MONTHLY|QUARTERLY|MANUAL) · status · started_at · finished_at
                   · triggered_by · employees_updated · coverage_json · notes
affinity_backup    run_id (FK) · employee_id · previous_preferred_service
```

Rollback = restore `previous_preferred_service` for a run. (Replaces the ad-hoc
`employee_preference_backup` table from the prototype.)

### Employee change

```
employee.weights_locked  boolean default false
```

Manual-edit protection: a locked carer is never overwritten by a refresh. Set
when a manager hand-tunes preferences. (Closes the one known gap in the SQL
prototype.)

### Staging

Recent PP pull only (monthly window, not the full 1.5M history), so staging can
be modest. Options: a `pp_staging` schema of raw tables, or stream-and-discard
after building a `pp_shift` working set. **Recommendation:** persist a rolling
`pp_shift` supported-living fact (filtered, bridged) keyed by date, so mining and
reconciliation read it without re-pulling. Raw payloads are transient.

## 5. Services

| Service | Responsibility | Mirrors prototype |
|---|---|---|
| `DataEngineClient` (interface) | `fetchShifts(from,to)`, `fetchEmployees()`, `fetchServiceLocations()` → DTOs. Impl from API shapes; a CSV-backed impl for tests/fallback. | replaces `p1_stage` |
| `StagingService` | Filter to supported-living shifts (drop dom-care by service type); build `pp_shift`. | `p2_canonicalise` |
| `BridgeService` | Auto-match employees (email → name) and locations (token + group rules); expose unmatched queue; accept manual confirmations. | `p3_reconcile` + bridges |
| `AffinityMiningService` | Count shifts per canonical → normalise to 0–100 weight (floor 5) → expand to live locations → assemble `LOC:w,LOC:w`. | `p4_mine` |
| `AffinityRefreshOrchestrator` | Run a cycle: stage → bridge(auto) → mine → back up → apply (mined + unlocked only) → record `AffinityRun`. Transactional apply; supports **dry-run** (compute + coverage, no write). | `p5` + apply |

Mining math (from the validated prototype):
`weight = round(100 * shifts_at_canonical / total_shifts)`, keep ≥ 5, written to
**every** live location the canonical expands to (per-location prefs are
independent 0–100, not a distribution — a fully-Alexandra carer gets 100 on each
Alexandra flat).

## 6. Scheduled jobs

- **Monthly** — `@Scheduled(cron=…)` → `orchestrator.run(MONTHLY)`. Pull window
  = rolling N months (config; prototype used ~since a fixed date, make it
  rolling e.g. 12 months). Auto-applies.
- **Quarterly** — `@Scheduled(cron=…)` → checks the unmatched queue; if
  non-empty, notifies admins that reconciliation is due (does **not** auto-apply
  human decisions). Light: a nudge + a coverage snapshot.

Both honour a global on/off and a dry-run flag so they can be enabled safely.

## 7. Admin API (`@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")`)

```
POST /api/admin/affinity/run?dryRun=            trigger a run (manual)
GET  /api/admin/affinity/runs                    run history / audit
GET  /api/admin/affinity/coverage                per-region coverage
GET  /api/admin/affinity/reconciliation          unmatched queue (carers + houses)
POST /api/admin/affinity/bridge/employee         confirm/override an employee match
POST /api/admin/affinity/bridge/location         confirm a house / mark out-of-scope
POST /api/admin/affinity/rollback/{runId}        restore previous weights
PUT  /api/admin/employee/{id}/weights-locked     lock/unlock a carer
```

Reconciliation queue is **live-driven** (the actionable framing from the
prototype): "active scheduler carers with no mined weights" (unmatched to PP →
add a bridge; or genuinely new → nothing to do), plus "in-scope PP houses with a
`Service -` branch and no map". Today that is ~32 carers and ~17 houses — a short
review, not a re-analysis.

## 8. Admin UI (employee-scheduler-vite)

One "Affinity" admin page:

- **Run now** (with dry-run toggle) + last-run summary and per-region coverage table.
- **Reconciliation** — two tabs (Carers / Houses), each row with Confirm-match /
  Skip-as-out-of-scope; shows the PP name/email or branch hint to decide.
- **History** — past runs with coverage + a Rollback action.

Build note: the Vite app is at `Downloads/employee-scheduler-vite` (outside the
workspace); watch the known stale-`dist/` issue on deploy.

## 9. Tunables / config

- Mining window (rolling months) and weight floor (default 5) — a small config
  table or app properties.
- Cadence crons + enable flags — config.
- **Affinity strength stays in `constraint_setting`** (`'Location preferences
  (reward only)'`, 500k–1M validated) — that is the solver dial, separate from
  this pipeline which only maintains the *data*.

## 10. Safety

- Every applying run backs up prior weights (`affinity_backup`) → one-click rollback.
- Only **mined + unlocked** active employees are written; everyone else untouched.
- Apply is transactional (no half-applied run); dry-run for preview.
- Out-of-scope PP data (domiciliary care) is excluded at the staging filter, by
  service type — never mined.

## 11. What I need from the Data Engine API (to build `DataEngineClient`)

The mining needs these fields; please map them from the API req/res shapes:

**Shifts (ServiceDuty)** — must support a **date-range filter** (monthly window):
- employee id (= PP `ActualEmployeeID` / `PlannedEmployeeID`)
- service-location id, service-requirement id
- planned start / end datetime, planned date
- **service type** (to keep "… Shift" and drop "Care Call Shift" / dom-care)
- cancelled flag

**Employees**:
- id, first name, last name, email, active/termination status

**Service locations**:
- id, name / branch, region or area

Also: auth method (key / OAuth), pagination model, and any rate/volume limits.

## 12. Phasing

1. **Bridges + mining as Java services + entities** — port §4/§5, unit-test to
   reproduce 216 employees / 815 weights against staged data. No API.
2. **Orchestrator + `AffinityRun` audit + admin endpoints + rollback + `weights_locked`.**
3. **`DataEngineClient`** — implement from the shared API shapes; swap out the
   CSV loader.
4. **Scheduled monthly/quarterly jobs.**
5. **Vite admin UI.**

The API dependency blocks only step 3; 1–2 (and their tests) start immediately.

## 13. Open questions

- Does the PP employee record carry region, or do we keep deriving it from the
  live `employee.preferred_region`?
- Cross-region carers: mine globally (as now) or scope per region?
- Rolling window length — 12 months balances recency vs. stability; confirm.
- Quarterly job: notify-only, or also auto-open a reconciliation task?
