package jedi.db.tests.unittests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import app.models.Autor;

public class AutorTest {
	
	@AfterClass
	public static void testCleanup() {
		
		for (Autor autor : Autor.objects.<Autor>all() ) {
			autor.delete();
		}
	}

	@Test
	public void testInsert() {
		
		Autor autorEsperado = new Autor();
		
		autorEsperado.setNome("Paulo");
		autorEsperado.setSobrenome("Coelhoo");
		autorEsperado.setEmail("paulocoelho@gmail.com");
		
		autorEsperado.save();
		
		Autor autorObtido = Autor.objects.get("email", "paulocoelho@gmail.com");
		
		Assert.assertEquals(autorEsperado.getId(), autorObtido.getId() );
	}
	
	@Test
	public void testUpdate() {
		
		Autor autorEsperado = Autor.objects.get("email", "paulocoelho@gmail.com");
		
		autorEsperado.update("sobrenome='Coelho'");
		
		Autor autorObtido = Autor.objects.get("email", "paulocoelho@gmail.com");
		
		Assert.assertTrue(autorEsperado.getSobrenome().equals(autorObtido.getSobrenome() ) );
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
		
		Assert.assertEquals(autorEsperado.getId(), autorObtido.getId() );
	}
	
	@Test
	public void testSaveUpdate() {
		
		Autor autorEsperado = Autor.objects.get("email", "jrrtolkien@gmail.com");
		
		autorEsperado.setSobrenome("Reuel Tolkien");
		
		autorEsperado.save();
		
		Autor autorObtido = Autor.objects.get("email", "jrrtolkien@gmail.com");
		
		Assert.assertTrue(autorEsperado.getSobrenome().equals(autorObtido.getSobrenome() ) );
	}

}
