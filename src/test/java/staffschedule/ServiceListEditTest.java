package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.util.ServiceListEdit;

/** Bulk update edits carers' lists entry by entry. */
class ServiceListEditTest {

	@Test
	void renameKeepsTheWeightAndOtherEntries() {
		assertEquals("RIVERDALE:100,1ST AVENUE:25,9 CLAYDON:10",
				ServiceListEdit.replace("RIVERDALE:100,FIRST AVENUE:25,9 CLAYDON:10", "FIRST AVENUE", "1ST AVENUE"));
	}

	@Test
	void aShortNameInsideALongerOneIsLeftAlone() {
		String list = "FLAT 2 AUDLEY:40,4 AUDLEY:60";
		assertEquals(list, ServiceListEdit.replace(list, "2 AUDLEY", "X"));
		assertEquals("FLAT 2 AUDLEY:40,X:60", ServiceListEdit.replace(list, "4 AUDLEY", "X"));
	}

	@Test
	void mergeKeepsTheHigherWeightInTheFirstPlace() {
		assertEquals("1ST AVENUE:70,RIVERDALE:5", ServiceListEdit.replace("FIRST AVENUE:25,RIVERDALE:5,1ST AVENUE:70", "FIRST AVENUE", "1ST AVENUE"));
		assertEquals("RIVERDALE:5,1ST AVENUE:70", ServiceListEdit.replace("RIVERDALE:5,1ST AVENUE:60,FIRST AVENUE:70", "FIRST AVENUE", "1ST AVENUE"));
	}

	@Test
	void mergingANameOntoItselfClearsARepeat() {
		String list = "Alexandra Flat B - LC:11,Alexandra Flat B - LC :30,9 CLAYDON:10";
		assertTrue(ServiceListEdit.listedTwice(list, "Alexandra Flat B - LC"));
		assertEquals("Alexandra Flat B - LC:30,9 CLAYDON:10",
				ServiceListEdit.replace(list, "Alexandra Flat B - LC", "Alexandra Flat B - LC"));
	}

	@Test
	void removeTakesOutTheEntry() {
		assertEquals("8 BARFORD:70", ServiceListEdit.replace("8 BARFORD:70,Cober Cottage:16", "Cober Cottage", null));
		assertEquals("", ServiceListEdit.replace("Cober Cottage:16", "Cober Cottage", null));
	}

	@Test
	void restrictedListsHaveNoWeights() {
		assertEquals("GOLDERS RISE,FLAT 3 AUDLEY",
				ServiceListEdit.replace("GOLDERS RISE RISE, FLAT 3 AUDLEY", "GOLDERS RISE RISE", "GOLDERS RISE"));
	}

	@Test
	void aListWithoutTheNameComesBackExactly() {
		String list = " 9 CLAYDON:50, RIVERDALE:25";
		assertEquals(list, ServiceListEdit.replace(list, "57 STURGESS", "X"));
		assertNull(ServiceListEdit.replace(null, "57 STURGESS", "X"));
	}

	@Test
	void namesAreTrimmedAndEmptyListsAreEmpty() {
		assertEquals(List.of("9 CLAYDON", "RIVERDALE"), ServiceListEdit.names(" 9 CLAYDON:50, RIVERDALE :25,"));
		assertTrue(ServiceListEdit.names(null).isEmpty());
		assertFalse(ServiceListEdit.listedTwice("A:1,B:2", "A"));
	}
}
