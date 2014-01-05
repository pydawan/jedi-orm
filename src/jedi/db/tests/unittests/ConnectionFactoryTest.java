package jedi.db.tests.unittests;

import java.sql.Connection;

import jedi.db.ConnectionFactory;

import org.junit.Assert;
import org.junit.Test;

public class ConnectionFactoryTest {

	@Test
	public void testConnectH2() {
		
		// Obtendo conexão com o banco de dados H2.
		Connection conexao = ConnectionFactory.connect("engine=h2");
		
		Assert.assertNotNull(conexao);
	}
	
	@Test
	public void testConnectMySQL() {
		
		// Obtendo conexão com o banco de dados H2.
		Connection conexao = ConnectionFactory.connect("engine=mysql", "user=jedi", "password=jedi", "database=jedi");
		// Definir "upd=jedi" como sendo um atalho para "user=jedi", "password=jedi", "database=jedi" 
		//Connection conexao = ConnectionFactory.connect("engine=mysql", "upd=jedi");
		
		Assert.assertNotNull(conexao);
	}
	
}
