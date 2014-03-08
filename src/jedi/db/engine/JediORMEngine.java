/***********************************************************************************************
 * @(#)JediORMEngine.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/03/07
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

package jedi.db.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import jedi.db.ConnectionFactory;
import jedi.db.FetchType;
import jedi.db.Models;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.BooleanField;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.DateTimeField;
import jedi.db.annotations.fields.DecimalField;
import jedi.db.annotations.fields.EmailField;
import jedi.db.annotations.fields.FloatField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.IntegerField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.annotations.fields.OneToOneField;
import jedi.db.annotations.fields.TextField;
import jedi.db.annotations.fields.TimeField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;
import jedi.db.util.TableUtil;

/**
 * Jedi's Object-Relational Mapping Engine.
 * 
 * @version 1.0 08 Jan 2014
 * @author Thiago Alexandre Martins Monteiro
 * 
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JediORMEngine {
	private static Properties databaseSettings;
	private static Connection connection;
	private static String databaseEngine;
	private static Manager SQLManager = new Manager(Model.class, getConnection(), false);
	
	public static String DATABASE_ENGINE;
	public static boolean FOREIGN_KEY_CHECKS = true;	
    // Application's root directory.
    public static String APP_ROOT_DIR = System.getProperty("user.dir");
    // Application's source code directory.
    public static String APP_SRC_DIR = String.format(
		"%s%ssrc", 
		APP_ROOT_DIR, File.separator
	);
    public static String APP_DB_CONFIG_FILE = String.format(
		"%s%sdatabase.properties", 
		JediORMEngine.APP_ROOT_DIR, 
		File.separator
	);
    public static String FETCH_TYPE = FetchType.EAGER.getValue();
    public static boolean WEB_APP = false;
    public static boolean DEBUG = true;
    // Framework's model directory.
    public static final String JEDI_DB_MODELS = String.format(
		"jedi%sdb%smodels", 
		File.separator, 
		File.separator,  
		File.separator
	);
    // Application's models that were read and that will be mapped in tables.
    public static List<String> readedAppModels = new ArrayList<String>();
    // Generated tables.
    public static List<String> generatedTables = new ArrayList<String>();
    
    /**
     * Converts model objects of a application in database tables.
     * 
     * @author Thiago Alexandre Martins Monteiro
     * @param path
     */
    public static void syncdb(String path) {
        // Reference to the application's directory.
        File appDir = new File(path);
        // Checks if the appDir exists.
        if (appDir != null && appDir.exists() ) {
            // Checks if appDir is a directory and not a 
            // file (both are referenced as a File object).
            if (appDir.isDirectory() ) {
                // Gets the appDir content.
                File[] appDirContents = appDir.listFiles();
                // Search by app/models subdirectory in the appDir content.
                for (File appDirContent : appDirContents) {
                    // Gets all directories named as "models" (except the models directory 
                    // of the Framework).
                    if (!appDirContent.getAbsolutePath().contains(JediORMEngine.JEDI_DB_MODELS) && 
                		appDirContent.getAbsolutePath().endsWith("models") ) {
                        // Gets all files in app/models.
                        File[] appModelsFiles = appDirContent.listFiles();
                        // Strings for SQL statements generation.
                        // General SQL.
                        String sql = "";
                        // SQL for the indexes generation.
                        String sqlIndex = "";
                        // SQL for the foreign key generation.
                        String sqlForeignKey = "";
                        // SQL for the generation of association tables (many to many tables).
                        String sqlManyToManyAssociation = "";
                        // SQL for the ORACLE® auto increment triggers generation.
                        Map<String, String> sqlOracleAutoIncrementTriggers = new HashMap<String, String>();
                        // SQL for the ORACLE® sequences generation.
                        String sqlOracleSequences = "";
                        // SQL for the MySQL DATETIME triggers generation.
                        List<String> mySQLDateTimeTriggers = new ArrayList<String>();
                        /* Intermediate models in many-to-many relationship.
                         * This model govern the relationship and hold extra fields.
                         */
                        List<String> intermediateModels = new ArrayList<String>();
                        Map<String, List<String>> mySQLAutoNow = new HashMap<String, List<String>>();
                        Map<String, List<String>> mySQLAutoNowAdd = new HashMap<String, List<String>>();                        
                        int mysqlVersionNumber = 0;                        
                        for (File appModelFile : appModelsFiles) {
                        	String modelClassName = appModelFile.getAbsolutePath();
                            if (!modelClassName.endsWith("java")) {
                                continue;
                            }
                            modelClassName = modelClassName.replace(
                        		String.format(
                    				"%s%ssrc%s", 
                    				JediORMEngine.APP_ROOT_DIR, 
                    				File.separator, 
                    				File.separator
                				), 
                				""
            				);
                            modelClassName = modelClassName.replace(File.separator, ".").replace(".java", "");
                            JediORMEngine.readedAppModels.add(modelClassName);                                
                            Class modelClass = null;
							try {
								modelClass = Class.forName(modelClassName);
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}                                
                            if (!jedi.db.models.Model.class.isAssignableFrom(modelClass)) {
                                continue;
                            }
                            for (Field field : modelClass.getDeclaredFields()) {
                            	ManyToManyField manyToManyFieldAnnotation = field.getAnnotation(ManyToManyField.class);
                            	if (manyToManyFieldAnnotation != null) {
                            		String through = manyToManyFieldAnnotation.through();
                            		if (through != null && !through.trim().isEmpty()) {
                            			intermediateModels.add(through);
                            		}
                            	} 
                            }
                        }
                        connection = ConnectionFactory.getConnection();
                        Statement statement = null;
                        try {
                            // Disable the auto-commit.
                            connection.setAutoCommit(false);
                            statement = connection.createStatement();
                            databaseSettings = new Properties();
                            FileInputStream fileInputStream = new FileInputStream(APP_DB_CONFIG_FILE);
                            databaseSettings.load(fileInputStream);
                            databaseEngine = databaseSettings.getProperty("database.engine");
                            databaseEngine = databaseEngine.trim();
                            DATABASE_ENGINE = databaseEngine;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // ORM Mapping - generates the database structure 
                        // for each model class of the application.
                        for (File appModelFile : appModelsFiles) {
                            try {
                                String modelClassName = appModelFile.getAbsolutePath();
                                // Ignore files that doesn't end with the .java extension.
                                if (!modelClassName.endsWith("java")) {
                                    continue;
                                }
                                modelClassName = modelClassName.replace(
                            		String.format(
                        				"%s%ssrc%s", 
                                		JediORMEngine.APP_ROOT_DIR, 
                                		File.separator, 
                                		File.separator
                            		), 
                            		""
                        		);
                                modelClassName = modelClassName.replace(File.separator, ".").replace(".java", "");
                                JediORMEngine.readedAppModels.add(modelClassName);                                
                                // A model class reference.
                                Class modelClass = Class.forName(modelClassName);                                
                                // Checks if the class is a subclass of model. 
                                if (!jedi.db.models.Model.class.isAssignableFrom(modelClass)) {
                                    continue;
                                }                                
                                String tableName = TableUtil.getTableName(modelClass);;                                
                                // Obtendo a anotação da classe de modelo.
                                Table tableAnnotation = (Table) modelClass.getAnnotation(Table.class);
                                if (!generatedTables.contains(tableName)) {
                                	generatedTables.add(tableName);
                                }
                                if (databaseEngine.trim().equalsIgnoreCase("mysql") || 
                                    databaseEngine.trim().equalsIgnoreCase("h2")) {
                                    sql += String.format(
                                        "CREATE TABLE IF NOT EXISTS %s (\n", 
                                        tableName
                                    );                                    
                                    sql += "    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n";                                    
                                } else {
                                    sql += String.format("CREATE TABLE %s (\n", tableName);
                                }
                                if (databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                	sql += "    id SERIAL NOT NULL PRIMARY KEY,\n";                                    
                                } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                	sql += "    id NUMBER(10,0) NOT NULL PRIMARY KEY,\n";                                	
                                    StringBuilder oracleSequence = new StringBuilder();
                                    oracleSequence.append("CREATE SEQUENCE seq_%s MINVALUE 1 ");
                                    oracleSequence.append("MAXVALUE 999999999999999999999999999 ");
                                    oracleSequence.append("INCREMENT BY 1 START WITH 1 CACHE 20 ");
                                    oracleSequence.append("NOORDER NOCYCLE;");
                                    oracleSequence.append("\n\n");                                    
                                    sqlOracleSequences += String.format(
                                        oracleSequence.toString(),
                                        tableName
                                    );                                    
                                    sqlOracleAutoIncrementTriggers.put(
                                        tableName, String.format("tgr_autoincr_%s", tableName)
                                    );
                                }
                                String postgresqlOrOracleColumnsComments = "";                                
                                for (Field field : modelClass.getDeclaredFields()) {
                                    if (field.getName().equals("serialVersionUID")) {
                                        continue;
                                    }
                                    for (Annotation fieldAnnotation : field.getAnnotations()) {
                                        if (fieldAnnotation instanceof CharField) {
                                            CharField charFieldAnnotation = (CharField) fieldAnnotation;                                            
                                            if (databaseEngine.equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    charFieldAnnotation.max_length(),
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        charFieldAnnotation.default_value() != null && 
                                                        !charFieldAnnotation.default_value().equals("\\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        charFieldAnnotation.comment() != null && 
                                                        !charFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        charFieldAnnotation
                                                        	.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    charFieldAnnotation.max_length(),
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        charFieldAnnotation.default_value() != null && 
                                                        !charFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (charFieldAnnotation != null && 
                                            		charFieldAnnotation.comment() != null && 
                                            		!charFieldAnnotation.comment().trim().isEmpty()) {                                                    
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        charFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s VARCHAR2(%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    charFieldAnnotation.max_length(),
                                                    (
                                                        charFieldAnnotation.default_value() != null && 
                                                        !charFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (charFieldAnnotation != null && charFieldAnnotation.comment() != null 
                                            		&& !charFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        charFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2")) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    charFieldAnnotation.max_length(),
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        charFieldAnnotation.default_value() != null && 
                                                        !charFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }                                        
                                        if (fieldAnnotation instanceof EmailField) {
                                            EmailField emailFieldAnnotation = (EmailField) fieldAnnotation;                                            
                                            if (databaseEngine.equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    emailFieldAnnotation.max_length(),
                                                    emailFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        emailFieldAnnotation.default_value() != null && 
                                                        !emailFieldAnnotation.default_value().equals("\\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        emailFieldAnnotation.default_value() 
                                                    ) : "",
                                                    emailFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        emailFieldAnnotation.comment() != null && 
                                                        !emailFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        emailFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    emailFieldAnnotation.max_length(),
                                                    emailFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        emailFieldAnnotation.default_value() != null && 
                                                        !emailFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        emailFieldAnnotation.default_value() 
                                                    ) : "",
                                                    emailFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (emailFieldAnnotation != null && 
                                            		emailFieldAnnotation.comment() != null && 
                                            		!emailFieldAnnotation.comment().trim().isEmpty()) {                                                    
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        emailFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s VARCHAR2(%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    emailFieldAnnotation.max_length(),
                                                    (
                                                        emailFieldAnnotation.default_value() != null && 
                                                        !emailFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        emailFieldAnnotation.default_value() 
                                                    ) : "",
                                                    emailFieldAnnotation.required() ? " NOT NULL" : "",
                                                    emailFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (emailFieldAnnotation != null && 
                                            		emailFieldAnnotation.comment() != null && 
                                            		!emailFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        emailFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2") ) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    emailFieldAnnotation.max_length(),
                                                    emailFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        emailFieldAnnotation.default_value() != null && 
                                                        !emailFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        emailFieldAnnotation.default_value() 
                                                    ) : "",
                                                    emailFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof TextField) {
                                            TextField textFieldAnnotation = (TextField) fieldAnnotation;                                            
                                            if (databaseEngine.equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    "    %s TEXT%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    textFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        textFieldAnnotation.default_value() != null && 
                                                        !textFieldAnnotation.default_value().equals("\\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        textFieldAnnotation.default_value() 
                                                    ) : "",
                                                    textFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        textFieldAnnotation.comment() != null && 
                                                        !textFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        textFieldAnnotation
                                                        	.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s TEXT%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    textFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        textFieldAnnotation.default_value() != null && 
                                                        !textFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        textFieldAnnotation.default_value() 
                                                    ) : "",
                                                    textFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (textFieldAnnotation != null && 
                                            		textFieldAnnotation.comment() != null && 
                                            		!textFieldAnnotation.comment().trim().isEmpty()) {                                                    
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        textFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s TEXT%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    (
                                                        textFieldAnnotation.default_value() != null && 
                                                        !textFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        textFieldAnnotation.default_value() 
                                                    ) : "",
                                                    textFieldAnnotation.required() ? " NOT NULL" : "",
                                                    textFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (textFieldAnnotation != null && textFieldAnnotation.comment() != null 
                                            		&& !textFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        textFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2")) {
                                                sql += String.format(
                                                    "    %s BLOB%s%s,\n",
                                                    TableUtil.getColumnName(field),                                                    
                                                    textFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        textFieldAnnotation.default_value() != null && 
                                                        !textFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        textFieldAnnotation.default_value() 
                                                    ) : "",
                                                    textFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof IntegerField) {
                                            IntegerField integerFieldAnnotation = (IntegerField) fieldAnnotation;                                            
                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    "    %s INT(%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    integerFieldAnnotation.size(),
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !integerFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                            			String.format(
                                        					" DEFAULT %s",
                                        					integerFieldAnnotation.default_value()
                                    					) : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        integerFieldAnnotation.comment() != null && 
                                                        !integerFieldAnnotation.comment().equals("") && 
                                                        databaseEngine.trim().equalsIgnoreCase("mysql") 
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        integerFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s INT%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !integerFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                            		String.format(
                                        				" DEFAULT %s",
                                        				integerFieldAnnotation.default_value()
                                    				) : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s NUMBER(%d,0) %s%s%s,\n", 
                                                    TableUtil.getColumnName(field),
                                                    integerFieldAnnotation.size(),
                                                    !integerFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                            			String.format(
                                        					" DEFAULT %s",
                                        					integerFieldAnnotation.default_value()
                                    					) : "",
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2")) {
                                                sql += String.format(
                                                    "    %s INT(%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    integerFieldAnnotation.size(),
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !integerFieldAnnotation
                                                    	.default_value()
                                                		.trim()
                                                    	.isEmpty() ? String.format(
                                                			" DEFAULT %s",
                                                			integerFieldAnnotation.default_value()
                                            			) : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof DecimalField) {
                                            DecimalField decimalFieldAnnotation = (DecimalField) fieldAnnotation;
                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !decimalFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? String.format(
                                                			" DEFAULT %s",
                                                			decimalFieldAnnotation.default_value()
                                            			) : "",
                                                    decimalFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        decimalFieldAnnotation.comment() != null && 
                                                        !decimalFieldAnnotation.comment().equals("") && 
                                                        databaseEngine.trim().equalsIgnoreCase("mysql")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        decimalFieldAnnotation .comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !decimalFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                        			String.format(
                                            			" DEFAULT %s",
                                            			decimalFieldAnnotation.default_value()
                                        			) : "",
                                                    decimalFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        decimalFieldAnnotation.comment() != null
                                                        && !decimalFieldAnnotation.comment().equals("") 
                                                        && databaseEngine.trim().equalsIgnoreCase("mysql")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        decimalFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s NUMERIC(%d,%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    !decimalFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                        			String.format(
                                            			" DEFAULT %s",
                                            			decimalFieldAnnotation.default_value()
                                        			) : "",
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    decimalFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        decimalFieldAnnotation.comment() != null && 
                                                        !decimalFieldAnnotation.comment().equals("") && 
                                                        databaseEngine.trim().equalsIgnoreCase("mysql")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        decimalFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2")) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !decimalFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                        			String.format(
                                                        " DEFAULT %s",
                                                        decimalFieldAnnotation.default_value()
                                                    ) : "",
                                                    decimalFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof FloatField) {
                                            FloatField floatFieldAnnotation = (FloatField) fieldAnnotation;
                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    "    %s FLOAT(%d,%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    floatFieldAnnotation.precision(),
                                                    floatFieldAnnotation.scale(),
                                                    floatFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !floatFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? String.format(
                                                			" DEFAULT %s",
                                                			floatFieldAnnotation.default_value()
                                            			) : "",
                                                    floatFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        floatFieldAnnotation.comment() != null && 
                                                        !floatFieldAnnotation.comment().equals("") && 
                                                        databaseEngine.trim().equalsIgnoreCase("mysql")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        floatFieldAnnotation .comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s REAL(%d,%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    floatFieldAnnotation.precision(),
                                                    floatFieldAnnotation.scale(),
                                                    floatFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !floatFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                        			String.format(
                                            			" DEFAULT %s",
                                            			floatFieldAnnotation.default_value()
                                        			) : "",
                                                    floatFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        floatFieldAnnotation.comment() != null
                                                        && !floatFieldAnnotation.comment().equals("") 
                                                        && databaseEngine.trim().equalsIgnoreCase("mysql")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        floatFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s NUMERIC(%d,%d)%s%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    floatFieldAnnotation.precision(),
                                                    floatFieldAnnotation.scale(),
                                                    !floatFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                        			String.format(
                                            			" DEFAULT %s",
                                            			floatFieldAnnotation.default_value()
                                        			) : "",
                                                    floatFieldAnnotation.required() ? " NOT NULL" : "",
                                                    floatFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        floatFieldAnnotation.comment() != null && 
                                                        !floatFieldAnnotation.comment().equals("") && 
                                                        databaseEngine.trim().equalsIgnoreCase("mysql")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        floatFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2")) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    floatFieldAnnotation.precision(),
                                                    floatFieldAnnotation.scale(),
                                                    floatFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !floatFieldAnnotation
                                                    	.default_value()
                                                    	.trim()
                                                    	.isEmpty() ? 
                                        			String.format(
                                                        " DEFAULT %s",
                                                        floatFieldAnnotation.default_value()
                                                    ) : "",
                                                    floatFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof BooleanField) {
                                        	BooleanField booleanFieldAnnotation = (BooleanField) fieldAnnotation;
                                        	String columnType = "";
                                        	String comment = booleanFieldAnnotation.comment();
                                        	comment = comment != null && !comment.trim().isEmpty() ? comment.trim() : "";
                                        	if (databaseEngine.equalsIgnoreCase("mysql")) {
                                        		columnType = "TINYINT(1)";
                                        	} else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                        		columnType = "SMALLINT(1)";
                                        	}
                                        	sql += String.format(
                                    			"    %s %s%s%s%s%s,\n",
                                    			TableUtil.getColumnName(field),
                                    			columnType,
                                    			booleanFieldAnnotation.required() == true ? " NOT NULL" : "",
                                    			booleanFieldAnnotation.unique() == true ? " UNIQUE" : "",
                            					booleanFieldAnnotation.default_value() == true ? " DEFAULT 1" : " DEFAULT 0",
                    							databaseEngine.equalsIgnoreCase("mysql") && !comment.isEmpty() ? String.format(" COMMENT '%s'", comment) : ""
                                			);
                                        	if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                        		postgresqlOrOracleColumnsComments += String.format(
                                        			"COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                        			tableName,
                                        			TableUtil.getColumnName(field)                                        			
                                    			);
                                        	}
                                        }
                                        if (fieldAnnotation instanceof DateField) {
                                            DateField dateFieldAnnotation = (DateField) fieldAnnotation;
                                            String format = "    %s DATE%s%s%s%s,\n";
                                            String defaultDate = dateFieldAnnotation.default_value();
                                            defaultDate = defaultDate.isEmpty() ? "" : 
                                            String.format(" DEFAULT '%s'", defaultDate.trim().toUpperCase());
                                            defaultDate = defaultDate.replace("'NULL'", "NULL");
                                            if (defaultDate.contains("NULL")) {
                                                defaultDate = defaultDate.replace("DEFAULT", "");
                                            }                                                                                           
                                        	String fieldName = TableUtil.getColumnName(field);
                                            if (dateFieldAnnotation.auto_now_add()) {
                                                if (mySQLAutoNowAdd.get(tableName) == null) {
                                                    mySQLAutoNowAdd.put(tableName, new ArrayList<String>());
                                                    mySQLAutoNowAdd.get(tableName).add(fieldName);
                                                } else {
                                                    mySQLAutoNowAdd.get(tableName).add(fieldName);
                                                }
                                            }
                                            if (dateFieldAnnotation.auto_now()) {
                                                if (mySQLAutoNow.get(tableName) == null) {
                                                    mySQLAutoNow.put(tableName, new ArrayList<String>());
                                                    mySQLAutoNow.get(tableName).add(fieldName);
                                                } else {
                                                    mySQLAutoNow.get(tableName).add(fieldName);
                                                }
                                            }
                                            if (databaseEngine.equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    format,
                                                    TableUtil.getColumnName(field),
                                                    dateFieldAnnotation.required() ? " NOT NULL" : "",
                                                    defaultDate, 
                                                    dateFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        dateFieldAnnotation.comment() != null && 
                                                        !dateFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        dateFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s DATE%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    dateFieldAnnotation.required() ? " NOT NULL" : "",
                                                    dateFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        dateFieldAnnotation
                                                        	.default_value() != null && 
                                                        !dateFieldAnnotation
                                                        	.default_value()
                                                        	.trim()
                                                        	.equals("\0") && 
                                                        !dateFieldAnnotation
                                                        	.default_value()
                                                        	.trim()
                                                        	.equals("")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        dateFieldAnnotation.default_value() 
                                                    ) : ""
                                                );
                                                if (dateFieldAnnotation != null
                                                    && dateFieldAnnotation.comment() != null
                                                    && !dateFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        dateFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("oracle") ) {
                                                sql += String.format(
                                                    "    %s DATE%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    (
                                                        dateFieldAnnotation.default_value() != null 
                                                        && !dateFieldAnnotation.default_value().equals("\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        dateFieldAnnotation.default_value() 
                                                    ) : "",
                                                    dateFieldAnnotation.required() ? " NOT NULL" : "",
                                                    dateFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (dateFieldAnnotation != null && 
                                            		dateFieldAnnotation.comment() != null && 
                                            		!dateFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        dateFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("h2")) {
                                                format = "    %s TIMESTAMP%s%s%s,\n";
                                                if (dateFieldAnnotation.auto_now()) {
                                                    defaultDate = " AS CURRENT_DATE()";
                                                } else {
                                                    if (dateFieldAnnotation.auto_now_add()) {
                                                        defaultDate = " DEFAULT CURRENT_DATE";
                                                    }
                                                }
                                                sql += String.format(
                                                    format,
                                                    TableUtil.getColumnName(field),
                                                    dateFieldAnnotation.required() ? " NOT NULL" : "",
                                                    defaultDate,
                                                    dateFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof TimeField) {
                                            TimeField timeFieldAnnotation = (TimeField) fieldAnnotation;
                                            String format = "    %s TIME%s%s%s%s,\n";
                                            String defaultTime = timeFieldAnnotation.default_value();
                                            defaultTime = defaultTime.isEmpty() ? "" : 
                                            String.format(" DEFAULT '%s'", defaultTime.trim().toUpperCase());
                                            defaultTime = defaultTime.replace("'NULL'", "NULL");
                                            if (defaultTime.contains("NULL")) {
                                                defaultTime = defaultTime.replace("DEFAULT", "");
                                            }                                                                                           
                                        	String fieldName = TableUtil.getColumnName(field);
                                            if (timeFieldAnnotation.auto_now_add()) {
                                                if (mySQLAutoNowAdd.get(tableName) == null) {
                                                    mySQLAutoNowAdd.put(tableName, new ArrayList<String>());
                                                    mySQLAutoNowAdd.get(tableName).add(fieldName);
                                                } else {
                                                    mySQLAutoNowAdd.get(tableName).add(fieldName);
                                                }
                                            }
                                            if (timeFieldAnnotation.auto_now()) {
                                                if (mySQLAutoNow.get(tableName) == null) {
                                                    mySQLAutoNow.put(tableName, new ArrayList<String>());
                                                    mySQLAutoNow.get(tableName).add(fieldName);
                                                } else {
                                                    mySQLAutoNow.get(tableName).add(fieldName);
                                                }
                                            }
                                            if (databaseEngine.equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    format,
                                                    TableUtil.getColumnName(field),
                                                    timeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    defaultTime, 
                                                    timeFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        timeFieldAnnotation.comment() != null && 
                                                        !timeFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        timeFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s TIME%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    timeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    timeFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        timeFieldAnnotation
                                                        	.default_value() != null && 
                                                        !timeFieldAnnotation
                                                        	.default_value()
                                                        	.trim()
                                                        	.equals("\0") && 
                                                        !timeFieldAnnotation
                                                        	.default_value()
                                                        	.trim()
                                                        	.equals("")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        timeFieldAnnotation.default_value() 
                                                    ) : ""
                                                );
                                                if (timeFieldAnnotation != null
                                                    && timeFieldAnnotation.comment() != null
                                                    && !timeFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        timeFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("oracle") ) {
                                                sql += String.format(
                                                    "    %s DATE%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    (
                                                        timeFieldAnnotation.default_value() != null 
                                                        && !timeFieldAnnotation.default_value().equals("\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        timeFieldAnnotation.default_value() 
                                                    ) : "",
                                                    timeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    timeFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (timeFieldAnnotation != null && 
                                            		timeFieldAnnotation.comment() != null && 
                                            		!timeFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        timeFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("h2")) {
                                                format = "    %s TIME%s%s%s,\n";
                                                if (timeFieldAnnotation.auto_now()) {
                                                    defaultTime = " AS CURRENT_DATE()";
                                                } else {
                                                    if (timeFieldAnnotation.auto_now_add()) {
                                                        defaultTime = " DEFAULT CURRENT_DATE";
                                                    }
                                                }
                                                sql += String.format(
                                                    format,
                                                    TableUtil.getColumnName(field),
                                                    timeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    defaultTime,
                                                    timeFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }
                                        if (fieldAnnotation instanceof DateTimeField) {
                                            DateTimeField dateTimeFieldAnnotation = (DateTimeField) fieldAnnotation;
                                            String format = "    %s DATETIME%s%s%s%s%s,\n";
                                            String defaultDate = dateTimeFieldAnnotation
                                        		.default_value()
                                        		.isEmpty() ? "" : 
                                    			String.format(
                                					" DEFAULT '%s'", 
                                					dateTimeFieldAnnotation
                                						.default_value()
                                						.trim()
                                						.toUpperCase()
                            					);
                                            defaultDate = defaultDate.replace("'NULL'", "NULL");
                                            if (defaultDate.contains("NULL")) {
                                                defaultDate = defaultDate.replace("DEFAULT", "");
                                            }
                                            if (dateTimeFieldAnnotation.auto_now_add() || dateTimeFieldAnnotation.auto_now()) {
                                                if (databaseEngine.equals("mysql")) {
                                                    Manager manager = new Manager(modelClass);
                                                    String[] mysqlVersion = manager.raw("SELECT VERSION()")
                                                        .get(0)
                                                        .get(0)
                                                        .get("VERSION()")
                                                        .toString()
                                                        .split("\\.");
                                                    mysqlVersionNumber = Integer.parseInt(
                                                        String.format(
                                                            "%s%s", 
                                                            mysqlVersion[0],
                                                            mysqlVersion[1]
                                                        )
                                                    );
                                                    if (mysqlVersionNumber >= 56 && dateTimeFieldAnnotation.auto_now_add()) {
                                                        defaultDate = String.format(
                                                            " DEFAULT CURRENT_TIMESTAMP",
                                                            dateTimeFieldAnnotation.default_value()
                                                        );
                                                    }
                                                } else {
                                                    format = "    %s TIMESTAMP%s%s%s%s%s,\n";
                                                }
                                                if (mysqlVersionNumber < 56) {
                                                    String fieldName = TableUtil.getColumnName(field);
                                                    if (dateTimeFieldAnnotation.auto_now_add()) {
                                                        if (mySQLAutoNowAdd.get(tableName) == null) {
                                                            mySQLAutoNowAdd.put(tableName, new ArrayList<String>());
                                                            mySQLAutoNowAdd.get(tableName).add(fieldName);
                                                        } else {
                                                            mySQLAutoNowAdd.get(tableName).add(fieldName);
                                                        }
                                                    }
                                                    if (dateTimeFieldAnnotation.auto_now()) {
                                                        if (mySQLAutoNow.get(tableName) == null) {
                                                            mySQLAutoNow.put(tableName, new ArrayList<String>());
                                                            mySQLAutoNow.get(tableName).add(fieldName);
                                                        } else {
                                                            mySQLAutoNow.get(tableName).add(fieldName);
                                                        }
                                                    }
                                                }
                                            }
                                            if (databaseEngine.equalsIgnoreCase("mysql")) {
                                                sql += String.format(
                                                    format,
                                                    TableUtil.getColumnName(field),
                                                    dateTimeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    defaultDate,
                                                    dateTimeFieldAnnotation.auto_now() && 
                                                    (
                                                        mysqlVersionNumber >= 56 || !databaseEngine.equals("mysql") 
                                                    ) ? String.format(
                                                        " ON UPDATE CURRENT_TIMESTAMP"
                                                    ) : "", 
                                                    dateTimeFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        dateTimeFieldAnnotation.comment() != null && 
                                                        !dateTimeFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        dateTimeFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                                                sql += String.format(
                                                    "    %s TIMESTAMP%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    dateTimeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    dateTimeFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        dateTimeFieldAnnotation.default_value() != null && 
                                                        !dateTimeFieldAnnotation
                                                        	.default_value()
                                                        	.trim()
                                                        	.equals("\0") && 
                                                        !dateTimeFieldAnnotation
                                                        	.default_value()
                                                        	.trim()
                                                        	.equals("")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        dateTimeFieldAnnotation.default_value() 
                                                    ) : ""
                                                );
                                                if (dateTimeFieldAnnotation != null && 
                                            		dateTimeFieldAnnotation.comment() != null && 
                                            		!dateTimeFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        dateTimeFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                                                sql += String.format(
                                                    "    %s DATETIME%s%s%s,\n",
                                                    TableUtil.getColumnName(field),
                                                    (
                                                        dateTimeFieldAnnotation.default_value() != null && 
                                                        !dateTimeFieldAnnotation.default_value().equals("\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        dateTimeFieldAnnotation.default_value() 
                                                    ) : "",
                                                    dateTimeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    dateTimeFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                                if (dateTimeFieldAnnotation != null && 
                                                		dateTimeFieldAnnotation.comment() != null && 
                                                		!dateTimeFieldAnnotation.comment().trim().isEmpty()) {
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        TableUtil.getColumnName(field),
                                                        dateTimeFieldAnnotation.comment()
                                                    );
                                                }
                                            } else if (databaseEngine.equalsIgnoreCase("h2")) {
                                                format = "    %s TIMESTAMP%s%s%s,\n";
                                                if (dateTimeFieldAnnotation.auto_now() ) {
                                                    defaultDate = " AS CURRENT_TIMESTAMP()";
                                                } else {                                                    
                                                    if (dateTimeFieldAnnotation.auto_now_add()) {
                                                        defaultDate = " DEFAULT CURRENT_TIMESTAMP";
                                                    }
                                                }
                                                sql += String.format(
                                                    format,
                                                    TableUtil.getColumnName(field),
                                                    dateTimeFieldAnnotation.required() ? " NOT NULL" : "",
                                                    defaultDate,
                                                    dateTimeFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }                                        
                                        if (fieldAnnotation instanceof OneToOneField) {
                                            OneToOneField oneToOneFieldAnnotation = (OneToOneField) fieldAnnotation;                                            
                                            String columnName = "";
                                            String referencedColumn = "";
                                            if (oneToOneFieldAnnotation.column_name().equals("")) {
                                                columnName = TableUtil.getColumnName(field);
                                                columnName = String.format("%s_id", columnName);
                                            } else {
                                                columnName = TableUtil.getColumnName(oneToOneFieldAnnotation.column_name());
                                            }
                                            if (oneToOneFieldAnnotation.referenced_column().equals("")) {
                                                referencedColumn = "id";
                                            } else {
                                                referencedColumn = TableUtil.getColumnName(oneToOneFieldAnnotation.referenced_column());
                                            }
                                            sql += String.format(
                                                "    %s INT NOT NULL UNIQUE%s,\n",
                                                columnName,
                                                (
                                                    oneToOneFieldAnnotation.comment() != null 
                                                    && !oneToOneFieldAnnotation
                                                    	.comment()
                                                    	.equals("")  
                                                    && databaseEngine
                                                    	.trim()
                                                    	.equalsIgnoreCase("mysql") 
                                                ) ? String.format(
                                                    " COMMENT '%s'", 
                                                    oneToOneFieldAnnotation.comment() 
                                                ) : ""
                                            );
                                            String onDeleteString = "";
                                            if (oneToOneFieldAnnotation.on_delete().equals(Models.PROTECT)) {
                                                onDeleteString = " ON DELETE RESTRICT";
                                            } else if (oneToOneFieldAnnotation.on_delete().equals(Models.SET_NULL)) {
                                                onDeleteString = " ON DELETE SET NULL";
                                            } else if (oneToOneFieldAnnotation.on_delete().equals(Models.CASCADE)) {
                                                onDeleteString = " ON DELETE CASCADE";
                                            } else if (oneToOneFieldAnnotation.on_delete().equals(Models.SET_DEFAULT)) {
                                                onDeleteString = " ON DELETE SET DEFAULT";
                                            }
                                            String onUpdateString = " ON UPDATE";
                                            if (oneToOneFieldAnnotation.on_update().equals(Models.PROTECT)) {
                                                onUpdateString = " ON UPDATE RESTRICT";
                                            } else if (oneToOneFieldAnnotation.on_update().equals(Models.SET_NULL)) {
                                                onUpdateString = " ON UPDATE SET NULL";
                                            } else if (oneToOneFieldAnnotation.on_update().equals(Models.CASCADE)) {
                                                onUpdateString = " ON UPDATE CASCADE";                                                
                                                if (databaseEngine != null && databaseEngine.equalsIgnoreCase("oracle")) {
                                                    onUpdateString = "";
                                                }
                                            } else if (oneToOneFieldAnnotation.on_update().equals(Models.SET_DEFAULT)) {
                                                onUpdateString = " ON UPDATE SET DEFAULT";
                                            }
                                            String model = oneToOneFieldAnnotation.model();
                                            if (model == null || model.trim().isEmpty()) {
                                            	model = field.getType().getName().replace(field.getType().getPackage().getName() + ".", "");
                                            }
                                            String constraintName = oneToOneFieldAnnotation.constraint_name();                                            
                                            if (constraintName == null || constraintName.trim().isEmpty()) {
                                            	constraintName = String.format("fk_%s_%s", tableName, TableUtil.getTableName(field.getType()));
                                            }
                                            String references = oneToOneFieldAnnotation.references();
                                            if (references == null || references.trim().isEmpty()) {
                                            	references = TableUtil.getTableName(field.getType());
                                            } else {
                                            	references = TableUtil.getColumnName(references);
                                            }
                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {
                                                sqlForeignKey += String.format(
                                                    "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n",
                                                    tableName,
                                                    constraintName,
                                                    columnName,
                                                    references,
                                                    referencedColumn,
                                                    onDeleteString,
                                                    onUpdateString
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                                sqlForeignKey += String.format(
                                                    "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n",
                                                    tableName,
                                                    constraintName,
                                                    columnName,
                                                    references,
                                                    referencedColumn,
                                                    onDeleteString,
                                                    onUpdateString
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                                sqlForeignKey += String.format(
                                                    "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n",
                                                    tableName,
                                                    constraintName,
                                                    columnName,
                                                    references,
                                                    referencedColumn,
                                                    onDeleteString,
                                                    onUpdateString
                                                );
                                            }
                                            sqlIndex += String.format(
                                                "CREATE INDEX idx_%s_%s ON %s (%s);\n",
                                                tableName,
                                                columnName,
                                                tableName,
                                                columnName
                                            );
                                            java.io.RandomAccessFile out = null;
                                            try {
                                                boolean generateCode = true;
                                                String classPath = appModelFile.getAbsolutePath();
                                                classPath = classPath.replace(appModelFile.getName(), String.format("%s.java", model));
                                                out = new java.io.RandomAccessFile(classPath, "rw");
                                                out.seek(0);
                                                String currentLine = null;
                                                while ((currentLine = out.readLine()) != null) {
                                                    if (currentLine.contains(String.format("get%s", modelClass.getSimpleName()))) {
                                                        generateCode = false;
                                                    }
                                                }
                                                if (out.length() > 0) {
                                                    out.seek(out.length() - 1);
                                                } else {
                                                    out.seek(out.length());
                                                }
                                                StringBuilder methodStr = new StringBuilder();
                                                methodStr.append("\n");
                                                methodStr.append(
                                                    String.format(
                                                        "    public %s get%s() {\n",
                                                        modelClass.getSimpleName(),
                                                        modelClass.getSimpleName()
                                                    )
                                                );
                                                methodStr.append(
                                                    String.format(
                                                        "        return %s.objects.get(\"%s_id\", this.id);\n",
                                                        modelClass.getSimpleName(),
                                                        tableName
                                                    )
                                                );
                                                methodStr.append("    }\n");
                                                methodStr.append("}");
                                                if (generateCode) {
                                                    out.writeBytes(methodStr.toString() );
                                                }
                                            } catch (java.io.IOException e) {
                                                System.err.println(e);
                                            } finally {
                                                if (out != null) {
                                                    out.close();
                                                }
                                            }
                                        }
                                        if (fieldAnnotation instanceof ForeignKeyField) {
                                            ForeignKeyField foreignKeyFieldAnnotation = (ForeignKeyField) fieldAnnotation;
                                            String columnName = "";
                                            String referencedColumn = "";
                                            if (foreignKeyFieldAnnotation.column_name().equals("")) {
                                                columnName = TableUtil.getColumnName(field);
                                                columnName = String.format("%s_id", columnName);
                                            } else {
                                                columnName = TableUtil.getColumnName(foreignKeyFieldAnnotation.column_name());
                                            }
                                            if (foreignKeyFieldAnnotation.referenced_column().equals("")) {
                                                referencedColumn = "id";
                                            } else {
                                                referencedColumn = TableUtil.getColumnName(foreignKeyFieldAnnotation.referenced_column());
                                            }
                                            sql += String.format(
                                                "    %s INT NOT NULL%s,\n",
                                                columnName,
                                                (
                                                    foreignKeyFieldAnnotation.comment() != null && 
                                                    !foreignKeyFieldAnnotation.comment().equals("") && 
                                                    databaseEngine.trim().equalsIgnoreCase("mysql") 
                                                ) ? String.format(" COMMENT '%s'", foreignKeyFieldAnnotation.comment()) : ""
                                            );
                                            String onDeleteString = "";
                                            if (foreignKeyFieldAnnotation.on_delete().equals(Models.PROTECT)) {
                                                onDeleteString = " ON DELETE RESTRICT";
                                            } else if (foreignKeyFieldAnnotation.on_delete().equals(Models.SET_NULL)) {
                                                onDeleteString = " ON DELETE SET NULL";
                                            } else if (foreignKeyFieldAnnotation.on_delete().equals(Models.CASCADE)) {
                                                onDeleteString = " ON DELETE CASCADE";
                                            } else if (foreignKeyFieldAnnotation.on_delete().equals(Models.SET_DEFAULT)) {
                                                onDeleteString = " ON DELETE SET DEFAULT";
                                            }
                                            String onUpdateString = " ON UPDATE";
                                            if (foreignKeyFieldAnnotation.on_update().equals(Models.PROTECT)) {
                                                onUpdateString = " ON UPDATE RESTRICT";
                                            } else if (foreignKeyFieldAnnotation.on_update().equals(Models.SET_NULL)) {
                                                onUpdateString = " ON UPDATE SET NULL";
                                            } else if (foreignKeyFieldAnnotation.on_update().equals(Models.CASCADE)) {
                                                onUpdateString = " ON UPDATE CASCADE";                                                
                                                if (databaseEngine != null && databaseEngine.equalsIgnoreCase("oracle")) {
                                                    onUpdateString = "";
                                                }
                                            } else if (foreignKeyFieldAnnotation.on_update().equals(Models.SET_DEFAULT) ) {
                                                onUpdateString = " ON UPDATE SET DEFAULT";
                                            }
                                            String model = foreignKeyFieldAnnotation.model();
                                            if (model == null || model.trim().isEmpty()) {
                                            	model = field.getType().getName().replace(field.getType().getPackage().getName() + ".", "");
                                            }
                                            String constraintName = foreignKeyFieldAnnotation.constraint_name();
                                            if (constraintName == null || constraintName.trim().isEmpty()) {
                                            	constraintName = String.format("fk_%s_%s", tableName, TableUtil.getTableName(field.getType()));
                                            }
                                            String references = foreignKeyFieldAnnotation.references();
                                            if (references == null || references.trim().isEmpty()) {
                                            	references = TableUtil.getTableName(field.getType());
                                            } else {
                                            	references = TableUtil.getColumnName(references);
                                            }
                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {
                                                sqlForeignKey += String.format(
                                                	"ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n",
                                                    tableName,
                                                    constraintName,
                                                    columnName,
                                                    references,
                                                    referencedColumn,
                                                    onDeleteString,
                                                    onUpdateString
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                                sqlForeignKey += String.format(
                                                    "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n",
                                                    tableName,
                                                    constraintName,
                                                    columnName,
                                                    references,                                                    	
                                                    referencedColumn,
                                                    onDeleteString,
                                                    onUpdateString
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                                sqlForeignKey += String.format(
                                                    "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n",
                                                    tableName,
                                                    constraintName,
                                                    columnName,
                                                    references,
                                                    referencedColumn,
                                                    onDeleteString,
                                                    onUpdateString
                                                );
                                            }
                                            sqlIndex += String.format(
                                                "CREATE INDEX idx_%s_%s ON %s (%s);\n",
                                                tableName,
                                                columnName,
                                                tableName,
                                                columnName
                                            );
                                            if (intermediateModels.contains(modelClass.getSimpleName())) {
                                            	continue;
                                            }                                            
                                        	java.io.RandomAccessFile out = null;
                                            try {
                                                boolean generateCode = true;
                                                String classPath = appModelFile.getAbsolutePath();
                                                classPath = classPath.replace(appModelFile.getName(), String.format("%s.java", model));
                                                // Creates a random access file.
                                                out = new java.io.RandomAccessFile(classPath, "rw");
                                                // Sets the file's pointer at the first register of the file.
                                                out.seek(0);
                                                String currentLine = null;
                                                while ((currentLine = out.readLine()) != null) {
                                                    if (currentLine.contains(String.format("get%sSet", modelClass.getSimpleName()))) {
                                                        generateCode = false;
                                                    }
                                                }
                                                // Sets the file's pointer at the last register of the file.
                                                if (out.length() > 0) {
                                                    out.seek(out.length() - 1);
                                                } else {
                                                    out.seek(out.length());
                                                }
                                                StringBuilder methodStr = new StringBuilder();
                                                methodStr.append("\n");
                                                methodStr.append(
                                                    String.format(
                                                        "    public jedi.db.models.query.QuerySet<%s> get%sSet() {\n",
                                                        modelClass.getSimpleName(),
                                                        modelClass.getSimpleName()
                                                    )
                                                );
                                                methodStr.append(
                                                    String.format(
                                                        "        return %s.objects.getSet(%s.class, this.id);\n",
                                                        modelClass.getSimpleName(),
                                                        model
                                                    )
                                                );
                                                methodStr.append("    }\n");
                                                methodStr.append("}");
                                                if (generateCode) {
                                                    out.writeBytes(methodStr.toString());
                                                }
                                            } catch (java.io.IOException e) {
                                                System.err.println(e);
                                            } finally {
                                                if (out != null) {
                                                    out.close();
                                                }
                                            }                                            
                                        }
                                        if (fieldAnnotation instanceof ManyToManyField) {
                                            ManyToManyField manyToManyFieldAnnotation = (ManyToManyField) fieldAnnotation;
                                            String fmt = "";
                                            String through = manyToManyFieldAnnotation.through();
                                            through = through.trim();
                                            String model = manyToManyFieldAnnotation.model();                                        
                                            if (model == null || model.trim().isEmpty()) {
                                            	ParameterizedType genericType = null;
                                            	if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
                                                    genericType = (ParameterizedType) field.getGenericType();
                                                    Class superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
                                                    if (superClazz == Model.class) {
                                                        Class clazz = (Class) genericType.getActualTypeArguments()[0];
                                                        model = clazz.getSimpleName();                                                        
                                                    }
                                                }                                            	
                                            }
                                            String references = manyToManyFieldAnnotation.references();
                                            if (references == null || references.trim().isEmpty()) {
                                            	references = TableUtil.getTableName(model);
                                            } else {
                                            	references = TableUtil.getColumnName(references);
                                            }
                                            if (through.isEmpty()) {                                            	                                            	
                                            	if (databaseEngine.equalsIgnoreCase("mysql")) {
	                                                fmt = "CREATE TABLE IF NOT EXISTS %s_%s (\n";
	                                                fmt += "    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
	                                                fmt += ");\n\n";
	                                            } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
	                                                fmt = "CREATE TABLE %s_%s (\n";
	                                                fmt += "    id SERIAL NOT NULL PRIMARY KEY,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
	                                                fmt += ");\n\n";
	                                            } else if (databaseEngine.equalsIgnoreCase("oracle")) {
	                                                fmt = "CREATE TABLE %s_%s (\n";
	                                                fmt += "    id NUMBER(10, 0) NOT NULL PRIMARY KEY,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
	                                                fmt += ");\n\n";
	                                            } else if (databaseEngine.equalsIgnoreCase("h2")) {
	                                                fmt = "CREATE TABLE IF NOT EXISTS %s_%s (\n";
	                                                fmt += "    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    %s_id INT NOT NULL,\n";
	                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
	                                                fmt += ");\n\n";
	                                            } else {
	
	                                            }	
	                                            sqlManyToManyAssociation += String.format(
	                                        		fmt, 
	                                        		tableName, 
	                                        		TableUtil.getColumnName(references),
	                                        		TableUtil.getColumnName(modelClass),
	                                        		TableUtil.getColumnName(model),	
	                                        		tableName,
	                                        		TableUtil.getColumnName(references),
	                                        		TableUtil.getColumnName(modelClass),
	                                        		TableUtil.getColumnName(model)
	                                    		);	                                            
	                                            String tbName = String.format("%s_%s", tableName, TableUtil.getColumnName(references));	                                            
	                                            if (!JediORMEngine.generatedTables.contains(tbName)) {
	                                            	JediORMEngine.generatedTables.add(tbName);
	                                            }	
	                                            if (databaseEngine.equalsIgnoreCase("oracle")) {
	                                            	StringBuilder oracleSequence = new StringBuilder(); 
	                                            	oracleSequence.append("CREATE SEQUENCE seq_%s_%s MINVALUE 1 ");
	                                            	oracleSequence.append("MAXVALUE 999999999999999999999999999 ");
	                                            	oracleSequence.append("INCREMENT BY 1 START WITH 1 CACHE 20 ");
	                                            	oracleSequence.append("NOORDER NOCYCLE;");
	                                            	oracleSequence.append("\n\n");                                            
	                                            	// Sequence.
	                                            	sqlOracleSequences += String.format(
                                            			oracleSequence.toString(), 
                                            			tableName, 
                                            			TableUtil.getColumnName(references)
                                        			);                                            	
	                                            	String manyToManyTableName = String.format(
                                            			"%s_%s", 
                                            			tableName, 
                                            			TableUtil.getColumnName(references)
                                        			);
	                                            	String triggerName = String.format(
                                            			"tgr_autoincr_%s_%s", 
                                            			tableName, 
                                            			TableUtil.getColumnName(references)
                                        			);
	                                            	// Trigger de auto-incremento
	                                            	sqlOracleAutoIncrementTriggers.put(manyToManyTableName, triggerName);
	                                            }	                                            
	                                            String onDeleteString = "";
	                                            if (manyToManyFieldAnnotation.on_delete().equals(Models.PROTECT)) {
	                                                onDeleteString = " ON DELETE RESTRICT";
	                                            } else if (manyToManyFieldAnnotation.on_delete().equals(Models.SET_NULL)) {
	                                                onDeleteString = " ON DELETE SET NULL";
	                                            } else if (manyToManyFieldAnnotation.on_delete().equals(Models.CASCADE)) {
	                                                onDeleteString = " ON DELETE CASCADE";
	                                            } else if (manyToManyFieldAnnotation.on_delete().equals(Models.SET_DEFAULT)) {
	                                                onDeleteString = " ON DELETE SET DEFAULT";
	                                            }	
	                                            String onUpdateString = " ON UPDATE";
	                                            if (manyToManyFieldAnnotation.on_update().equals(Models.PROTECT)) {
	                                                onUpdateString = " ON UPDATE RESTRICT";
	                                            } else if (manyToManyFieldAnnotation.on_update().equals(Models.SET_NULL)) {
	                                                onUpdateString = " ON UPDATE SET NULL";
	                                            } else if (manyToManyFieldAnnotation.on_update().equals(Models.CASCADE)) {
	                                                onUpdateString = " ON UPDATE CASCADE";                                                
	                                                if (databaseEngine != null && databaseEngine.equalsIgnoreCase("oracle")) {
	                                                    onUpdateString = "";
	                                                }
	                                            } else if (manyToManyFieldAnnotation.on_update().equals(Models.SET_DEFAULT)) {
	                                                onUpdateString = " ON UPDATE SET DEFAULT";
	                                            }
	                                            sqlForeignKey += String.format(
	                                                "ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id)%s%s;\n",
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                tableName,
	                                                TableUtil.getColumnName(modelClass),
	                                                tableName,
	                                                onDeleteString,
	                                                onUpdateString
	                                            );	
	                                            sqlIndex += String.format(
	                                                "CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id);\n",
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                TableUtil.getColumnName(modelClass),
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                TableUtil.getColumnName(modelClass)
                                            	);	
	                                            sqlForeignKey += String.format(
	                                                "ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id)%s%s;\n",
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                TableUtil.getColumnName(references),	                                                	
	                                                TableUtil.getColumnName(model),
	                                                TableUtil.getColumnName(references),
	                                                onDeleteString,
	                                                onUpdateString
	                                            );	
	                                            sqlIndex += String.format(
	                                                "CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id);\n",
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                TableUtil.getColumnName(model),
	                                                tableName,
	                                                TableUtil.getColumnName(references),
	                                                TableUtil.getColumnName(model)
	                                            );	                                            
	                                        }
                                            java.io.RandomAccessFile out = null;
                                            boolean generateCode = true;
                                            String classPath = appModelFile.getAbsolutePath();
                                            String currentLine = null;
                                            StringBuilder methodStr = new StringBuilder();
                                            String intermediateTable = null;                                            
                                            String fieldType = field.getGenericType().toString().replace("java.util.List", "List");
                                        	fieldType = fieldType.replace("jedi.db.models.query.QuerySet", "QuerySet");
                                        	fieldType = fieldType.replace(modelClass.getPackage().getName() + ".", "");
                                            try {
                                                if (through.isEmpty()) {
	                                                classPath = classPath.replace(appModelFile.getName(), String.format("%s.java", model));
                                                }
                                                out = new java.io.RandomAccessFile(classPath, "rw");
                                                out.seek(0);
                                                methodStr.append("\n");
                                                if (through.isEmpty()) {
                                                	while ((currentLine = out.readLine()) != null) {                                                	
                                                		if (currentLine.contains(String.format("get%sSet", modelClass.getSimpleName()))) {
                                                			generateCode = false;
                                                			break;
                                                		}
                                                	}	
	                                                methodStr.append(
	                                                    String.format(
	                                                        "    public jedi.db.models.query.QuerySet<%s> get%sSet() {\n",
	                                                        modelClass.getSimpleName(),
	                                                        modelClass.getSimpleName()
	                                                    )
	                                                );
	                                                methodStr.append(
	                                                    String.format(
	                                                        "        return %s.objects.getSet(%s.class, this.id);\n",
	                                                        modelClass.getSimpleName(),
	                                                        model
	                                                    )
	                                                );	                                                
                                                } else {
                                                	methodStr.append(
                                            			String.format(
                                        					"    public %s get%s() {\n",
                                        					fieldType,                                        					
                                        					String.format(
                                    							"%s%s", 
                                    							Character.toUpperCase(field.getName().charAt(0)), 
                                								field.getName().substring(1)
                                							)
                                    					)
                                    				);                                                	
                                                	while ((currentLine = out.readLine()) != null) {                                                	
                                                		if (currentLine.contains(
                                            				String.format(
                                            					"    public %s get%s%s() {",
                                            					fieldType,                                        					
                                    							Character.toUpperCase(field.getName().charAt(0)), 
                                								field.getName().substring(1)
                                        					))) {
                                                			generateCode = false;
                                                			break;
                                                		}
                                                	}
                                                	Class intermediateModelClass = Class.forName(
                                            			String.format(
                                        					"%s.%s", 
                                        					modelClass
                                        						.getPackage()
                                        						.getName(), 
                                    						through
                                    					)
                                					);
                                                	intermediateTable = TableUtil.getTableName(intermediateModelClass);
                                                	methodStr.append(
                                            			String.format(
                                        					"        %s %s = %s.objects.getSet(%s.class, this.id);\n",
                                        					fieldType.replace(model, through),
                                        					intermediateTable,
                                        					through,
                                        					modelClass.getSimpleName()
                                    					)
                                        			);
                                                	methodStr.append(
                                            			String.format(
                                        					"        %s = new %s();\n", 
                                        					field.getName(), 
                                        					fieldType.startsWith("List") ? 
                                							fieldType.replace("List", "java.util.ArrayList") : fieldType
                                    					)
                                					);                                                	
                                                	methodStr.append(
                                            			String.format(
                                        					"        %s %s = null;\n", 
                                        					model, 
                                        					model.toLowerCase()
                                    					)
                                					);
                                                	methodStr.append(
                                            			String.format(
                                        					"        for (%s %s : %s) {\n",
                                        					through,
                                        					through.toLowerCase(),
                                        					intermediateTable
                                    					)
                                        			);
                                                	methodStr.append(
                                            			String.format(
                                        					"            %s = %s.objects.<%s>get(\"id\", %s.get%s().getId());\n",
                                        					model.toLowerCase(),
                                        					model,
                                        					model,
                                        					through.toLowerCase(),
                                        					model
                                            			)
                                        			);
                                                	methodStr.append(
                                            			String.format(
                                        					"            %s.add(%s);\n", 
                                        					field.getName(), 
                                        					model.toLowerCase()
                                    					)
                                					);
                                                	methodStr.append("        }\n");
                                                	methodStr.append(String.format("        return %s;\n", field.getName()));
                                                }
                                            	methodStr.append("    }\n");
                                            	if (!through.isEmpty()) {                                            		
 	                                            	methodStr.append("\n");	                                            	
	                                            	methodStr.append(
	                                        			String.format(
	                                    					"    public void set%s%s(%s %s) {\n",
	                                    					Character.toUpperCase(field.getName().charAt(0)),
	                                    					field.getName().substring(1),
	                                    					fieldType,
	                                    					field.getName()
	                                					)
	                                    			);
	                                            	methodStr.append(String.format("        this.%s = %s;\n", field.getName(), field.getName()));
	                                            	methodStr.append("    }\n");
 	                                            	
                                            	}
                                            	methodStr.append("}");
                                            	if (out.length() > 0) {
                                                    out.seek(out.length() - 1);
                                                } else {
                                                    out.seek(out.length());
                                                }                                            	
                                                if (generateCode) {
                                                    out.writeBytes(methodStr.toString());
                                                }
                                            } catch (IOException e) {
                                                System.err.println(e);
                                            } finally {
                                                if (out != null) {
                                                    out.close();
                                                }
                                            }
                                            if (!through.isEmpty()) {
	                                            try {
	                                                generateCode = true;
	                                                classPath = appModelFile.getAbsolutePath();
	                                                model = manyToManyFieldAnnotation.model();
	                                                if (model == null || model.trim().isEmpty()) {
	                                                	ParameterizedType genericType = null;
	                                                	if (ParameterizedType.class.isAssignableFrom(field.getGenericType().getClass())) {
	                                                        genericType = (ParameterizedType) field.getGenericType();
	                                                        Class superClazz = ((Class) (genericType.getActualTypeArguments()[0])).getSuperclass();
	                                                        if (superClazz == Model.class) {
	                                                            Class clazz = (Class) genericType.getActualTypeArguments()[0];
	                                                            model = clazz.getSimpleName();                                                        
	                                                        }
	                                                    }                                            	
	                                                }
	                                                classPath = classPath.replace(appModelFile.getName(), String.format("%s.java", model));
	                                                out = new java.io.RandomAccessFile(classPath, "rw");
	                                                out.seek(0);
	                                                currentLine = null;
	                                                methodStr = methodStr.delete(0, methodStr.length());
	                                                methodStr.append("\n");
	                                                while ((currentLine = out.readLine()) != null) {                                                	
	                                            		if (currentLine.contains(
                                            				String.format(
                                        						"QuerySet<%s> get%sSet", 
                                        						modelClass.getSimpleName(),
                                        						modelClass.getSimpleName()
                                    						)
                                						)) {
	                                            			generateCode = false;
	                                            			break;
	                                            		}
	                                            	}
	                                                methodStr.append(
	                                                    String.format(
	                                                        "    public jedi.db.models.query.QuerySet<%s> get%sSet() {\n",
	                                                        modelClass.getSimpleName(),
	                                                        modelClass.getSimpleName()
	                                                    )
	                                                );
	                                                methodStr.append(
	                                                    String.format(
	                                                        "        jedi.db.models.query.QuerySet<%s> %s = %s.objects.getSet(%s.class, this.id);\n",
	                                                        through,
	                                                        intermediateTable,
	                                                        through,
	                                                        model
	                                                    )
	                                                );
	                                                methodStr.append(
                                                		String.format(
                                            				"        jedi.db.models.query.QuerySet<%s> %s = new jedi.db.models.query.QuerySet<%s>();\n",
                                            				modelClass.getSimpleName(),
                                            				TableUtil.getTableName(modelClass),
                                            				modelClass.getSimpleName()
                                        				)
                                    				);
	                                                methodStr.append(
                                                		String.format(
                                            				"        %s %s = null;\n",
                                            				modelClass.getSimpleName(),
                                            				modelClass.getSimpleName().toLowerCase()
                                            				
                                                		)
                                            		);
	                                                methodStr.append(
                                                		String.format(
                                            				"        for (%s %s : %s) {\n",
                                            				through,
                                            				through.toLowerCase(),
                                            				intermediateTable
                                        				)
                                    				);
	                                                methodStr.append(
                                                		String.format(
                                            				"            %s = %s.objects.<%s>get(\"id\", %s.get%s().getId());\n", 
                                            				modelClass.getSimpleName().toLowerCase(),
                                            				modelClass.getSimpleName(),
                                            				modelClass.getSimpleName(),
                                            				through.toLowerCase(),
                                            				modelClass.getSimpleName()
                                        				)
                                    				);
	                                                methodStr.append(
                                                		String.format(
                                            				"            %s.get%s%s();\n", 
                                            				modelClass.getSimpleName().toLowerCase(),
                                            				Character.toUpperCase(field.getName().charAt(0)),
                                            				field.getName().substring(1)
                                        				)
                                    				);
	                                                methodStr.append(
                                                		String.format(
                                            				"            %s.add(%s);\n", 
                                            				TableUtil.getTableName(modelClass),
                                            				modelClass.getSimpleName().toLowerCase()
                                        				)
                                    				);
	                                                methodStr.append("        }\n");
	                                                methodStr.append(String.format("        return %s;\n", TableUtil.getTableName(modelClass)));
	                                                methodStr.append("    }\n");	                                                
	                                            	methodStr.append("}");
	                                            	if (out.length() > 0) {
	                                                    out.seek(out.length() - 1);
	                                                } else {
	                                                    out.seek(out.length());
	                                                }                                            	
	                                                if (generateCode) {
	                                                    out.writeBytes(methodStr.toString());
	                                                }
	                                            } catch (java.io.IOException e) {
	                                            	System.err.println(e);
	                                            } finally {
	                                            	if (out != null) {
	                                            		out.close();
	                                            	}
	                                            }
                                            }
                                        }
                                    }
                                }
                                sql = sql.substring(0, sql.lastIndexOf(",") ) + "\n";
                                if (tableAnnotation != null) {
                                    if (databaseEngine.trim().equalsIgnoreCase("mysql")) {
                                        sql += String.format(
                                            ")%s%s%s;\n\n",
                                            tableAnnotation.engine().trim().equals("") ? "" : String.format(
                                                " ENGINE=%s", tableAnnotation.engine() 
                                            ),
                                            tableAnnotation.charset().trim().equals("") ? "" : String.format(
                                                " DEFAULT CHARSET=%s", tableAnnotation.charset()
                                            ),
                                            tableAnnotation.comment().trim().equals("") ? "" : String.format(
                                                " COMMENT '%s'", tableAnnotation.comment() 
                                            )
                                        );
                                    } else if (databaseEngine.trim().equalsIgnoreCase("postgresql") || 
                                    		   databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                        sql += String.format(
                                            ");\n\n%s",
                                            tableAnnotation.comment().trim().equals("") ? "" : String.format(
                                                "COMMENT ON TABLE %s IS '%s';\n\n",
                                                tableName,
                                                tableAnnotation.comment()
                                            )
                                        );
                                        sql += postgresqlOrOracleColumnsComments;
                                    } else {
                                        sql += ");\n\n";
                                    }
                                } else {
                                    sql += ");\n\n";
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            String sqlTransaction = "";
                            if (databaseEngine.trim().equalsIgnoreCase("mysql") || 
                        		databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                sqlTransaction = "BEGIN;\n\n";
                            }
                            sqlTransaction += sql;
                            sqlTransaction += sqlManyToManyAssociation;
                            sqlTransaction += sqlForeignKey + "\n";
                            sqlTransaction += sqlIndex;
                            if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                sqlTransaction += sqlOracleSequences;
                            }
                            sqlTransaction += "\nCOMMIT;";
                            if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                sqlTransaction = sqlTransaction.toUpperCase();
                            }                                                        
                            /* Shows the complete SQL that generates the application's 
                             * database structure.
                             */
                            // System.out.println(sql_transaction);
                            Scanner scanner = new Scanner(sqlTransaction);
                            scanner.useDelimiter(";\n");
                            String currentStatement = "";                            
                            if (JediORMEngine.DEBUG) {
                            	System.out.println("");
                            }
                            while (scanner.hasNext()) {
                                currentStatement = scanner.next();
                                // Shows each SQL statement that will be executed.
                                if (JediORMEngine.DEBUG) {
                                	if (currentStatement.endsWith(";")) {
                                		System.out.println(currentStatement);
                                	} else {
                                		System.out.println(currentStatement + ";");
                                	}
                                }
                                statement.execute(currentStatement);
                            }
                            if (JediORMEngine.DEBUG) {
                            	System.out.println("");
                            }
                            scanner.close();
                            if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                                String oracleTriggers = "";
                                for (Map.Entry<String, String> sqlOracleAutoIncrementTrigger : sqlOracleAutoIncrementTriggers.entrySet()) {
                                    String table = sqlOracleAutoIncrementTrigger.getKey();
                                    String trigger = sqlOracleAutoIncrementTrigger.getValue();
                                    String oracleTrigger = "" +
                                    "CREATE OR REPLACE TRIGGER "
                                        + trigger
                                        + "\n"
                                        + "BEFORE INSERT ON "
                                        + table
                                        + " FOR EACH ROW\n"
                                        + "DECLARE\n"
                                        + "    MAX_ID NUMBER;\n"
                                        + "    CUR_SEQ NUMBER;\n"
                                        + "BEGIN\n"
                                        + "    IF :NEW.ID IS NULL THEN\n"
                                        + "        -- No ID passed, get one from the sequence\n"
                                        + "        SELECT seq_"
                                        + table
                                        + ".NEXTVAL INTO :NEW.ID FROM DUAL;\n"
                                        + "    ELSE\n"
                                        + "        -- ID was set via insert, so update the sequence\n"
                                        + "        SELECT GREATEST(NVL(MAX(ID), 0), :NEW.ID) INTO MAX_ID FROM "
                                        + table + ";\n" + "        SELECT seq_" + table
                                        + ".NEXTVAL INTO CUR_SEQ FROM DUAL;\n"
                                        + "        WHILE CUR_SEQ < MAX_ID\n" + "        LOOP\n"
                                        + "            SELECT seq_" + table
                                        + ".NEXTVAL INTO CUR_SEQ FROM DUAL;\n"
                                        + "        END LOOP;\n" + "    END IF;\n" + "END;\n";
                                    // + "/";

                                    oracleTriggers += oracleTrigger.toUpperCase() + "\n\n";
                                    oracleTriggers += String.format("ALTER TRIGGER %s ENABLE\n\n", trigger.toUpperCase());
                                    statement.executeUpdate(oracleTrigger);
                                }                                
                                if (JediORMEngine.DEBUG) {
                                	System.out.println(oracleTriggers);
                                }                                
                                if (!connection.getAutoCommit()) {
                                    connection.commit();
                                }
                            } else if (databaseEngine.trim().equalsIgnoreCase("mysql") && mysqlVersionNumber < 56) {
                            	StringBuilder sb = new StringBuilder();
                                for (String tb : mySQLAutoNow.keySet()) {
                                	if (mySQLAutoNow.get(tb).size() > 1) {
                                		sb.append(String.format("CREATE TRIGGER tgr_%s_auto_now BEFORE UPDATE ON %s FOR EACH ROW\n", tb, tb));
                                    	sb.append("BEGIN\n");
                                    } else if (mySQLAutoNow.get(tb).size() == 1) {
                                    	sb.append(String.format("CREATE TRIGGER tgr_%s_auto_now BEFORE UPDATE ON %s FOR EACH ROW", tb, tb));
                                    } else {
                                    	
                                    }
                                    for (String fieldName : mySQLAutoNow.get(tb)) {
                                    	if (mySQLAutoNow.get(tb).size() > 1) {
                                    		sb.append(String.format("    SET NEW.%s = NOW();\n", fieldName));
                                    	} else if (mySQLAutoNow.get(tb).size() == 1) {
                                    		sb.append(String.format(" SET NEW.%s = NOW();\n", fieldName));
                                    	} else {
                                    	}
                                    }
                                    if (mySQLAutoNow.get(tb).size() > 1) {
                                    	sb.append("END;\n");
                                    }
                                    if (sb.length() > 0) {
                                        mySQLDateTimeTriggers.add(sb.toString());
                                        sb.delete(0, sb.length());
                                    }
                                }
                                for (String tb : mySQLAutoNowAdd.keySet()) {
                                	if (mySQLAutoNowAdd.get(tb).size() > 1) {
                                		sb.append(String.format("CREATE TRIGGER tgr_%s_auto_now_add BEFORE INSERT ON %s FOR EACH ROW\n", tb, tb));
                                    	sb.append("BEGIN\n");
                                    } else if (mySQLAutoNowAdd.get(tb).size() == 1) {
                                    	sb.append(String.format("CREATE TRIGGER tgr_%s_auto_now_add BEFORE INSERT ON %s FOR EACH ROW", tb, tb));
                                    } else {
                                    }
                                    for (String fieldName : mySQLAutoNowAdd.get(tb)) {
                                    	if (mySQLAutoNowAdd.get(tb).size() > 1) {
                                    		sb.append(String.format("    SET NEW.%s = NOW();\n", fieldName));
                                    	} else if (mySQLAutoNowAdd.get(tb).size() == 1) {
                                    		sb.append(String.format(" SET NEW.%s = NOW();\n", fieldName));
                                    	} else {
                                    	}
                                    }
                                    if (mySQLAutoNowAdd.get(tb).size() > 1) {
                                    	sb.append("END;\n");
                                    }
                                    if (sb.length() > 0) {
                                        mySQLDateTimeTriggers.add(sb.toString());
                                        sb.delete(0, sb.length());
                                    }
                                }
                                sb = null;
                                if (mySQLDateTimeTriggers.size() > 0) {
                                    for (String trigger : mySQLDateTimeTriggers) {
                                    	if (JediORMEngine.DEBUG) {
                                    		if (trigger.endsWith("END;\n")) {
                                    			String triggerAux = trigger.replace("END;\n", "END $$\nDELIMITER ;\n");
                                    			System.out.println("DELIMITER $$");
                                    			System.out.println(triggerAux);
                                    		} else {
                                    			System.out.println(trigger);
                                    		}
                                    	}
                                    	statement.execute(trigger);
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                statement.close();
                                connection.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        syncdb(appDirContent.getAbsolutePath());
                    }
                }
            }
        }
    }
    
    public static void syncdb() {
    	syncdb(JediORMEngine.APP_SRC_DIR);
    }
    
    public static void droptables(String...tables) {    	
    	setForeignKeyChecks();    	
    	if (tables != null && tables.length > 0) {				
			if (JediORMEngine.DEBUG) {
				System.out.println("");
			}
    		for (String table : tables) {
    			if (JediORMEngine.DEBUG) {
    				System.out.println(String.format("DROP TABLE %s;", table));
    			}    			
    			SQLManager.raw(String.format("DROP TABLE %s", table));    			
    		}    		
    	} 	
    }
    
    public static void droptables(List<String> tables) {
    	setForeignKeyChecks();
    	if (tables != null && tables.size() > 0) {
			if (JediORMEngine.DEBUG) {
				System.out.println("");
			}			
    		for (String table : tables) {
    			if (JediORMEngine.DEBUG) {
    				System.out.println(String.format("DROP TABLE %s;", table));
    			}    			
    			SQLManager.raw(String.format("DROP TABLE %s", table));
    		}    		
    	}
    }   
    
    public static void droptables() {
    	droptables(JediORMEngine.generatedTables);
    } 
    
    public static void flush() {
    	droptables();
    	syncdb(JediORMEngine.APP_SRC_DIR);
    }
    
    public static void sqlclear() {
    	List<String> tables = JediORMEngine.generatedTables;
    	if (tables != null && tables.size() > 0) {  
    		System.out.println();
    		for (String table : tables) {
    			String statement = String.format("DROP TABLE %s;", table);
    			System.out.println(statement);
    		}
    	}
    }

    public static void raw(String sql) {
    	if (sql != null && !sql.trim().isEmpty()) {    		
    		SQLManager.raw(sql);
    	}
    }
    
    private static Connection getConnection() {
    	try {
			if (connection == null || connection.isClosed()) {
				connection = ConnectionFactory.getConnection();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return connection;
	}
    
    private static void setForeignKeyChecks() {
    	if (JediORMEngine.DATABASE_ENGINE == null) {
    		Properties databaseSettings = new Properties();
            FileInputStream fileInputStream;
			try {
				fileInputStream = new FileInputStream(APP_DB_CONFIG_FILE);
	            databaseSettings.load(fileInputStream);
	            JediORMEngine.DATABASE_ENGINE = databaseSettings.getProperty("database.engine");
	            JediORMEngine.DATABASE_ENGINE = JediORMEngine.DATABASE_ENGINE.trim();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	
    	if (JediORMEngine.DATABASE_ENGINE.equalsIgnoreCase("mysql")) {
    		if (JediORMEngine.FOREIGN_KEY_CHECKS) {
        		// Enable    		
    			JediORMEngine.raw("SET FOREIGN_KEY_CHECKS = 1");        		
        	} else {
        		// Disable
        		JediORMEngine.raw("SET FOREIGN_KEY_CHECKS = 0");
        	}
        }
    }
}