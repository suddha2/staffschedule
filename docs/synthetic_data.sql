-- ============================================================================
-- Synthetic dataset: service locations, shift templates, employees.
-- Shapes and distributions mirror the real staffrota_live data (profiled
-- 2026-09-13) but all names/emails are fake -- safe for dev, demo, load tests.
--
-- Builds into an isolated schema `synth` (nothing touches live). Reproducible:
-- setseed + md5-hashed keys give the same data every run.
--
-- Run:  psql -U postgres -d <db> -f synthetic_data.sql   (or paste into DBeaver)
-- Load into a TEST app DB: see the INSERT..SELECT block at the bottom.
--
-- Distributions reproduced:
--   102 locations across 8 regions (BARNET 30 ... MANCHESTER 1)
--   ~1435 shift templates: DAY 31% / WAKING_NIGHT 24% / LONG_DAY 15% /
--     FLOATING 15% / SLEEP_IN 8% / CARE_CALL 7%; ~6% 2:1 (emp_count 2)
--   260 employees (248 active): PERMANENT ~78% / ZERO_HOURS; L1/L2/ZERO rate;
--     region mix matching real; each with weighted preferred_service affinity
--     to 1-3 houses in their own region.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS synth;
SELECT setseed(0.42);

-- ---------- 1. SERVICE LOCATIONS ----------
DROP TABLE IF EXISTS synth.service_location CASCADE;
CREATE TABLE synth.service_location (id serial PRIMARY KEY, name text, region text, dominant_gender text);

WITH band(region, lo, hi) AS (VALUES
  ('BARNET',1,31),('HERTFORDSHIRE',31,50),('KENT',50,70),('NORTHANTS',70,84),
  ('BUCKS',84,91),('CORNWALL',91,97),('GLOUCOXF',97,102),('MANCHESTER',102,103)),
seq AS (SELECT gs AS gid FROM generate_series(1,102) gs),
streets(w) AS (VALUES ('Oakfield'),('Maple'),('Cedar'),('Birchwood'),('Elm'),('Hillcrest'),
  ('Meadow'),('Riverside'),('Ashgrove'),('Willow'),('Sunnybank'),('Fairview'),('Brookside'),
  ('Highfield'),('Kingsley'),('Langley'),('Woodland'),('Priory'),('Grange'),('Foxglove'),
  ('Harewood'),('Larch'),('Rowan'),('Sycamore'),('Chestnut')),
suffix(s) AS (VALUES ('Road'),('Close'),('Avenue'),('Lodge'),('House'),('Court'),('Gardens'),('Rise'))
INSERT INTO synth.service_location (name, region, dominant_gender)
SELECT
  (SELECT w FROM streets ORDER BY md5(s.gid::text||w) LIMIT 1) || ' '
   || (SELECT x FROM suffix x2(x) ORDER BY md5(s.gid::text||x) LIMIT 1) || ' No. ' || s.gid,
  b.region,
  CASE WHEN (('x'||substr(md5(s.gid::text||'g'),1,8))::bit(32)::int & 127) < 47 THEN 'FEMALE'
       WHEN (('x'||substr(md5(s.gid::text||'g'),1,8))::bit(32)::int & 127) < 88 THEN 'MALE'
       ELSE 'ANY' END
FROM seq s JOIN band b ON s.gid >= b.lo AND s.gid < b.hi;

-- ---------- 2. SHIFT TEMPLATES ----------
DROP TABLE IF EXISTS synth.shift_template CASCADE;
CREATE TABLE synth.shift_template (
  id serial PRIMARY KEY, location text, shift_type text, day_of_week text,
  start_time time, end_time time, break_start time, break_end time,
  total_hours numeric, required_gender text, required_skills text,
  region text, emp_count smallint, allocation_priority int, active boolean);

