/***********************************************************************************************
 * @(#)AutorTest.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/17
 * 
 * Copyright (c) 2014 Thiago Alexandre Martins Monteiro.
 * 
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the GNU Public License v2.0 which accompanies 
 * this distribution, and is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *    Thiago Alexandre Martins Monteiro - initial API and implementation
 ************************************************************************************************/

package jedi.tests.unittests;

import jedi.db.engine.JediORMEngine;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import app.models.Autor;

public class AutorTest {

	@BeforeClass
	public static void testSetup() {
		JediORMEngine.FOREIGN_KEY_CHECKS = false;
		JediORMEngine.flush();
	}

	@AfterClass
	public static void testCleanup() {
		/*
		 * for (Autor autor : Autor.objects.<Autor> all() ) { autor.delete(); }
		 */
		JediORMEngine.droptables();
	}

	@Test
	public void testInsert() {
		Autor autorEsperado = new Autor();
		autorEsperado.setNome("Paulo");
		autorEsperado.setSobrenome("Coelhoo");
		autorEsperado.setEmail("paulocoelho@gmail.com");
		autorEsperado.save();
		Autor autorObtido = Autor.objects.get("email", "paulocoelho@gmail.com");
		Assert.assertEquals(autorEsperado.getId(), autorObtido.getId());
	}

	@Test
	public void testUpdate() {
		Autor autorEsperado = Autor.objects.get("email",
				"paulocoelho@gmail.com");
		autorEsperado.update("sobrenome='Coelho'");
		Autor autorObtido = Autor.objects.get("email", "paulocoelho@gmail.com");
		Assert.assertTrue(autorEsperado.getSobrenome().equals(
				autorObtido.getSobrenome()));
	}

	@Test
	public void testDelete() {
		int esperado = 0;
		Autor.objects.all().delete();
		int obtido = Autor.objects.all().count();
		Assert.assertEquals(esperado, obtido);
	}

	@Test
	public void testSaveInsert() {
		Autor autorEsperado = new Autor();
		autorEsperado.setNome("John Ronald");
		autorEsperado.setSobrenome("Reuel Tolkienn");
		autorEsperado.setEmail("jrrtolkien@gmail.com");
		autorEsperado.save();
		Autor autorObtido = Autor.objects.get("email", "jrrtolkien@gmail.com");
		Assert.assertEquals(autorEsperado.getId(), autorObtido.getId());
	}

	@Test
	public void testSaveUpdate() {
		Autor autorEsperado = Autor.objects
				.get("email", "jrrtolkien@gmail.com");
		autorEsperado.setSobrenome("Reuel Tolkien");
		autorEsperado.save();
		Autor autorObtido = Autor.objects.get("email", "jrrtolkien@gmail.com");
		Assert.assertTrue(autorEsperado.getSobrenome().equals(
				autorObtido.getSobrenome()));
	}
}