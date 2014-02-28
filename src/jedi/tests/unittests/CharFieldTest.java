package jedi.tests.unittests;

import jedi.db.models.CharField;

import org.junit.Test;

public class CharFieldTest {
	@Test
	public void test() {
		CharField charField = new CharField();
		charField.setMaxLength(50);
		charField.setComment("TESTE");
	}
}