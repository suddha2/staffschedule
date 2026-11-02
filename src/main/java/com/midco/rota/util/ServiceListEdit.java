package com.midco.rota.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edits a carer's comma-separated service list ("HOUSE:weight,HOUSE:weight" or "HOUSE,HOUSE") one
 * entry at a time. Text replace is not safe here: "2 AUDLEY" sits inside "FLAT 2 AUDLEY".
 */
public final class ServiceListEdit {

	private ServiceListEdit() {
	}

	/** The names in a list, trimmed, in order, repeats included. */
	public static List<String> names(String list) {
		List<String> names = new ArrayList<>();
		if (list != null) {
			for (String entry : list.split(",")) {
				String name = nameOf(entry);
				if (!name.isEmpty()) {
					names.add(name);
				}
			}
		}
		return names;
	}

	/** True when this name is in the list more than once. */
	public static boolean listedTwice(String list, String name) {
		return names(list).stream().filter(name::equals).count() > 1;
	}

	/**
	 * The list with {@code name} changed to {@code newName}, or removed when {@code newName} is
	 * null. Where that leaves a name twice, one entry stays at the higher weight, in the first
	 * one's place. Other entries are left as they are. A list without the name comes back
	 * unchanged, character for character.
	 */
	public static String replace(String list, String name, String newName) {
		if (!names(list).contains(name)) {
			return list;
		}
		Map<String, String> entries = new LinkedHashMap<>(); // name -> weight, null when none
		for (String entry : list.split(",")) {
			String entryName = nameOf(entry);
			if (entryName.isEmpty()) {
				continue;
			}
			String target = entryName.equals(name) ? newName : entryName;
			if (target != null) {
				entries.merge(target, weightOf(entry) == null ? "" : weightOf(entry), ServiceListEdit::higher);
			}
		}
		List<String> out = new ArrayList<>();
		entries.forEach((n, w) -> out.add(w.isEmpty() ? n : n + ":" + w));
		return String.join(",", out);
	}

	private static String nameOf(String entry) {
		int colon = entry.indexOf(':');
		return (colon < 0 ? entry : entry.substring(0, colon)).trim();
	}

	private static String weightOf(String entry) {
		int colon = entry.indexOf(':');
		return colon < 0 ? null : entry.substring(colon + 1).trim();
	}

	private static String higher(String a, String b) {
		if (a.isEmpty() || b.isEmpty()) {
			return a.isEmpty() ? b : a;
		}
		try {
			return Integer.parseInt(a) >= Integer.parseInt(b) ? a : b;
		} catch (NumberFormatException e) {
			return a;
		}
	}
}
