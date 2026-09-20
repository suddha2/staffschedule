-- V018: rename the follower discriminator SLEEP_IN -> FOLLOWER.
--
-- The paired shadow entity was generalised: it is no longer specifically a
-- sleep-in, it is "the follower role in a pair" (any type flagged as a follower).
-- The Java class SleepInShiftAssignment was renamed to FollowerShiftAssignment and
-- its @DiscriminatorValue changed from 'SLEEP_IN' to 'FOLLOWER'. Existing rows
-- still carry the old value, so Hibernate could not instantiate them until this
-- runs. Pure rename — no behaviour change; the SLEEP_IN shift-type CODE is
-- untouched (this is the assignment-row discriminator, not the shift type).
--
-- Must be applied in lockstep with the code that renames the class.

UPDATE rota_shift_assignment
   SET assignment_type = 'FOLLOWER'
 WHERE assignment_type = 'SLEEP_IN';
