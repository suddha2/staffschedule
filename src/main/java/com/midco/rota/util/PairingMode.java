package com.midco.rota.util;

/**
 * Per-template override of how a follower-type shift (e.g. SLEEP_IN) is staffed.
 * Unset (null) means "use the shift type's default".
 *
 * <ul>
 * <li>{@link #PAIRED}: the shift mirrors its leader's carer (same carer as the LONG_DAY).</li>
 * <li>{@link #STANDALONE}: the shift gets its own carer, chosen by the solver. For LA packages
 * where the sleep-in is not covered by the day's long-day carer.</li>
 * </ul>
 */
public enum PairingMode {
	PAIRED, STANDALONE
}
