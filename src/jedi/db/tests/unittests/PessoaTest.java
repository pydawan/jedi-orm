package jedi.db.tests.unittests;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import app.models.Pessoa;

public class PessoaTest {

	@AfterClass
	public static void testCleanup() {
		
		Pessoa.objects.all().delete();
	}
	
	@Test
	public void testInsert() {
		
		Pessoa pessoaEsperada = new Pessoa();
		
		pessoaEsperada.setNome("Thiago Alexandre Martins Mont");
		pessoaEsperada.setIdade(30);
		pessoaEsperada.setCpf("003.696.631-28");
		pessoaEsperada.setRg("4559419");
		pessoaEsperada.setTipoPessoa("F");
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		
		try {
			
			Date dataNascimento = sdf.parse("19/11/1982");
			
			pessoaEsperada.setDataNascimento(dataNascimento);
			
		} catch (ParseException e) {
			e.printStackTrace();
		}

		
		pessoaEsperada.insert();
		
		Pessoa pessoaObtida = Pessoa.objects.get("cpf", "003.696.631-28");
		
		Assert.assertEquals(pessoaEsperada.getId(), pessoaObtida.getId() );
	}
	
	@Test
	public void testUpdate() {
		
		Pessoa pessoaEsperada = Pessoa.objects.get("cpf", "003.696.631-28");
		
		pessoaEsperada.update("nome='Thiago Alexandre Martins Monteiro'");
		
		Pessoa pessoaObtida = Pessoa.objects.get("cpf", "003.696.631-28");
		
		Assert.assertTrue(pessoaEsperada.getNome().equals(pessoaObtida.getNome() ) );
	}
	
	@Test
	public void testDelete() {
		
		int quantidadePessoasEsperada = 0;
		
		Pessoa.objects.all().delete();
		
		int quantidadePessoasObtida = Pessoa.objects.all().count();
		
		Assert.assertEquals(quantidadePessoasEsperada, quantidadePessoasObtida);
	}
	
	@Test
	public void testSaveInsert() {
		
		Pessoa pessoaEsperada = new Pessoa();
		
		pessoaEsperada.setNome("Guido");
		pessoaEsperada.setIdade(57);
		pessoaEsperada.setCpf("777.777.777-77");
		pessoaEsperada.setRg("111111");
		pessoaEsperada.setTipoPessoa("F");
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		
		try {
			
			/* Bug 1, supostamente corrigido no Framework.
			 * No MySQL o menor ano armazenado pelo tipo TIMESTAMP é 1970 enquanto que
			 * o intervalo de DATETIME é muito maior.
			 * 
			 * Bug 2, auto_now e auto_now_add não estão funcionando corretamente.
			 * 
			 * OBS: Parece que os bugs foram corrigidos mas é bom verificar mais um pouco.
			 */
			Date dataNascimento = sdf.parse("31/01/1956");
			
			pessoaEsperada.setDataNascimento(dataNascimento);
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		pessoaEsperada.save();
		
		Pessoa pessoaObtida = Pessoa.objects.get("nome", "Guido");
		
		Assert.assertEquals(pessoaEsperada.getId(), pessoaObtida.getId() );
	}
	
	@Test
	public void testSaveUpdate() {
		
		Pessoa pessoaEsperada = Pessoa.objects.get("nome", "Guido");
		
		pessoaEsperada.setNome("Guido van Rossum");
		
		pessoaEsperada.save();
		
		Pessoa pessoaObtida = Pessoa.objects.get("nome", "Guido van Rossum");
		
		Assert.assertTrue(pessoaEsperada.getNome().equals(pessoaObtida.getNome() ) );
	}

}
