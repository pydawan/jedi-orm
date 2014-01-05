package jedi.db.tests.unittests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import app.models.Autor;
import app.models.Editora;
import app.models.Livro;
import app.models.Pais;
import app.models.Uf;

public class LivroTest {

	@Test
	public void testInsert() {
		
		Livro livroEsperado = new Livro();
		
		livroEsperado.setTitulo("O Alquimista");
		livroEsperado.setDataPublicacao("17/10/2013");
		livroEsperado.setEditora(new Editora("Editora Abril", new Uf("São Paulo", "SP", new Pais("Brasil", "BR") ) ) );
		
		List<Autor> autores = new ArrayList<Autor>();
		autores.add(new Autor("Paulo", "Coelho", "paulocoelho@gmail.com") );
		
		livroEsperado.setAutores(autores);
		
		livroEsperado.save();
		
		Livro livroObtido = Livro.objects.get("titulo", "O Alquimista");
		
		Assert.assertEquals(livroEsperado.getId(), livroObtido.getId() );
	}

}
