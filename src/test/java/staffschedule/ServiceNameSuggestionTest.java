package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.midco.rota.service.ServiceBulkUpdateService;

/** Names to fix: which rota location to offer for a name that matches none. */
class ServiceNameSuggestionTest {

	private static final Set<String> KNOWN = Set.of("1ST AVENUE", "FLAT 2 AUDLEY", "4 AUDLEY", "GOLDERS RISE", "TORQUAY DRIVE",
			"DOM CARE RUNS 2,");

	private static String suggest(String name) throws Exception {
		Method m = ServiceBulkUpdateService.class.getDeclaredMethod("suggest", String.class, Set.class);
		m.setAccessible(true);
		return (String) m.invoke(null, name, KNOWN);
	}

	@Test
	void offersTheSameNameSpeltDifferently() throws Exception {
		assertEquals("1ST AVENUE", suggest("FIRST AVENUE"));
		assertEquals("GOLDERS RISE", suggest("GOLDERS RISE RISE"));
		assertEquals("GOLDERS RISE", suggest("golders  rise"));
		assertEquals("TORQUAY DRIVE", suggest("TORQUAY"));
	}

	@Test
	void neverOffersADifferentNumber() throws Exception {
		assertNull(suggest("FLAT 3 AUDLEY"));
		assertNull(suggest("5 AUDLEY"));
	}

	@Test
	void neverOffersANameThatCannotGoOnAList() throws Exception {
		assertNull(suggest("DOM CARE RUNS 2"));
	}

	@Test
	void offersNothingForAStranger() throws Exception {
		assertNull(suggest("PARKROAD"));
	}
}
