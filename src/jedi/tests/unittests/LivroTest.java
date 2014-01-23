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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import app.models.Autor;
import app.models.Publisher;
import app.models.Livro;
import app.models.Pais;
import app.models.Uf;

public class LivroTest {

	@Test
	public void testInsert() {
		
		Livro livroEsperado = new Livro();
		
		livroEsperado.setTitulo("O Alquimista");
		livroEsperado.setDataPublicacao("17/10/2013");
		livroEsperado.setEditora(new Publisher("Editora Abril", new Uf("São Paulo", "SP", new Pais("Brasil", "BR") ) ) );
		
		List<Autor> autores = new ArrayList<Autor>();
		autores.add(new Autor("Paulo", "Coelho", "paulocoelho@gmail.com") );
		
		livroEsperado.setAutores(autores);
		
		livroEsperado.save();
		
		Livro livroObtido = Livro.objects.get("titulo", "O Alquimista");
		
		Assert.assertEquals(livroEsperado.getId(), livroObtido.getId() );
	}

}
