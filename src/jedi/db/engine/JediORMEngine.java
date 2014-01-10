/***********************************************************************************************
 * @(#)JediORMEngine.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/08
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

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
import jedi.db.Models;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.DecimalField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.IntegerField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Manager;

/**
 * Jedi's Object-Relational Mapping Engine.
 * 
 * @version 1.0 08 Jan 2014
 * @author Thiago Alexandre Martins Monteiro
 * 
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JediORMEngine {

    // Application's root directory.
    public static final String APP_ROOT_DIR = System.getProperty("user.dir");

    // Application's source code directory.
    public static final String APP_SRC_DIR = String.format(
        "%s/web/WEB-INF/src",
        APP_ROOT_DIR
    );

    public static final String APP_DB_CONFIG = String.format(
        "%s/web/WEB-INF/config/database.properties",
        JediORMEngine.APP_ROOT_DIR
    );

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
                    if (!appDirContent.getAbsolutePath().contains(JediORMEngine.JEDI_DB_MODELS)
                        && appDirContent.getAbsolutePath().endsWith("models") ) {

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
                        List<String> mysqlDatetimeTriggers = new ArrayList<String>();
                        
                        int mysqlVersionNumber = 0;
                        Connection conn = ConnectionFactory.getConnection();
                        Statement stmt = null;
                        String databaseEngine = "";

                        try {
                            // Disable the auto-commit.
                            conn.setAutoCommit(false);
                            stmt = conn.createStatement();
                            Properties databaseSettings = new Properties();
                            FileInputStream fileInputStream = new FileInputStream(APP_DB_CONFIG);
                            databaseSettings.load(fileInputStream);
                            databaseEngine = databaseSettings.getProperty("database.engine");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // ORM Mapping - generates the database structure 
                        // for each model class of the application.
                        for (File appModelFile : appModelsFiles) {
                            try {
                                String modelClassName = appModelFile.getAbsolutePath();

                                // Ignore files that doesn't end with the .java extension.
                                if (!modelClassName.endsWith("java") ) {
                                    continue;
                                }

                                modelClassName = modelClassName.replace(
                                    String.format(
                                        "%s%ssrc%s", 
                                        JediORMEngine.APP_ROOT_DIR, 
                                        File.separator,
                                        File.separator
                                    ), ""
                                );

                                modelClassName = modelClassName.replace(File.separator, ".").replace(".java", "");

                                JediORMEngine.readedAppModels.add(modelClassName);

                                // A model class reference.
                                Class modelClass = Class.forName(modelClassName);

                                // Ignores any object that doesn't extends Model.
                                if (!modelClass.getSuperclass().getName().equals("jedi.db.models.Model") ) {
                                    continue;
                                }

                                String tableName = "";

                                // Obtendo a anotação da classe de modelo.
                                Table tableAnnotation = (Table) modelClass.getAnnotation(Table.class);

                                // Verificando se a anotação existe e se um nome foi informado 
                                // para a tabela do modelo.
                                if (tableAnnotation != null && !( (Table) tableAnnotation).name().equals("") ) {
                                    tableName = ( (Table) tableAnnotation).name();
                                } else {
                                    tableName = String.format("%ss", modelClass.getSimpleName() );
                                }
                                
                                tableName = tableName.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2");
                                tableName = tableName.toLowerCase();
                                generatedTables.add(tableName);

                                if (databaseEngine.trim().equalsIgnoreCase("mysql") || 
                                    databaseEngine.trim().equalsIgnoreCase("h2") ) {

                                    sql += String.format(
                                        "CREATE TABLE IF NOT EXISTS %s (\n", 
                                        tableName
                                    );

                                    sql += String.format(
                                        "    %s INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n",
                                        modelClass.getSuperclass().getDeclaredField("id").getName() 
                                    );
                                } else {
                                    sql += String.format("CREATE TABLE %s (\n", tableName);
                                }

                                if (databaseEngine.trim().equalsIgnoreCase("postgresql") ) {
                                    sql += String.format(
                                        "    %s SERIAL NOT NULL PRIMARY KEY,\n",
                                        modelClass.getSuperclass().getDeclaredField("id").getName()
                                    );
                                } else if (databaseEngine.trim().equalsIgnoreCase("oracle") ) {
                                    sql += String.format(
                                        "    %s NUMBER(10,0) NOT NULL PRIMARY KEY,\n",
                                        modelClass.getSuperclass().getDeclaredField("id").getName()
                                    );

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

                                for (Field field : modelClass.getDeclaredFields() ) {
                                    if (field.getName().equals("serialVersionUID") ) {
                                        continue;
                                    }

                                    for (Annotation fieldAnnotation : field.getAnnotations() ) {
                                        if (fieldAnnotation instanceof CharField) {
                                            CharField charFieldAnnotation = (CharField) fieldAnnotation;
                                            
                                            if (databaseEngine.equalsIgnoreCase("mysql") ) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    charFieldAnnotation.max_length(),
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        charFieldAnnotation.default_value() != null 
                                                        && !charFieldAnnotation.default_value().equals("\\0") 
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        charFieldAnnotation.comment() != null 
                                                        && !charFieldAnnotation.comment().equals("")
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        charFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.equalsIgnoreCase("postgresql") ) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    charFieldAnnotation.max_length(),
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        charFieldAnnotation.default_value() != null 
                                                        && !charFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );

                                                if (charFieldAnnotation != null 
                                                    && charFieldAnnotation.comment() != null
                                                    && !charFieldAnnotation.comment().trim().isEmpty() ) {
                                                    
                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        field.getName().replaceAll(
                                                            "([a-z0-9]+)([A-Z])", "$1_$2"
                                                        ).toLowerCase(),
                                                        charFieldAnnotation.comment()
                                                    );
                                                }

                                            } else if (databaseEngine.equalsIgnoreCase("oracle") ) {
                                                sql += String.format(
                                                    "    %s VARCHAR2(%d)%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    charFieldAnnotation.max_length(),
                                                    (
                                                        charFieldAnnotation.default_value() != null 
                                                        && !charFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );

                                                if (charFieldAnnotation != null
                                                    && charFieldAnnotation.comment() != null
                                                    && !charFieldAnnotation.comment().trim().isEmpty() ) {

                                                    postgresqlOrOracleColumnsComments += String.format(
                                                        "COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                        tableName,
                                                        field.getName().replaceAll(
                                                            "([a-z0-9]+)([A-Z])", "$1_$2"
                                                        ).toLowerCase(),
                                                        charFieldAnnotation.comment()
                                                    );
                                                }

                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2") ) {
                                                sql += String.format(
                                                    "    %s VARCHAR(%d)%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    charFieldAnnotation.max_length(),
                                                    charFieldAnnotation.required() ? " NOT NULL" : "",
                                                    (
                                                        charFieldAnnotation.default_value() != null 
                                                        && !charFieldAnnotation.default_value().equals("\\0")
                                                    ) ? String.format(
                                                        " DEFAULT '%s'", 
                                                        charFieldAnnotation.default_value() 
                                                    ) : "",
                                                    charFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }

                                        if (fieldAnnotation instanceof IntegerField) {
                                            IntegerField integerFieldAnnotation = (IntegerField) fieldAnnotation;
                                            
                                            if (databaseEngine.trim().equalsIgnoreCase("mysql") ) {
                                                sql += String.format(
                                                    "    %s INT(%d)%s%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    integerFieldAnnotation.size(),
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !integerFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        integerFieldAnnotation
                                                        .default_value()
                                                    ) : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : "",
                                                    (
                                                        integerFieldAnnotation.comment() != null
                                                        && !integerFieldAnnotation.comment().equals("") 
                                                        && databaseEngine.trim()
                                                        .equalsIgnoreCase("mysql") 
                                                    ) ? String.format(
                                                        " COMMENT '%s'", 
                                                        integerFieldAnnotation.comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql") ) {
                                                sql += String.format(
                                                    "    %s INT%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !integerFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        integerFieldAnnotation.default_value()
                                                    ) : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle") ) {
                                                sql += String.format(
                                                    "    %s NUMBER(%d,0) %s%s%s,\n", 
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    integerFieldAnnotation.size(),
                                                    !integerFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        integerFieldAnnotation.default_value()
                                                    ) : "",
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2") ) {
                                                sql += String.format(
                                                    "    %s INT(%d)%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    integerFieldAnnotation.size(),
                                                    integerFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !integerFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        integerFieldAnnotation
                                                        .default_value()
                                                    ) : "",
                                                    integerFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }

                                        if (fieldAnnotation instanceof DecimalField) {
                                            DecimalField decimalFieldAnnotation = (DecimalField) fieldAnnotation;

                                            if (databaseEngine.trim().equalsIgnoreCase("mysql") ) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !decimalFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
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
                                                        decimalFieldAnnotation .comment() 
                                                    ) : ""
                                                );
                                            } else if (databaseEngine.trim().equalsIgnoreCase("postgresql") ) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !decimalFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        decimalFieldAnnotation
                                                        .default_value()
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
                                            } else if (databaseEngine.trim().equalsIgnoreCase("oracle") ) {
                                                sql += String.format(
                                                    "    %s NUMERIC(%d,%d)%s%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])", "$1_$2"
                                                    ).toLowerCase(),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    !decimalFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        decimalFieldAnnotation.default_value()
                                                    ) : "",
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
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
                                            } else if (databaseEngine.trim().equalsIgnoreCase("h2") ) {
                                                sql += String.format(
                                                    "    %s DECIMAL(%d,%d)%s%s%s,\n",
                                                    field.getName().replaceAll(
                                                        "([a-z0-9]+)([A-Z])",
                                                        "$1_$2"
                                                    ).toLowerCase(),
                                                    decimalFieldAnnotation.precision(),
                                                    decimalFieldAnnotation.scale(),
                                                    decimalFieldAnnotation.required() ? " NOT NULL" : "",
                                                    !decimalFieldAnnotation.default_value()
                                                    .trim()
                                                    .isEmpty() ? String.format(
                                                        " DEFAULT %s",
                                                        decimalFieldAnnotation.default_value()
                                                    ) : "",
                                                    decimalFieldAnnotation.unique() ? " UNIQUE" : ""
                                                );
                                            } else {

                                            }
                                        }

                                        if (fieldAnnotation instanceof DateField) {
                                            DateField dateFieldAnnotation = (DateField) fieldAnnotation;

                                            String format = "    %s DATE%s%s%s%s%s,\n";
                                            String defaultDate = dateFieldAnnotation.default_value().isEmpty() ? "" : String.format(
                                                " DEFAULT '%s'", 
                                                dateFieldAnnotation.default_value()
                                                .trim()
                                                .toUpperCase()
                                            );

                                            defaultDate = defaultDate.replace("'NULL'", "NULL");

                                            if (defaultDate.contains("NULL")) {
                                                defaultDate = defaultDate.replace("DEFAULT", "");
                                            }

                                            if (dateFieldAnnotation.auto_now_add()
                                                    || dateFieldAnnotation.auto_now()) {

                                                if (databaseEngine.equals("mysql")) {

                                                    format = "    %s DATETIME%s%s%s%s%s,\n";

                                                    Manager manager = new Manager(modelClass);

                                                    String[] mysql_version = manager
                                                            .raw("SELECT VERSION()").get(0).get(0)
                                                            .get("VERSION()").toString()
                                                            .split("\\.");

                                                    mysqlVersionNumber = Integer.parseInt(String
                                                            .format("%s%s", mysql_version[0],
                                                                    mysql_version[1]));

                                                    if (mysqlVersionNumber >= 56
                                                            && dateFieldAnnotation.auto_now_add()) {
                                                        defaultDate = String.format(
                                                                " DEFAULT CURRENT_TIMESTAMP",
                                                                dateFieldAnnotation
                                                                        .default_value());
                                                    }

                                                } else {
                                                    format = "    %s TIMESTAMP%s%s%s%s%s,\n";
                                                }

                                                // sb.append("DELIMITER $\n\n");

                                                if (mysqlVersionNumber < 56) {

                                                    StringBuilder sb = null;

                                                    if (dateFieldAnnotation.auto_now_add()) {

                                                        sb = new StringBuilder();

                                                        sb.append(String
                                                                .format("\nCREATE TRIGGER tgr_%s_%s_insert BEFORE INSERT ON %s\n",
                                                                        tableName,
                                                                        field.getName()
                                                                                .replaceAll(
                                                                                        "([a-z0-9]+)([A-Z])",
                                                                                        "$1_$2")
                                                                                .toLowerCase(),
                                                                        tableName));

                                                        sb.append("FOR EACH ROW\n");
                                                        sb.append("BEGIN\n");
                                                        sb.append("    SET NEW.data_nascimento = NOW();\n");
                                                        // sb.append("END;$\n\n");
                                                        sb.append("END;\n\n");

                                                        mysqlDatetimeTriggers.add(sb.toString());
                                                    }

                                                    if (dateFieldAnnotation.auto_now()) {

                                                        sb = new StringBuilder();

                                                        sb.append(String
                                                                .format("CREATE TRIGGER tgr_%s_%s_update BEFORE UPDATE ON %s\n",
                                                                        tableName,
                                                                        field.getName()
                                                                                .replaceAll(
                                                                                        "([a-z0-9]+)([A-Z])",
                                                                                        "$1_$2")
                                                                                .toLowerCase(),
                                                                        tableName));

                                                        sb.append("FOR EACH ROW\n");
                                                        sb.append("BEGIN\n");
                                                        sb.append("    SET NEW.data_nascimento = NOW();\n");
                                                        // sb.append("END;$\n\n");
                                                        sb.append("END;\n\n");

                                                        mysqlDatetimeTriggers.add(sb.toString());
                                                    }

                                                    sb = null;

                                                    // sb.append("DELIMITER ;\n");
                                                }
                                            }

                                            if (databaseEngine.equalsIgnoreCase("mysql")) {

                                                sql += String
                                                        .format(

                                                        format,

                                                                field.getName()
                                                                        .replaceAll(
                                                                                "([a-z0-9]+)([A-Z])",
                                                                                "$1_$2")
                                                                        .toLowerCase(),

                                                                dateFieldAnnotation.required()
                                                                        ? " NOT NULL"
                                                                        : "",

                                                                defaultDate,

                                                                dateFieldAnnotation.auto_now()
                                                                        && (mysqlVersionNumber >= 56 || !databaseEngine
                                                                                .equals("mysql"))
                                                                        ? String.format(" ON UPDATE CURRENT_TIMESTAMP")
                                                                        : "",

                                                                dateFieldAnnotation.unique()
                                                                        ? " UNIQUE"
                                                                        : "",

                                                                (dateFieldAnnotation.comment() != null && !dateFieldAnnotation
                                                                        .comment().equals(""))
                                                                        ? " COMMENT '"
                                                                                + dateFieldAnnotation
                                                                                        .comment()
                                                                                + "'" : "");

                                            } else if (databaseEngine
                                                    .equalsIgnoreCase("postgresql")) {

                                                sql += String
                                                        .format(

                                                        "    %s DATE%s%s%s,\n",

                                                                field.getName()
                                                                        .replaceAll(
                                                                                "([a-z0-9]+)([A-Z])",
                                                                                "$1_$2")
                                                                        .toLowerCase(),

                                                                dateFieldAnnotation.required()
                                                                        ? " NOT NULL"
                                                                        : "",

                                                                dateFieldAnnotation.unique()
                                                                        ? " UNIQUE"
                                                                        : "",

                                                                (dateFieldAnnotation
                                                                        .default_value() != null && !dateFieldAnnotation
                                                                        .default_value().equals(
                                                                                "\0"))
                                                                        ? " DEFAULT '"
                                                                                + dateFieldAnnotation
                                                                                        .default_value()
                                                                                + "'"
                                                                        : ""

                                                        );

                                                if (dateFieldAnnotation != null
                                                        && dateFieldAnnotation.comment() != null
                                                        && !dateFieldAnnotation.comment().trim()
                                                                .isEmpty()) {

                                                    postgresqlOrOracleColumnsComments += String
                                                            .format("COMMENT ON COLUMN %s.%s IS '%s';\n\n",
                                                                    tableName,
                                                                    field.getName()
                                                                            .replaceAll(
                                                                                    "([a-z0-9]+)([A-Z])",
                                                                                    "$1_$2")
                                                                            .toLowerCase(),
                                                                    dateFieldAnnotation.comment());
                                                }

                                            } else if (databaseEngine.equalsIgnoreCase("oracle")) {

                                                sql += String
                                                        .format(

                                                        "    %s DATE%s%s%s,\n",

                                                                field.getName()
                                                                        .replaceAll(
                                                                                "([a-z0-9]+)([A-Z])",
                                                                                "$1_$2")
                                                                        .toLowerCase(),

                                                                (dateFieldAnnotation
                                                                        .default_value() != null && !dateFieldAnnotation
                                                                        .default_value().equals(
                                                                                "\0"))
                                                                        ? " DEFAULT '"
                                                                                + dateFieldAnnotation
                                                                                        .default_value()
                                                                                + "'"
                                                                        : "",

                                                                dateFieldAnnotation.required()
                                                                        ? " NOT NULL"
                                                                        : "",

                                                                dateFieldAnnotation.unique()
                                                                        ? " UNIQUE"
                                                                        : ""

                                                        );

                                                if (dateFieldAnnotation != null
                                                        && dateFieldAnnotation.comment() != null
                                                        && !dateFieldAnnotation.comment().trim()
                                                                .isEmpty()) {

                                                    postgresqlOrOracleColumnsComments += String
                                                            .format(

                                                            "COMMENT ON COLUMN %s.%s IS '%s';\n\n",

                                                                    tableName,

                                                                    field.getName()
                                                                            .replaceAll(
                                                                                    "([a-z0-9]+)([A-Z])",
                                                                                    "$1_$2")
                                                                            .toLowerCase(),

                                                                    dateFieldAnnotation.comment());
                                                }

                                            } else if (databaseEngine.equalsIgnoreCase("h2")) {

                                                format = "    %s TIMESTAMP%s%s%s,\n";

                                                if (dateFieldAnnotation.auto_now()) {
                                                    defaultDate = " AS CURRENT_TIMESTAMP()";
                                                } else {
                                                    if (dateFieldAnnotation.auto_now_add()) {
                                                        defaultDate = " DEFAULT CURRENT_TIMESTAMP";
                                                    }
                                                }

                                                sql += String.format(

                                                        format,

                                                        field.getName()
                                                                .replaceAll("([a-z0-9]+)([A-Z])",
                                                                        "$1_$2").toLowerCase(),

                                                        dateFieldAnnotation.required()
                                                                ? " NOT NULL"
                                                                : "",

                                                        defaultDate,

                                                        dateFieldAnnotation.unique()
                                                                ? " UNIQUE"
                                                                : "");

                                            } else {

                                            }

                                        }

                                        if (fieldAnnotation instanceof ForeignKeyField) {

                                            ForeignKeyField foreign_key_field_annotation = (ForeignKeyField) fieldAnnotation;

                                            String column_name = "";

                                            String referenced_column = "";

                                            if (foreign_key_field_annotation.column_name().equals(
                                                    "")) {

                                                column_name = field.getName()
                                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                                        .toLowerCase();

                                                column_name = String.format("%s_id", column_name);

                                            } else {

                                                column_name = foreign_key_field_annotation
                                                        .column_name()
                                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                                        .toLowerCase();

                                            }

                                            if (foreign_key_field_annotation.referenced_column()
                                                    .equals("")) {

                                                referenced_column = "id";

                                            } else {

                                                referenced_column = foreign_key_field_annotation
                                                        .referenced_column()
                                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                                        .toLowerCase();

                                            }

                                            sql += String
                                                    .format(

                                                    "    %s INT NOT NULL%s,\n",

                                                            column_name,

                                                            (foreign_key_field_annotation.comment() != null
                                                                    && !foreign_key_field_annotation
                                                                            .comment().equals("") && databaseEngine
                                                                    .trim().equalsIgnoreCase(
                                                                            "mysql"))
                                                                    ? " COMMENT '"
                                                                            + foreign_key_field_annotation
                                                                                    .comment()
                                                                            + "'" : "");

                                            String on_delete_string = "";

                                            if (foreign_key_field_annotation.on_delete().equals(
                                                    Models.PROTECT)) {

                                                on_delete_string = " ON DELETE RESTRICT";

                                            } else if (foreign_key_field_annotation.on_delete()
                                                    .equals(Models.SET_NULL)) {

                                                on_delete_string = " ON DELETE SET NULL";

                                            } else if (foreign_key_field_annotation.on_delete()
                                                    .equals(Models.CASCADE)) {

                                                on_delete_string = " ON DELETE CASCADE";

                                            } else if (foreign_key_field_annotation.on_delete()
                                                    .equals(Models.SET_DEFAULT)) {

                                                on_delete_string = " ON DELETE SET DEFAULT";

                                            }

                                            String on_update_string = " ON UPDATE";

                                            if (foreign_key_field_annotation.on_update().equals(
                                                    Models.PROTECT)) {

                                                on_update_string = " ON UPDATE RESTRICT";

                                            } else if (foreign_key_field_annotation.on_update()
                                                    .equals(Models.SET_NULL)) {

                                                on_update_string = " ON UPDATE SET NULL";

                                            } else if (foreign_key_field_annotation.on_update()
                                                    .equals(Models.CASCADE)) {

                                                on_update_string = " ON UPDATE CASCADE";

                                                if (databaseEngine != null
                                                        && databaseEngine
                                                                .equalsIgnoreCase("oracle")) {

                                                    on_update_string = "";
                                                }

                                            } else if (foreign_key_field_annotation.on_update()
                                                    .equals(Models.SET_DEFAULT)) {

                                                on_update_string = " ON UPDATE SET DEFAULT";

                                            }

                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {

                                                sqlForeignKey += String
                                                        .format(

                                                        "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n\n",

                                                                tableName,

                                                                foreign_key_field_annotation
                                                                        .constraint_name(),

                                                                column_name,

                                                                foreign_key_field_annotation
                                                                        .references()
                                                                        .replaceAll(
                                                                                "([a-z0-9]+)([A-Z])",
                                                                                "$1_$2")
                                                                        .toLowerCase(),

                                                                referenced_column,

                                                                on_delete_string,

                                                                on_update_string);

                                            } else if (databaseEngine.trim().equalsIgnoreCase(
                                                    "postgresql")) {

                                                sqlForeignKey += String
                                                        .format(

                                                        // "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s%s;\n\n",

                                                        "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n\n",

                                                                tableName,

                                                                foreign_key_field_annotation
                                                                        .constraint_name(),

                                                                column_name,

                                                                foreign_key_field_annotation
                                                                        .references()
                                                                        .replaceAll(
                                                                                "([a-z0-9]+)([A-Z])",
                                                                                "$1_$2")
                                                                        .toLowerCase(),

                                                                referenced_column,

                                                                on_delete_string,

                                                                on_update_string

                                                        // " DEFERRABLE INITIALLY DEFERRED"
                                                        );

                                            } else if (databaseEngine.trim().equalsIgnoreCase(
                                                    "oracle")) {

                                                sqlForeignKey += String
                                                        .format(

                                                        // "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s%s;\n\n",

                                                        "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n\n",

                                                                tableName,

                                                                foreign_key_field_annotation
                                                                        .constraint_name(),

                                                                column_name,

                                                                foreign_key_field_annotation
                                                                        .references()
                                                                        .replaceAll(
                                                                                "([a-z0-9]+)([A-Z])",
                                                                                "$1_$2")
                                                                        .toLowerCase(),

                                                                referenced_column,

                                                                on_delete_string,

                                                                on_update_string

                                                        // " DEFERRABLE INITIALLY DEFERRED"
                                                        );

                                            }

                                            sqlIndex += String.format(

                                            "CREATE INDEX idx_%s_%s ON %s (%s);\n\n",

                                            tableName,

                                            column_name,

                                            tableName,

                                            column_name);

                                            java.io.RandomAccessFile out = null;

                                            try {

                                                boolean generate_code = true;

                                                String class_path = appModelFile
                                                        .getAbsolutePath();

                                                class_path = class_path.replace(appModelFile
                                                        .getName(), String.format("%s.java",
                                                        foreign_key_field_annotation.model()));

                                                // Criando arquivo de acesso
                                                // aleatório.
                                                out = new java.io.RandomAccessFile(class_path, "rw");

                                                // Posicionando o ponteiro de
                                                // registro no início do
                                                // arquivo.
                                                out.seek(0);

                                                String current_line = null;

                                                while ((current_line = out.readLine()) != null) {

                                                    if (current_line.contains(String.format(
                                                            "%s_set", modelClass.getSimpleName()
                                                                    .toLowerCase()))) {
                                                        generate_code = false;
                                                    }
                                                }

                                                // Posicionando o ponteiro de
                                                // registro no fim do arquivo.
                                                if (out.length() > 0) {
                                                    out.seek(out.length() - 1);
                                                } else {
                                                    out.seek(out.length());
                                                }

                                                StringBuilder method_str = new StringBuilder();

                                                method_str.append("\n");

                                                method_str
                                                        .append("\t@SuppressWarnings(\"rawtypes\")\n");

                                                method_str
                                                        .append(String
                                                                .format("\tpublic jedi.db.models.QuerySet %s_set() {\n",
                                                                        modelClass.getSimpleName()
                                                                                .toLowerCase()));

                                                method_str
                                                        .append(String
                                                                .format("\t\treturn %s.objects.get_set(%s.class, this.id);\n",
                                                                        modelClass.getSimpleName(),
                                                                        foreign_key_field_annotation
                                                                                .model()));

                                                method_str.append("\t}\n");

                                                method_str.append("}");

                                                if (generate_code) {
                                                    out.writeBytes(method_str.toString());
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

                                            ManyToManyField many_to_many_field_annotation = (ManyToManyField) fieldAnnotation;

                                            String fmt = "";

                                            if (databaseEngine.trim().equalsIgnoreCase("mysql")) {

                                                fmt = "CREATE TABLE IF NOT EXISTS %s_%s (\n";

                                                fmt += "    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n";

                                                fmt += "    %s_id INT NOT NULL,\n";

                                                fmt += "    %s_id INT NOT NULL,\n";

                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";

                                                fmt += ");\n\n";

                                            } else if (databaseEngine.trim().equalsIgnoreCase(
                                                    "postgresql")) {

                                                fmt = "CREATE TABLE %s_%s (\n";

                                                fmt += "    id SERIAL NOT NULL PRIMARY KEY,\n";

                                                fmt += "    %s_id INT NOT NULL,\n";

                                                fmt += "    %s_id INT NOT NULL,\n";

                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";

                                                fmt += ");\n\n";

                                            } else if (databaseEngine.trim().equalsIgnoreCase(
                                                    "oracle")) {

                                                fmt = "CREATE TABLE %s_%s (\n";

                                                fmt += "    id NUMBER(10, 0) NOT NULL PRIMARY KEY,\n";

                                                fmt += "    %s_id INT NOT NULL,\n";

                                                fmt += "    %s_id INT NOT NULL,\n";

                                                fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";

                                                fmt += ");\n\n";

                                            } else if (databaseEngine.trim()
                                                    .equalsIgnoreCase("h2")) {

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

                                                    many_to_many_field_annotation
                                                            .references()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase(),

                                                    modelClass
                                                            .getSimpleName()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase(),

                                                    many_to_many_field_annotation
                                                            .model()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase(),

                                                    tableName,

                                                    many_to_many_field_annotation
                                                            .references()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase(),

                                                    modelClass
                                                            .getSimpleName()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase(),

                                                    many_to_many_field_annotation
                                                            .model()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase());

                                            // Sequência
                                            sqlOracleSequences += String
                                                    .format(

                                                    "CREATE SEQUENCE seq_%s_%s MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;\n\n",

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase());

                                            // Trigger de auto-incremento
                                            sqlOracleAutoIncrementTriggers.put(

                                            String.format(
                                                    "%s_%s",
                                                    tableName,
                                                    many_to_many_field_annotation
                                                            .references()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase()),

                                            String.format(
                                                    "tgr_autoincr_%s_%s",
                                                    tableName,
                                                    many_to_many_field_annotation
                                                            .references()
                                                            .replaceAll("([a-z0-9]+)([A-Z])",
                                                                    "$1_$2").toLowerCase()));

                                            sqlForeignKey += String
                                                    .format(

                                                    "ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id);\n\n",

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            tableName,

                                                            modelClass
                                                                    .getSimpleName()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            tableName);

                                            sqlIndex += String
                                                    .format(

                                                    "CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id);\n\n",

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            modelClass
                                                                    .getSimpleName()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            modelClass
                                                                    .getSimpleName()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase());

                                            sqlForeignKey += String
                                                    .format(

                                                    "ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id);\n\n",

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            many_to_many_field_annotation
                                                                    .model()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase());

                                            sqlIndex += String
                                                    .format(

                                                    "CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id);\n\n",

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            many_to_many_field_annotation
                                                                    .model()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            tableName,

                                                            many_to_many_field_annotation
                                                                    .references()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase(),

                                                            many_to_many_field_annotation
                                                                    .model()
                                                                    .replaceAll(
                                                                            "([a-z0-9]+)([A-Z])",
                                                                            "$1_$2").toLowerCase());

                                            java.io.RandomAccessFile out = null;

                                            try {

                                                boolean generate_code = true;

                                                String class_path = appModelFile
                                                        .getAbsolutePath();

                                                class_path = class_path.replace(appModelFile
                                                        .getName(), String.format("%s.java",
                                                        many_to_many_field_annotation.model()));

                                                // Criando arquivo de acesso
                                                // aleatório.
                                                out = new java.io.RandomAccessFile(class_path, "rw");

                                                // Posicionando o ponteiro de
                                                // registro no início do
                                                // arquivo.
                                                out.seek(0);

                                                String current_line = null;

                                                while ((current_line = out.readLine()) != null) {

                                                    if (current_line.contains(String.format(
                                                            "%s_set", modelClass.getSimpleName()
                                                                    .toLowerCase()))) {
                                                        generate_code = false;
                                                    }
                                                }

                                                // Posicionando o ponteiro de
                                                // registro no fim do arquivo.
                                                if (out.length() > 0) {
                                                    out.seek(out.length() - 1);
                                                } else {
                                                    out.seek(out.length());
                                                }

                                                StringBuilder method_str = new StringBuilder();

                                                method_str.append("\n");

                                                method_str
                                                        .append("\t@SuppressWarnings(\"rawtypes\")\n");

                                                method_str
                                                        .append(String
                                                                .format("\tpublic jedi.db.models.QuerySet %s_set() {\n",
                                                                        modelClass.getSimpleName()
                                                                                .toLowerCase()));

                                                method_str
                                                        .append(String
                                                                .format("\t\treturn %s.objects.get_set(%s.class, this.id);\n",
                                                                        modelClass.getSimpleName(),
                                                                        many_to_many_field_annotation
                                                                                .model()));

                                                method_str.append("\t}\n");

                                                method_str.append("}");

                                                if (generate_code) {
                                                    out.writeBytes(method_str.toString());
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

                                sql = sql.substring(0, sql.lastIndexOf(",")) + "\n";

                                if (tableAnnotation != null) {

                                    if (databaseEngine.trim().equalsIgnoreCase("mysql")) {

                                        sql += String.format(

                                        ") %s %s %s;\n\n",

                                        tableAnnotation.engine().trim().equals("")
                                                ? ""
                                                : "ENGINE=" + tableAnnotation.engine(),

                                        tableAnnotation.charset().trim().equals("")
                                                ? ""
                                                : "DEFAULT CHARSET=" + tableAnnotation.charset(),

                                        tableAnnotation.comment().trim().equals("")
                                                ? ""
                                                : "COMMENT '" + tableAnnotation.comment() + "'");

                                    } else if (databaseEngine.trim()
                                            .equalsIgnoreCase("postgresql")
                                            || databaseEngine.trim().equalsIgnoreCase("oracle")) {

                                        sql += String.format(

                                                ");\n\n%s",

                                                tableAnnotation.comment().trim().equals("")
                                                        ? ""
                                                        : String.format(
                                                                "COMMENT ON TABLE %s IS '%s';\n\n",
                                                                tableName,
                                                                tableAnnotation.comment()));

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

                            String sql_transaction = "";

                            if (databaseEngine.trim().equalsIgnoreCase("mysql")
                                    || databaseEngine.trim().equalsIgnoreCase("postgresql")) {
                                sql_transaction = "BEGIN;\n\n";
                            }

                            sql_transaction += sql;

                            sql_transaction += sqlManyToManyAssociation;

                            sql_transaction += sqlForeignKey;

                            sql_transaction += sqlIndex;

                            if (databaseEngine.trim().equalsIgnoreCase("oracle")) {

                                sql_transaction += sqlOracleSequences;

                            }

                            sql_transaction += "COMMIT";

                            if (databaseEngine.trim().equalsIgnoreCase("oracle")) {

                                sql_transaction = sql_transaction.toUpperCase();
                            }

                            // Mostrando o SQL completo da criação da estrutura
                            // de banco de dados da aplicação.

                            // System.out.println(sql_transaction);

                            Scanner scanner = new Scanner(sql_transaction);

                            scanner.useDelimiter(";\n");

                            // Comando atual (current statement).
                            String current_statement = "";

                            System.out.println("");

                            while (scanner.hasNext()) {

                                current_statement = scanner.next();

                                // Mostrando o cada sendo executada do SQL da
                                // transação.
                                System.out.println(current_statement + ";\n");

                                stmt.execute(current_statement);

                            }

                            scanner.close();

                            if (databaseEngine.trim().equalsIgnoreCase("oracle")) {

                                String oracle_triggers = "";

                                for (Map.Entry<String, String> sql_oracle_auto_increment_trigger : sqlOracleAutoIncrementTriggers
                                        .entrySet()) {

                                    String table = sql_oracle_auto_increment_trigger.getKey();

                                    String trigger = sql_oracle_auto_increment_trigger.getValue();

                                    String oracle_trigger = "" +

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

                                    oracle_triggers += oracle_trigger.toUpperCase() + "\n\n";

                                    oracle_triggers += String.format("ALTER TRIGGER %s ENABLE\n\n",
                                            trigger.toUpperCase());

                                    stmt.executeUpdate(oracle_trigger);

                                }

                                System.out.println(oracle_triggers);

                                if (!conn.getAutoCommit()) {

                                    conn.commit();

                                }
                            } else if (databaseEngine.trim().equalsIgnoreCase("mysql")
                                    && mysqlVersionNumber < 56) {

                                if (mysqlDatetimeTriggers.size() > 0) {

                                    for (String trigger : mysqlDatetimeTriggers) {
                                        stmt.executeUpdate(trigger);
                                    }

                                    System.out.println(mysqlDatetimeTriggers.toString()
                                            .replace("[", "").replace("]", "").replace(", ", ""));

                                    if (!conn.getAutoCommit()) {
                                        conn.commit();

                                        System.out.println("COMMIT;");
                                    }
                                }
                            }

                        } catch (SQLException e) {

                            e.printStackTrace();

                        } finally {

                            try {

                                stmt.close();

                                conn.close();

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
}
