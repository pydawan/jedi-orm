import java.io.File;

import jedi.db.engine.JediORMEngine;

/**
 * Classe de execução do framework Jedi.
 * 
 * @author thiago.monteiro
 *
 */
public class JediORM {
	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		
		try {
			
			// Para teste.
			//args = new String[1];
			
			//args[0] = "syncdb";
			
			if (args.length > 0 && !args[0].trim().equals("syncdb") ) {
				
				JediORMEngine.app_root_dir = args[0];
				
				JediORMEngine.app_db_config = String.format("%s%sconfig%sdatabase.properties", JediORMEngine.app_root_dir, File.separator, File.separator);

				
			} else {
			
				/* Diretório onde o framework foi invocado.
				 * Geralmente o diretório de um aplicação mas também pode ser o diretório 
				 * do próprio framework.
				 */
				JediORMEngine.app_root_dir = System.getProperty("user.dir");
				
				JediORMEngine.app_db_config = String.format("%s%sdatabase.properties", JediORMEngine.app_root_dir, File.separator);
			}

			/*
			 * Diretório onde estão localizados os códigos fonte da aplicação.
			 */
			JediORMEngine.app_src_dir = String.format("%s%ssrc", JediORMEngine.app_root_dir, File.separator);
			
			JediORMEngine.syncdb(JediORMEngine.app_src_dir);
			
		} catch (Exception e) {
			
			e.printStackTrace();			
		}
	}
}