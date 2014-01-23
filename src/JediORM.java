/***********************************************************************************************
 * @(#)JediORMEngine.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/23
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

import java.io.File;

import jedi.db.engine.JediORMEngine;

/**
 * Main class of the framework.
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
			
			if (args.length > 0 && !args[0].trim().equals("syncdb") ) {
				JediORMEngine.APP_ROOT_DIR = args[0];
				JediORMEngine.APP_DB_CONFIG = String.format("%s%sconfig%sdatabase.properties", 
				        JediORMEngine.APP_ROOT_DIR, File.separator, File.separator);
			} else {
				JediORMEngine.APP_ROOT_DIR = System.getProperty("user.dir");
				JediORMEngine.APP_DB_CONFIG = String.format("%s%sdatabase.properties", 
				        JediORMEngine.APP_ROOT_DIR, File.separator);
			}
			
			JediORMEngine.APP_SRC_DIR = String.format("%s%ssrc", 
			        JediORMEngine.APP_ROOT_DIR, File.separator);
			JediORMEngine.syncdb(JediORMEngine.APP_SRC_DIR);
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
}
