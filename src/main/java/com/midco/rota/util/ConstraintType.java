package com.midco.rota.util;

/**
 * Whether a constraint is a rule the solver may never break, or a preference it
 * should weigh against everything else.
 *
 * <p>This is the severity half of a constraint setting; the magnitude lives
 * alongside it as a single score weight. Keeping the two apart means a setting
 * cannot express the meaningless state of being partly hard and partly soft.
 */
public enum ConstraintType {

	/** Never break this. A violation makes the whole rota infeasible. */
	HARD,

	/** Prefer not to break this. A violation costs score but still yields a usable rota. */
	SOFT
}