WITH cat(shift_type, st, et, bs, be, hrs, prob) AS (VALUES
  ('DAY',time '08:00',time '20:00',NULL,NULL,11.0,0.63),
  ('WAKING_NIGHT',time '20:00',time '08:00',NULL,NULL,12.0,0.46),
  ('FLOATING',time '12:00',time '14:00',NULL,NULL,2.0,0.32),
  ('LONG_DAY',time '07:00',time '22:00',time '12:00',time '14:00',13.0,0.31),
  ('SLEEP_IN',time '22:00',time '07:00',NULL,NULL,9.0,0.14),
  ('CARE_CALL',time '06:30',time '21:30',NULL,NULL,12.0,0.10)),
days(dow) AS (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY'),('SUNDAY'))
INSERT INTO synth.shift_template
  (location,shift_type,day_of_week,start_time,end_time,break_start,break_end,total_hours,
   required_gender,required_skills,region,emp_count,allocation_priority,active)
SELECT l.name,c.shift_type,d.dow,c.st,c.et,c.bs,c.be,c.hrs,
  CASE WHEN c.shift_type='FLOATING' THEN 'ANY'
       WHEN (('x'||substr(md5(l.id::text||c.shift_type||d.dow||'g'),1,8))::bit(32)::int & 7) < 2 THEN 'ANY'
       ELSE l.dominant_gender END,
  CASE WHEN (('x'||substr(md5(l.id::text||c.shift_type||d.dow||'k'),1,8))::bit(32)::int & 15)=0 THEN 'BUCCAL' END,
  l.region,
  CASE WHEN (('x'||substr(md5(l.id::text||c.shift_type||d.dow||'e'),1,8))::bit(32)::int & 31)=0 THEN 2 ELSE 1 END,
  CASE WHEN c.shift_type='CARE_CALL' THEN 1 ELSE 10 END, true
FROM synth.service_location l
JOIN cat c ON (('x'||substr(md5(l.id::text||c.shift_type||'incl'),1,8))::bit(32)::int & 1023) < (c.prob*1024)
CROSS JOIN days d;

-- ---------- 3. EMPLOYEES ----------
DROP TABLE IF EXISTS synth.employee CASCADE;
CREATE TABLE synth.employee (
  id serial PRIMARY KEY, first_name text, last_name text, gender text, contract_type text,
  min_hours numeric, max_hours numeric, rate_code text, rest_days_per_cycle int,
  preferred_region text, preferred_service text, restricted_service text,
  preferred_days text, restricted_days text, preferred_shifts text, restricted_shifts text,
  skills text, days_on int, days_off int, week_on int, week_off int,
  invert_pattern boolean, active boolean, email text);

WITH fn(w) AS (VALUES ('Amara'),('Blessing'),('Chen'),('Divya'),('Emeka'),('Fatima'),('Grace'),
  ('Hassan'),('Ines'),('Joyce'),('Kwame'),('Lucia'),('Mariam'),('Ngozi'),('Omar'),('Priya'),
  ('Rosa'),('Samuel'),('Tendai'),('Uche'),('Vera'),('Wei'),('Yusuf'),('Zara'),('Daniel'),('Esther')),
ln(w) AS (VALUES ('Adeyemi'),('Baker'),('Chowdhury'),('Dube'),('Evans'),('Fernandez'),('Gupta'),
  ('Hughes'),('Iqbal'),('Jones'),('Khan'),('Lewis'),('Mensah'),('Nkosi'),('Owusu'),('Patel'),
  ('Quinn'),('Roberts'),('Sithole'),('Thomas'),('Uddin'),('Verma'),('Walsh'),('Xu'),('Young'),('Zulu')),
reg(region, lo, hi) AS (VALUES
  ('BARNET',0,76),('HERTFORDSHIRE',76,134),('KENT',134,190),('BUCKS',190,214),
  ('CORNWALL',214,227),('GLOUCOXF',227,239),('NORTHANTS',239,248)),
seq AS (SELECT gs AS n FROM generate_series(1,260) gs)
INSERT INTO synth.employee
  (first_name,last_name,gender,contract_type,min_hours,max_hours,rate_code,rest_days_per_cycle,
   preferred_region,preferred_shifts,skills,days_on,days_off,invert_pattern,active,email)
SELECT
  (SELECT w FROM fn ORDER BY md5(n::text||w) LIMIT 1),
  (SELECT w FROM ln ORDER BY md5(n::text||'-'||w) LIMIT 1),
  CASE WHEN n % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
  perm.is_perm,
  CASE WHEN perm.is_perm='PERMANENT' THEN 37.50 ELSE 0.00 END,
  (ARRAY[72.00,105.00,60.00,40.00])[1+(n%4)],
  CASE WHEN perm.is_perm='ZERO_HOURS' THEN 'ZERO_HOURS'
       WHEN n%20=0 THEN 'L2' WHEN n%53=0 THEN 'L3' ELSE 'L1' END,
  CASE WHEN n%5=0 THEN (ARRAY[4,6,8])[1+(n%3)] END,
  (SELECT region FROM reg WHERE (n%248)>=lo AND (n%248)<hi LIMIT 1),
  CASE WHEN n%6=0 THEN 'WAKING_NIGHT' WHEN n%6=1 THEN 'DAY' WHEN n%6=2 THEN 'LONG_DAY' END,
  CASE WHEN n%7=0 THEN 'BUCCAL' END,
  CASE WHEN perm.is_perm='PERMANENT' AND n%3=0 THEN 23 WHEN perm.is_perm='PERMANENT' THEN 6 END,
  CASE WHEN perm.is_perm='PERMANENT' AND n%3=0 THEN 4 WHEN perm.is_perm='PERMANENT' THEN 1 END,
  false, (n<=248), 'carer'||n||'@synth.example'
FROM seq CROSS JOIN LATERAL (SELECT CASE WHEN (n%100)<71 THEN 'PERMANENT' ELSE 'ZERO_HOURS' END is_perm) perm;

-- weighted preferred_service affinity: 1-3 houses in the employee's own region
UPDATE synth.employee e SET preferred_service = a.svc FROM (
  SELECT e2.id, string_agg(p.name||':'||p.wt, ',' ORDER BY p.wt DESC) svc
  FROM synth.employee e2 CROSS JOIN LATERAL (
    SELECT l.name, (ARRAY[60,30,10])[row_number() OVER (ORDER BY md5(l.id::text||e2.id::text))] wt
    FROM synth.service_location l WHERE l.region=e2.preferred_region
    ORDER BY md5(l.id::text||e2.id::text) LIMIT (1+(e2.id%3))) p
  WHERE p.wt IS NOT NULL GROUP BY e2.id) a WHERE a.id=e.id;

-- ---------- summary ----------
SELECT 'service_location' t, count(*) n FROM synth.service_location
UNION ALL SELECT 'shift_template', count(*) FROM synth.shift_template
UNION ALL SELECT 'employee', count(*) FROM synth.employee;

-- ============================================================================
-- LOAD INTO A TEST APP DATABASE (uncomment; run against a NON-production DB).
-- Column shapes match the app tables. There is no app service_location table --
-- locations live as the `location` string on shift_templates + in
-- preferred_service, both already populated above.
-- ============================================================================
-- INSERT INTO shift_templates
--   (location,shift_type,day_of_week,start_time,end_time,break_start,break_end,total_hours,
--    required_gender,required_skills,region,emp_count,allocation_priority,active)
-- SELECT location,shift_type,day_of_week,start_time,end_time,break_start,break_end,total_hours,
--        required_gender,required_skills,region,emp_count,allocation_priority,active
-- FROM synth.shift_template;
--
-- INSERT INTO employee
--   (first_name,last_name,gender,contract_type,min_hours,max_hours,rate_code,rest_days_per_cycle,
--    preferred_region,preferred_service,restricted_service,preferred_days,restricted_days,
--    preferred_shifts,restricted_shifts,skills,days_on,days_off,week_on,week_off,invert_pattern,active,email)
-- SELECT first_name,last_name,gender,contract_type,min_hours,max_hours,rate_code,rest_days_per_cycle,
--        preferred_region,preferred_service,restricted_service,preferred_days,restricted_days,
--        preferred_shifts,restricted_shifts,skills,days_on,days_off,week_on,week_off,invert_pattern,active,email
-- FROM synth.employee;
