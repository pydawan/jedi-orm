package jedi.db.tests.unittests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import app.models.Pais;
import app.models.Uf;

public class UfTest {
	
	@AfterClass
	public static void testCleanup() {
		
		// OBS: O CASCADE no DELETE não está funcionando.
		for (Uf uf : Uf.objects.<Uf>all() ) {
			uf.delete();
			uf.getPais().delete();
		}
	}

	@Test
	public void testInsert() {
		
		Uf ufEsperada = new Uf();		
		ufEsperada.setNome("Goiaz");
		ufEsperada.setSigla("GO");
		
		Pais pais = new Pais();
		pais.nome("Brasil");
		pais.sigla("BR");
		
		ufEsperada.setPais(pais);
		
		ufEsperada.insert();
		
		Uf ufObtida = Uf.objects.get("sigla", "GO");
		
		Assert.assertEquals(ufEsperada.getId(), ufObtida.getId() );
	}
	
	@Test
	public void testUpdate() {
		
		Uf ufEsperada = Uf.objects.get("sigla", "GO");
		
		ufEsperada.update("nome='Goiás'");
		
		Uf ufObtida = Uf.objects.get("sigla", "GO");
		
		Assert.assertTrue(ufEsperada.getNome().equals(ufObtida.getNome() ) );
	}
	
	@Test
	public void testDelete() {
		
		int esperado = 0;
		
		Pais.objects.all().delete();
		
		Uf.objects.all().delete();
		
		int obtido = Uf.objects.all().count();
		
		Assert.assertEquals(esperado, obtido);
	}
	
	@Test
	public void testSaveInsert() {
		
		Uf ufEsperada = new Uf();
		
		ufEsperada.setNome("Sao Paulo");
		ufEsperada.setSigla("SP");
		
		Pais pais = new Pais("Brasil", "BR");
		
		ufEsperada.setPais(pais);
		
		ufEsperada.save();
		
		Uf ufObtida = Uf.objects.get("sigla", "SP");
		
		Assert.assertEquals(ufEsperada.getId(), ufObtida.getId() );
	}
	
	@Test
	public void testSaveUpdate() {
		
		Uf ufEsperada = Uf.objects.get("sigla", "SP");
		
		ufEsperada.setNome("São Paulo");
		
		ufEsperada.save();
		
		Uf ufObtida = Uf.objects.get("sigla", "SP");
		
		Assert.assertTrue(ufEsperada.getNome().equals(ufObtida.getNome() ) );
		
	}

}
