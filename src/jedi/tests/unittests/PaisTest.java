/*******************************************************************************
 * Copyright (c) 2014 Thiago Alexandre Martins Monteiro.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Thiago Alexandre Martins Monteiro - initial API and implementation
 ******************************************************************************/
package jedi.tests.unittests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import app.models.Pais;


public class PaisTest {
	
	@AfterClass
	public static void testCleanup() {
		
		/* Exclui todos os registros da tabela após a execução
		 * de todos os testes.
		 */
		
		// Para MySQL
		//Pais.objects.raw("SET FOREIGN_KEY_CHECKS = 0");
		
		Pais.objects.all().delete();
		
		//Pais.objects.raw("SET FOREIGN_KEY_CHECKS = 1");
	}
	
	@Test
	public void testInsert() {
		
		// Instancia um país.
		Pais paisEsperado = new Pais("Brasil", "BR");
		
		// Insere o registro na tabela.
		paisEsperado.insert();
		
		// Pesquisa pelo registro que acabou de ser inserido.
		Pais paisObtido = Pais.objects.get("nome", "Brasil");
		
		// Verifica se o id do inserido é igual ao id do consultado.
		Assert.assertEquals(paisEsperado.getId(), paisObtido.getId() );
	}
	
	@Test
	public void testUpdate() {
		
		Pais paisEsperado = Pais.objects.get("nome", "Brasil");
		
		paisEsperado.update("nome='Brazil'");
		
		Pais paisObtido = Pais.objects.get("sigla", "BR");
		
		Assert.assertTrue(paisEsperado.getNome().equals(paisObtido.getNome() ) );
	}
	
	@Test
	public void testDelete() {
		
		int esperado = 0;
		
		for (Pais pais : Pais.objects.<Pais>all() ) {
			pais.delete();
		}
		
		int obtido = Pais.objects.count();
		
		Assert.assertEquals(esperado, obtido);
	}
	
	@Test
	public void testSaveInsert() {
		
		Pais paisEsperado = new Pais();
		
		paisEsperado.setNome("Estados Unidos da América");
		paisEsperado.setSigla("UZ");
		paisEsperado.setCapital("Washington");
		paisEsperado.setContinente("Americano");
		
		paisEsperado.save();
		
		Pais paisObtido = Pais.objects.get("sigla", "UZ");
		
		Assert.assertEquals(paisEsperado.getId(), paisObtido.getId() );
	}
	
	@Test
	public void testSaveUpdate() {
		
		Pais paisEsperado = Pais.objects.get("sigla", "UZ");
		
		paisEsperado.setSigla("US");
		
		paisEsperado.save();
		
		Pais paisObtido = Pais.objects.get("sigla", "US");
		
		Assert.assertTrue(paisEsperado.getNome().equals(paisObtido.getNome() ) );
	}
	
}
