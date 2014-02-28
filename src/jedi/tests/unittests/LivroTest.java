/***********************************************************************************************
 * @(#)LivroTest.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/15
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

import java.util.ArrayList;
import java.util.List;

import jedi.db.engine.JediORMEngine;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import app.models.Autor;
import app.models.Editora;
import app.models.Livro;
import app.models.Pais;
import app.models.Uf;

public class LivroTest {
	
	@BeforeClass
	public static void testSetup() {
		JediORMEngine.FOREIGN_KEY_CHECKS = false;
		JediORMEngine.flush();
	}
	
	@AfterClass
	public static void testCleanup() {
		JediORMEngine.droptables();
	}

    @Test
    public void testInsert() {
        Livro livroEsperado = new Livro();
        livroEsperado.setTitulo("O Alquimista");
        livroEsperado.setDataPublicacao("17/10/2013");
        livroEsperado.setEditora(new Editora("Editora Abril", new Uf("São Paulo", "SP", new Pais("Brasil", "BR"))));
        List<Autor> autores = new ArrayList<Autor>();
        autores.add(new Autor("Paulo", "Coelho", "paulocoelho@gmail.com"));
        livroEsperado.setAutores(autores);
        livroEsperado.save();
        Livro livroObtido = Livro.objects.get("titulo", "O Alquimista");
        Assert.assertEquals(livroEsperado.getId(), livroObtido.getId());
    }
    
    @Test
    public void testUpdate() {
    	Livro livroEsperado = Livro.objects.get("titulo", "O Alquimista");
    	livroEsperado.update("titulo='O Hobbit'");
    	Livro livroObtido = new Livro();
    	livroObtido.setTitulo("O Hobbit");
    	Assert.assertEquals(livroEsperado.getTitulo(), livroObtido.getTitulo());
    }
    
    @Test
    public void testDelete() {
    	int esperado = 0;
    	int obtido = Livro.objects.all().delete().count();
    	Assert.assertEquals(esperado, obtido);
    }
    
    @Test
    public void testSaveInsert() {
    	Livro livroEsperado = new Livro();
        livroEsperado.setTitulo("Neuromancer");
        livroEsperado.setDataPublicacao("01/07/1984");
        livroEsperado.setEditora(new Editora("Ace Books", new Uf("British Columbia", "BC", new Pais("Canadá", "CA"))));
        List<Autor> autores = new ArrayList<Autor>();
        autores.add(new Autor("Willian", "Gibson", "williangibson@gmail.com"));
        livroEsperado.setAutores(autores);
        livroEsperado.save();
        Livro livroObtido = Livro.objects.get("titulo", "Neuromancer");
        Assert.assertEquals(livroEsperado.getTitulo(), livroObtido.getTitulo());
    }
    
    @Test
    public void testSaveUpdate() {
    	Livro livroEsperado = Livro.objects.get("titulo", "Neuromancer");
    	Autor autorEsperado = (Autor) livroEsperado.getAutores().first();
    	autorEsperado.setNome("William");
    	autorEsperado.setEmail("williamgibson@gmail.com");
    	livroEsperado.getAutores().set(0, autorEsperado);
    	livroEsperado.save();
    	Livro livroObtido = Livro.objects.get("titulo", "Neuromancer");
    	Autor autorObtido = (Autor) livroObtido.getAutores().first();
    	Assert.assertEquals(autorEsperado.getNome(), autorObtido.getNome());
    }
}