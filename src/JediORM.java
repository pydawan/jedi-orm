/***********************************************************************************************
 * @(#)JediORM.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/03/08
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
            if (args.length > 0) {
            	for (String arg : args) {
            		arg = arg.trim();
            		if (arg.matches("app_root_dir=\\s*\\w+\\s*")) {
            			JediORMEngine.APP_ROOT_DIR = arg;
            		} else {
            			JediORMEngine.APP_ROOT_DIR = System.getProperty("user.dir");
            		}
            		if (arg.equals("web_app")) {
            			JediORMEngine.WEB_APP = true;
            		}
            		if (arg.matches("debug=\\s*true\\s*")) {
            			JediORMEngine.DEBUG = true;
            		} else if (arg.matches("debug=\\s*false\\s*")) {
            			JediORMEngine.DEBUG = false;
            		} else {
            			
            		}
            	}
            } else {
                JediORMEngine.APP_ROOT_DIR = System.getProperty("user.dir");
            }
            if (JediORMEngine.WEB_APP) {
            	JediORMEngine.APP_SRC_DIR = String.format("%s%sweb%sWEB-INF%ssrc", 
            			JediORMEngine.APP_ROOT_DIR, File.separator, File.separator, File.separator);
            	JediORMEngine.APP_DB_CONFIG_FILE = String.format("%s%sweb%sWEB-INF%sconfig%sdatabase.properties", 
                        JediORMEngine.APP_ROOT_DIR, File.separator, File.separator, File.separator, File.separator);
            	File dbConfigFile = new File(JediORMEngine.APP_DB_CONFIG_FILE);
            	if (!dbConfigFile.exists()) {
	            	JediORMEngine.APP_DB_CONFIG_FILE = String.format("%s%sweb%sWEB-INF%sdatabase.properties", 
	                        JediORMEngine.APP_ROOT_DIR, File.separator, File.separator, File.separator);
            	}
            } else {
            	JediORMEngine.APP_SRC_DIR = String.format("%s%ssrc",
            			JediORMEngine.APP_ROOT_DIR, File.separator);
            	JediORMEngine.APP_DB_CONFIG_FILE = String.format("%s%sdatabase.properties", 
                        JediORMEngine.APP_ROOT_DIR, File.separator);
            }
            JediORMEngine.syncdb(JediORMEngine.APP_SRC_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}