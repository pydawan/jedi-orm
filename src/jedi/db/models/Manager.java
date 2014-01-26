/***********************************************************************************************
 * @(#)Manager.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/25
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

package jedi.db.models;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import jedi.db.ConnectionFactory;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.engine.JediORMEngine;

/**
 * Performs queries on the database tables.
 * It's used by a Model object to perform queries on the database.
 * 
 * @author Thiago Alexandre Martins Monteiro
 * @version 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Manager {
    private Connection connection;
    private String tableName;
    
    public Class entity;

    public Manager() {
        this.connection = ConnectionFactory.getConnection();
    }

    public Manager(Connection connection) {
        this.connection = connection;
    }

    public Manager(Class entity) {
        this();
        this.entity = entity;
    }

    protected void finalize() {
        try {
            super.finalize();

            if (this.connection != null && this.connection.isValid(10)) {
                this.connection.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public <T extends Model> QuerySet<T> all() {
        return this.all(this.entity);
    }

    /**
     * Returns all the rows in a table.
     * 
     * @return QuerySet
     */
    public <T extends Model> QuerySet<T> all(Class<T> model_class) {
        QuerySet<T> querySet = null;

        if (this.connection != null) {
            try {
                String sql = "SELECT * FROM";
                // Verifies if the model class was annotated with @Table.
                Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);
                String tableName = String.format("%ss", this.entity.getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());

                if (tableAnnotation != null && !tableAnnotation.name().trim().equals("")) {
                    tableName = tableAnnotation.name().trim()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                }

                sql = String.format("%s %s", sql, tableName);
                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();

                if (resultSet == null) {
                    return null;
                }

                querySet = new QuerySet<T>();
                querySet.setEntity(this.entity);

                while (resultSet.next()) {
                    Object obj = entity.newInstance();
                    Field id = entity.getSuperclass().getDeclaredField("id");

                    // Oracle returns BigDecimal object.
                    if (this.connection.toString().startsWith("oracle")) {
                        id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.toString()
                            .substring(id.toString().lastIndexOf('.') + 1))).intValue());
                    } else {
                        // MySQL and PostgreSQL returns a Integer object.
                        id.set(obj, resultSet.getObject(id.toString().substring(id.toString()
                            .lastIndexOf('.') + 1)));
                    }

                    for (Field field : entity.getDeclaredFields()) {
                        field.setAccessible(true);

                        if (field.toString().substring(field.toString()
                            .lastIndexOf('.') + 1).equals("serialVersionUID")) {
                            continue;
                        }

                        if (field.getName().equalsIgnoreCase("objects")) {
                            continue;
                        }

                        ForeignKeyField foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);
                        ManyToManyField manyToManyAnnotation = field.getAnnotation(ManyToManyField.class);
                        Manager manager = null;

                        if (manyToManyAnnotation != null && !manyToManyAnnotation.references().isEmpty()) {
                            String className = String.format("app.models.%s", manyToManyAnnotation.model());
                            Class associatedModelClass = Class.forName(className);
                            manager = new Manager(associatedModelClass);
                            QuerySet querySetAssociatedModels = manager.raw(
                                String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)", 
                                    manyToManyAnnotation
                                        .references()
                                        .trim()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(), 
                                    manyToManyAnnotation
                                        .model()
                                        .trim()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(), 
                                    tableName, 
                                    manyToManyAnnotation
                                        .references()
                                        .trim()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(), 
                                    obj
                                        .getClass()
                                        .getSimpleName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    ((Model) obj).getId()
                                ), 
                                associatedModelClass
                            );
                            field.set(obj, querySetAssociatedModels);
                        } else if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references().isEmpty()) {
                            // Recovers the attribute class.
                            Class associatedModelClass = Class.forName(field.getType().getName());
                            manager = new Manager(associatedModelClass);
                            String s = String.format("%s_id", field.getType().getSimpleName()
                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                            Object o = resultSet.getObject(s);
                            Model associatedModel = manager.get("id", o);
                            // References a model associated by a foreign key.
                            field.set(obj, associatedModel);
                        } else {
                            // Sets the fields that aren't Model instances.
                            if ((field.getType().getSimpleName().equals("int") || field.getType().getSimpleName()
                                .equals("Integer")) && this.connection.toString().startsWith("oracle")) {
                                if (resultSet.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                    .toLowerCase()) == null) {
                                    field.set(obj, 0);
                                } else {
                                    field.set(obj, ((java.math.BigDecimal) resultSet.getObject(field.getName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase())).intValue());
                                }
                            } else {
                                field.set(obj, resultSet.getObject(field.getName()
                                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()));
                            }
                        }
                        manager = null;
                    }
                    T model = (T) obj;

                    if (model != null) {
                        model.isPersisted(true);
                    }
                    querySet.add(model);
                }
                resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (QuerySet<T>) querySet;
    }

    public <T extends Model> QuerySet<T> filter(String... fields) {
        return (QuerySet<T>) this.filter(this.entity, fields);
    }

    /**
     * @param modelClass
     * @param fields
     * @return
     */
    public <T extends Model> QuerySet<T> filter(Class<T> modelClass, String... fields) {
        QuerySet<T> querySet = null;

        if (this.connection != null && fields != null && !fields.equals("")) {
            try {
                String tableName = String.format("%ss", entity.getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);

                if (tableAnnotation != null) {
                    if (!tableAnnotation.name().trim().isEmpty()) {
                        tableName = tableAnnotation.name().trim()
                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                    }
                } else if (this.tableName != null && !this.tableName.isEmpty()) {
                    tableName = this.tableName.trim()
                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                }

                String sql = String.format("SELECT * FROM %s WHERE", tableName);
                String where = "";

                // Iterates through the pairs field=value passed.
                for (int i = 0; i < fields.length; i++) {
                    // Changes the name of the field to the corresponding pattern name on the database.
                    if (fields[i].contains("=")) {
                        fields[i] = String.format(
                            "%s%s",
                            fields[i]
                                .substring(0, fields[i].lastIndexOf("="))
                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                .toLowerCase(),
                            fields[i].substring(fields[i].lastIndexOf("="))
                        );
                    }
                    // Adds a blank space between the field name and value.
                    fields[i] = fields[i].replace("=", " = ");
                    // Replaces % by \%
                    fields[i] = fields[i].replace("%", "\\%");
                    // Adds a blank space between the values separated by commas.
                    fields[i] = fields[i].replace(",", ", ");

                    // Checks if the current pair contains __startswith, __contains or __endswith.
                    if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__contains") > -1 
                        || fields[i].indexOf("__endswith") > -1) {
                        // Creates a LIKE statement in SQL.
                        if (fields[i].indexOf("__startswith") > -1) {
                            fields[i] = fields[i].replace("__startswith = ", " LIKE ");
                            // Replaces 'value' by 'value%'.
                            fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                            fields[i] = fields[i] + "%\'";
                        } else if (fields[i].indexOf("__contains") > -1) {
                            fields[i] = fields[i].replace("__contains = ", " LIKE ");
                            // Replaces 'value' by '%value%'.
                            fields[i] = fields[i].replaceFirst("\'", "\'%");
                            fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                            fields[i] = fields[i] + "%\'";
                        } else if (fields[i].indexOf("__endswith") > -1) {
                            fields[i] = fields[i].replace("__endswith = ", " LIKE ");
                            // Replaces 'value' by '%value'.
                            fields[i] = fields[i].replaceFirst("\'", "\'%");
                        }
                    }

                    if (fields[i].indexOf("__in") > -1) {
                        // Creates a IN statement in SQL.
                        fields[i] = fields[i].replace("__in = ", " IN ");
                        // Replaces [] by ()
                        fields[i] = fields[i].replace("[", "(");
                        fields[i] = fields[i].replace("]", ")");
                    }

                    if (fields[i].indexOf("__range") > -1) {
                        // Creates a BETWEEN statement in SQL.
                        fields[i] = fields[i].replace("__range = ", " BETWEEN ");
                        // Removes [ or ] characters.
                        fields[i] = fields[i].replace("[", "");
                        fields[i] = fields[i].replace("]", "");
                        // Replaces , (comma character) by AND.
                        fields[i] = fields[i].replace(", ", " AND ");
                    }

                    if (fields[i].indexOf("__lt") > -1) {
                        fields[i] = fields[i].replace("__lt = ", " < ");
                    }

                    if (fields[i].indexOf("__lte") > -1) {
                        fields[i] = fields[i].replace("__lte = ", " <= ");
                    }

                    if (fields[i].indexOf("__gt") > -1) {
                        fields[i] = fields[i].replace("__gt = ", " > ");
                    }

                    if (fields[i].indexOf("__gte") > -1) {
                        fields[i] = fields[i].replace("__gte = ", " >= ");
                    }

                    if (fields[i].indexOf("__exact") > -1) {
                        fields[i] = fields[i].replace("__exact = ", " = ");
                    }

                    if (fields[i].indexOf("__isnull") > -1) {
                        String bool = fields[i].substring(fields[i].indexOf("=") + 1, fields[i].length()).trim();

                        if (bool.equalsIgnoreCase("true")) {
                            fields[i] = fields[i].replace("__isnull = ", " IS NULL ");
                        }

                        if (bool.equalsIgnoreCase("false")) {
                            fields[i] = fields[i].replace("__isnull = ", " IS NOT NULL ");
                        }
                        fields[i] = fields[i].replace(bool, "");
                    }
                    where += fields[i] + " AND ";
                    where = where.replace(" AND OR AND", " OR");
                    where = where.replace(" AND AND AND", " AND");
                }
                where = where.substring(0, where.lastIndexOf("AND"));
                sql = String.format("%s %s", sql, where);
                // Shows the generated SQL statement.
                // System.out.println(sql);
                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();
                querySet = new QuerySet<T>();
                querySet.setEntity(this.entity);

                while (resultSet.next()) {
                    Object obj = entity.newInstance();

                    if (resultSet.getObject("id") != null) {
                        Field id = entity.getSuperclass().getDeclaredField("id");

                        if (this.connection.toString().startsWith("oracle")) {
                            id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
                        } else {
                            id.set(obj, resultSet.getObject(id.getName()));
                        }
                    }

                    // Iterates through the fields of the model.
                    for (Field field : entity.getDeclaredFields()) {
                        // Sets private or protected fields as accessible.
                        field.setAccessible(true);

                        // Discards the serialVersionUID field.
                        if (field.getName().equals("serialVersionUID"))
                            continue;

                        // Discards the objects field.
                        if (field.getName().equalsIgnoreCase("objects"))
                            continue;

                        // Checks if the field are annotated as ForeignKeyField or ManyToManyField.
                        ForeignKeyField foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);
                        ManyToManyField manyToManyAnnotation = field.getAnnotation(ManyToManyField.class);
                        Manager manager = null;

                        if (manyToManyAnnotation != null && !manyToManyAnnotation.references().isEmpty()) {
                            Class associatedModelClass = Class.forName(
                                String.format("app.models.%s", manyToManyAnnotation.model())
                            );
                            manager = new Manager(associatedModelClass);
                            List<List<HashMap<String, Object>>> recordSet = null;
                            // Performs a SQL query.
                            recordSet = manager.raw(
                                String.format(
                                    "SELECT %s_id FROM %s_%s WHERE %s_id = %d",
                                    manyToManyAnnotation
                                        .model()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    tableName,
                                    manyToManyAnnotation
                                        .references()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    this.entity.getSimpleName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    ((Model) obj).id()
                                )
                            );

                            String args = recordSet.toString();
                            args = args.replace("[", "");
                            args = args.replace("{", "");
                            args = args.replace("]", "");
                            args = args.replace("}", "");
                            args = args.replace("=", "");
                            args = args.replace(", ", ",");
                            args = args.replace(String.format("%s_id", manyToManyAnnotation.model()
                                .trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()), "");
                            args = String.format("id__in=[%s]", args);
                            
                            QuerySet querySetAssociatedModels = manager.filter(args);
                            field.set(obj, querySetAssociatedModels);
                        } else if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references().isEmpty()) {
                            // If it's recovers the field's class.
                            Class associatedModelClass = Class.forName(field.getType().getName());
                            // Instanciates a Model Manager.
                            manager = new Manager(associatedModelClass);
                            // Calls the get method recursivelly.
                            Model associatedModel = manager.get("id", resultSet.getObject(String.format("%s_id", 
                                field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                .toLowerCase())));
                            // References the model associated by foreign key annotation.
                            field.set(obj, associatedModel);
                        } else {
                            // Sets fields the aren't Model's instances.
                            if ((field.getType().getSimpleName().equals("int") 
                                || field.getType().getSimpleName().equals("Integer")) 
                                && this.connection.toString().startsWith("oracle")) {
                                if (resultSet.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                    .toLowerCase()) == null) {
                                    field.set(obj, 0);
                                } else {
                                    field.set(obj, ((java.math.BigDecimal) resultSet.getObject(field.getName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase())).intValue());
                                }
                            } else {
                                field.set(obj, resultSet.getObject(field.getName()
                                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()));
                            }
                        }
                        manager = null;
                    }
                    T model = (T) obj;

                    if (model != null) {
                        model.isPersisted(true);
                    }
                    querySet.add(model);
                }
                if (querySet != null) {
                    querySet.is_persisted(true);
                }
                resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (QuerySet<T>) querySet;
    }

    public <T extends Model> T create(String... list) {
        return (T) this.create(entity, list);
    }
    
    /**
     * @param modelClass
     * @param list
     * @return
     */
    public <T extends Model> T create(Class<T> modelClass, String... list) {
        Object obj = null;

        if (this.connection != null && list != null && !list.equals("")) {
            try {
                // Defines the name of the table associated with the model.
                String tableName = this.entity.getName().replace(this.entity.getPackage().getName() + ".", "") + "s";
                tableName = tableName.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);

                if (tableAnnotation != null) {
                    if (!tableAnnotation.name().trim().equals("")) {
                        tableName = tableAnnotation.name().toLowerCase();
                    }
                } else if (this.tableName != null && !this.tableName.equals("")) {
                    tableName = this.tableName;
                }
                String sql = String.format("INSERT INTO %s", tableName);
                String fields = "";
                String field = "";
                String values = "";
                String value = "";
                // Instanciates a model object managed by this Manager.
                obj = this.entity.newInstance();

                for (int i = 0; i < list.length; i++) {
                    field = list[i].split("=")[0];
                    value = list[i].split("=")[1];
                    Field f = null;

                    if (field.endsWith("_id")) {
                        f = this.entity.getDeclaredField(field.replace("_id", ""));
                    } else {
                        f = this.entity.getDeclaredField(field);
                    }
                    // Changes the field name to reflect the pattern to the table column names.
                    field = String.format("%s", field.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                    
                    // Handles the insertion the the ForeignKeyField and ManyToManyField.
                    ForeignKeyField foreignKeyAnnotation = f.getAnnotation(ForeignKeyField.class);

                    // Allows access to the private and protected fields (attributes).
                    f.setAccessible(true);

                    // Discards serialVersionUID field.
                    if (f.getName().equals("serialVersionUID"))
                        continue;

                    // Discards objects field.
                    if (f.getName().equalsIgnoreCase("objects"))
                        continue;

                    // Converts the data to the appropriate type.
                    if (value.matches("\\d+")) {
                        if (foreignKeyAnnotation != null) {
                            Manager manager = new Manager(f.getType());
                            f.set(obj, manager.get("id", value));
                        } else {
                            f.set(obj, Integer.parseInt(value)); // Integer
                        }
                    } else if (value.matches("\\d+f")) { // Float
                        f.set(obj, Float.parseFloat(value));
                    } else if (value.matches("\\d+.d+")) { // Double
                        f.set(obj, Double.parseDouble(value));
                    } else { // String
                        f.set(obj, list[i].split("=")[1]);
                    }
                    fields += field + ", ";
                    values += value + ", ";
                }
                fields = fields.substring(0, fields.lastIndexOf(","));
                values = values.substring(0, values.lastIndexOf(","));
                sql = String.format("%s (%s) VALUES (%s)", sql, fields, values);

                // Shows the generated SQL statement on the STDOUT (Standard Output).
                // System.out.println(sql);

                // Executes the SQL statement.
                this.connection.prepareStatement(sql).execute();

                Field f = this.entity.getSuperclass().getDeclaredField("id");

                /* Gets the primary key (pk) of the last row inserted and 
                 * assigns it to the model.
                 */
                f.set(obj, this.getLastInsertedID());
                T model = (T) obj;

                if (model != null) {
                    model.isPersisted(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (T) obj;
    }

    /**
     * Returns the id of the last inserted row.
     * @return int
     */
    public int getLastInsertedID() {
        int id = 0;

        if (this.connection != null) {
            try {
                String tableName = String.format("%ss", this.entity.getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);

                if (tableAnnotation != null) {
                    if (!tableAnnotation.name().trim().equals("")) {
                        tableName = tableAnnotation.name().toLowerCase();
                    }
                } else if (this.tableName != null && !this.tableName.trim().equals("")) {
                    tableName = this.tableName;
                }
                String sql = "";
                Properties databaseSettings = new Properties();
                FileInputStream fileInputStream = new FileInputStream(JediORMEngine.APP_DB_CONFIG);

                databaseSettings.load(fileInputStream);

                String databaseEngine = databaseSettings.getProperty("database.engine");

                if (databaseEngine != null) {
                    if (databaseEngine.trim().equalsIgnoreCase("mysql") 
                        || databaseEngine.trim().equalsIgnoreCase("postgresql")
                        || databaseEngine.trim().equalsIgnoreCase("h2")) {
                        sql = String.format("SELECT id FROM %s ORDER BY id DESC LIMIT 1", tableName);
                    } else if (databaseEngine.trim().equalsIgnoreCase("oracle")) {
                        sql = String.format("SELECT MAX(id) AS id FROM %s", tableName);
                    } else {
                    }
                } else {
                    return id;
                }
                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();

                while (resultSet.next()) {
                    id = resultSet.getInt("id");
                }
                resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    /**
     * @param conditions
     * @return int
     */
    public int count(String... conditions) {
        int rows = 0;

        if (this.connection != null) {
            try {
                String tableName = String.format("%ss", this.entity.getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);

                if (tableAnnotation != null) {
                    if (!tableAnnotation.name().trim().equals("")) {
                        tableName = tableAnnotation.name().trim()
                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                    } else if (this.tableName != null && !this.tableName.trim().equals("")) {
                        tableName = this.tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                            .toLowerCase();
                    }
                }
                String sql = String.format("SELECT COUNT(id) AS \"rows\" FROM %s", tableName);

                if (conditions != null && conditions.length > 0) {
                    String where = "WHERE";

                    for (int i = 0; i < conditions.length; i++) {
                        if (!conditions[i].equals("")) {
                            /* Changes the name of the field to reflect the name 
                             * pattern of the table columns.
                             */
                            if (conditions[i].contains("=")) {
                                conditions[i] = String.format(
                                    "%s%s",
                                    conditions[i]
                                        .substring(0, conditions[i].lastIndexOf("="))
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    conditions[i].substring(conditions[i].lastIndexOf("="))
                                );
                            }
                            // Adds a blank space between the field's name and value.
                            conditions[i] = conditions[i].replace("=", " = ");
                            // Replaces % by \%
                            conditions[i] = conditions[i].replace("%", "\\%");
                            // Adds a blank space between the values separated by comma character.
                            conditions[i] = conditions[i].replace(",", ", ");
                            // Checks if the current pair contains __startswith, __contains or __endswith.
                            if (conditions[i].indexOf("__startswith") > -1 || conditions[i].indexOf("__contains") > -1
                                    || conditions[i].indexOf("__endswith") > -1) {
                                // Creates the LIKE SQL statement.
                                if (conditions[i].indexOf("__startswith") > -1) {
                                    conditions[i] = conditions[i].replace("__startswith = ", " LIKE ");
                                    // Replaces 'value' by 'value%'.
                                    conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                                    conditions[i] = conditions[i] + "%\'";
                                } else if (conditions[i].indexOf("__contains") > -1) {
                                    conditions[i] = conditions[i].replace("__contains = ", " LIKE ");
                                    // Replaces 'value' by '%value%'.
                                    conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                                    conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'"));
                                    conditions[i] = conditions[i] + "%\'";
                                } else if (conditions[i].indexOf("__endswith") > -1) {
                                    conditions[i] = conditions[i].replace("__endswith = ", " LIKE ");
                                    // Replaces 'value' by '%value'.
                                    conditions[i] = conditions[i].replaceFirst("\'", "\'%");
                                }
                            }

                            if (conditions[i].indexOf("__in") > -1) {
                                // Creates the IN SQL statement.
                                conditions[i] = conditions[i].replace("__in = ", " IN ");
                                // Replaces the [] characters by ().
                                conditions[i] = conditions[i].replace("[", "(");
                                conditions[i] = conditions[i].replace("]", ")");
                            } else
                            if (conditions[i].indexOf("__range") > -1) {
                                // Creates the BETWEEN SQL statement.
                                conditions[i] = conditions[i].replace("__range = ", " BETWEEN ");
                                // Removes the [ or ] characters.
                                conditions[i] = conditions[i].replace("[", "");
                                conditions[i] = conditions[i].replace("]", "");
                                // Replaces the comma character by AND.
                                conditions[i] = conditions[i].replace(", ", " AND ");
                            }

                            if (conditions[i].indexOf("__lt") > -1) {
                                conditions[i] = conditions[i].replace("__lt = ", " < ");
                            }

                            if (conditions[i].indexOf("__lte") > -1) {
                                conditions[i] = conditions[i].replace("__lte = ", " <= ");
                            }

                            if (conditions[i].indexOf("__gt") > -1) {
                                conditions[i] = conditions[i].replace("__gt = ", " > ");
                            }

                            if (conditions[i].indexOf("__gte") > -1) {
                                conditions[i] = conditions[i].replace("__gte = ", " >= ");
                            }

                            if (conditions[i].indexOf("__exact") > -1) {
                                conditions[i] = conditions[i].replace("__exact = ", " = ");
                            }
                            where += " " + conditions[i] + " AND";
                            where = where.replace(" AND OR AND", " OR");
                            where = where.replace(" AND AND AND", " AND");
                        }
                    }

                    if (where.indexOf(" AND") > -1) {
                        where = where.substring(0, where.lastIndexOf("AND"));
                        sql = String.format("%s %s", sql, where);
                    }
                }

                // Shows the generated SQL statement on the STDOUT (STANDARD OUTPUT).
                // System.out.println(sql);

                // Executes the SQL statement.
                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();

                while (resultSet.next()) {
                    rows = resultSet.getInt("rows");
                }
                resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rows;
    }

    /**
     * @return int
     */
    public int count() {
        return count("");
    }

    /**
     * @param fields
     * @return
     */
    public <T extends Model> QuerySet<T> exclude(String... fields) {
        QuerySet<T> querySet = null;

        if (this.connection != null && fields != null && !fields.equals("")) {
            try {
                String tableName = String.format("%ss", this.entity.getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);

                if (tableAnnotation != null) {
                    if (!tableAnnotation.name().trim().equals("")) {
                        tableName = tableAnnotation.name().trim()
                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                    } else if (this.tableName != null && !this.tableName.trim().equals("")) {
                        tableName = this.tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                    }
                }
                String sql = String.format("SELECT * FROM %s WHERE", tableName);
                String where = "";

                // Iterates through the pairs field=value.
                for (int i = 0; i < fields.length; i++) {
                    // Creates the column name.
                    if (fields[i].contains("=")) {
                        fields[i] = String.format(
                            "%s%s",
                            fields[i]
                                .substring(0, fields[i].lastIndexOf("="))
                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                .toLowerCase(),
                            fields[i].substring(fields[i].lastIndexOf("="))
                        );
                    }
                    // Adds a blank space between the field name and value.
                    fields[i] = fields[i].replace("=", " = ");
                    // Replaces % by \%
                    fields[i] = fields[i].replace("%", "\\%");
                    // Adds a blank space between the values separated by comma character.
                    fields[i] = fields[i].replace(",", ", ");

                    // Checks if the current pair contains __startswith, __contains ou __endswith.
                    if (fields[i].indexOf("__startswith") > -1 
                        || fields[i].indexOf("__contains") > -1 
                        || fields[i].indexOf("__endswith") > -1) {
                        // Creates a LIKE SQL statement.
                        if (fields[i].indexOf("__startswith") > -1) {
                            fields[i] = fields[i].replace("__startswith = ", " LIKE ");
                            // Replaces 'value' by 'value%'.
                            fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                            fields[i] = fields[i] + "%\'";
                        } else if (fields[i].indexOf("__contains") > -1) {
                            fields[i] = fields[i].replace("__contains = ", " LIKE ");
                            // Replaces 'value' by '%value%'.
                            fields[i] = fields[i].replaceFirst("\'", "\'%");
                            fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'"));
                            fields[i] = fields[i] + "%\'";
                        } else if (fields[i].indexOf("__endswith") > -1) {
                            fields[i] = fields[i].replace("__endswith = ", " LIKE ");
                            // Replaces 'value' by '%value'.
                            fields[i] = fields[i].replaceFirst("\'", "\'%");
                        }
                    }

                    if (fields[i].indexOf("__in") > -1) {
                        // Creates a IN SQL statement.
                        fields[i] = fields[i].replace("__in = ", " IN ");
                        // Replaces [] characters by () characters.
                        fields[i] = fields[i].replace("[", "(");
                        fields[i] = fields[i].replace("]", ")");
                    }

                    if (fields[i].indexOf("__range") > -1) {
                        // Creates a BETWEEN SQL statement.
                        fields[i] = fields[i].replace("__range = ", " BETWEEN ");
                        // Removes the [ character.
                        fields[i] = fields[i].replace("[", "");
                        // Removes the ] character.
                        fields[i] = fields[i].replace("]", "");
                        // Substituindo o caracter , por AND.
                        fields[i] = fields[i].replace(", ", " AND ");
                    }

                    if (fields[i].indexOf("__lt") > -1) {
                        fields[i] = fields[i].replace("__lt = ", " < ");
                    }

                    if (fields[i].indexOf("__lte") > -1) {
                        fields[i] = fields[i].replace("__lte = ", " <= ");
                    }

                    if (fields[i].indexOf("__gt") > -1) {
                        fields[i] = fields[i].replace("__gt = ", " > ");
                    }

                    if (fields[i].indexOf("__gte") > -1) {
                        fields[i] = fields[i].replace("__gte = ", " >= ");
                    }

                    if (fields[i].indexOf("__exact") > -1) {
                        fields[i] = fields[i].replace("__exact = ", " = ");
                    }
                    where += fields[i] + " AND ";
                }
                where = where.substring(0, where.lastIndexOf("AND"));
                sql = String.format("%s NOT (%s)", sql, where);
                // Shows the generated SQL statement on the STDOUT (STANDARD OUTPUT).
                // System.out.println(sql);
                
                // Executes the SQL statement.
                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();
                querySet = new QuerySet();
                querySet.setEntity(this.entity);

                while (resultSet.next()) {
                    Object obj = entity.newInstance();

                    if (resultSet.getObject("id") != null) {
                        Field id = entity.getSuperclass().getDeclaredField("id");

                        if (this.connection.toString().startsWith("oracle")) {
                            id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.toString()
                                .substring(id.toString().lastIndexOf('.') + 1))).intValue());
                        } else {
                            id.set(obj, resultSet.getObject(id.toString()
                                .substring(id.toString().lastIndexOf('.') + 1)));
                        }
                    }

                    for (Field field : entity.getDeclaredFields()) {
                        field.setAccessible(true);

                        if (field.getName().equals("serialVersionUID")) {
                            continue;
                        }

                        if (field.getName().equals("objects")) {
                            continue;
                        }

                        ForeignKeyField foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);
                        ManyToManyField manyToManyAnnotation = field.getAnnotation(ManyToManyField.class);
                        Manager manager = null;

                        if (manyToManyAnnotation != null && !manyToManyAnnotation.references().isEmpty()) {
                            Class associatedModelClass = Class.forName(String.format("app.models.%s", 
                                manyToManyAnnotation.model()));
                            manager = new Manager(associatedModelClass);
                            QuerySet querySet_associated_models = manager.raw(
                                String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)", 
                                    manyToManyAnnotation
                                        .references()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    manyToManyAnnotation
                                        .model()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    tableName,
                                    manyToManyAnnotation
                                        .references()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    obj
                                        .getClass()
                                        .getSimpleName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                ((Model) obj).getId()),
                                associatedModelClass
                            );
                            field.set(obj, querySet_associated_models);
                        } else if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references().isEmpty()) {
                            Class associatedModelClass = Class.forName(field.getType().getName());
                            manager = new Manager(associatedModelClass);
                            Model associatedModel = manager.get(
                                String.format("id"),
                                resultSet.getObject(
                                    String.format("%s_id", field.getType().getSimpleName().toLowerCase())
                                )
                            );
                            field.set(obj, associatedModel);
                        } else {
                            if ((field.getType().getSimpleName().equals("int") 
                                || field.getType().getSimpleName().equals("Integer"))
                                && this.connection.toString().startsWith("oracle")) {
                                if (resultSet.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                    .toLowerCase()) == null) {
                                    field.set(obj, 0);
                                } else {
                                    field.set(
                                        obj,
                                        ((java.math.BigDecimal) resultSet.getObject(
                                            field
                                                .getName()
                                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                                .toLowerCase()
                                        )).intValue()
                                    );
                                }
                            } else {
                                field.set(
                                    obj, 
                                    resultSet.getObject(
                                        field
                                            .getName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase()
                                    )
                                );
                            }
                        }
                    }
                    T model = (T) obj;

                    if (model != null) {
                        model.isPersisted(true);
                    }
                    querySet.add(model);
                }
                resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return querySet;
    }

    /**
     * @param sql
     * @return
     */
    public List<List<HashMap<String, Object>>> raw(String sql) {
        // Creates a list of list of maps.
        // The first list represents a set of rows.
        // The second list represents a row (set of columns).
        // The map represents a pair of key value (the column and its value).
        List<List<HashMap<String, Object>>> recordSet = null;

        if (this.connection != null && sql != null && !sql.trim().equals("")) {
            try {
                ResultSet resultSet = null;

                // DQL - Data Query Language (SELECT).
                if (sql.startsWith("select") || sql.startsWith("SELECT")) {
                    // Returns a navigable ResultSet.
                    resultSet = this.connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, 
                        ResultSet.CONCUR_READ_ONLY).executeQuery();
                    ResultSetMetaData tableMetadata = null;

                    if (resultSet != null) {
                        tableMetadata = resultSet.getMetaData();

                        if (tableMetadata != null) {
                            // Deslocando o cursor até o último registro.
                            // result_set.last();
                            recordSet = new ArrayList<List<HashMap<String, Object>>>();
                            // O método getFetchSize() retorna o número de
                            // registros que o SGBD recupera por vez no banco de
                            // dados. Por padrão é 10.
                            // for (int i = 1; i < result_set.getFetchSize();
                            // i++) {

                            // Obtendo a quantidade de registros retornados pela
                            // consulta.
                            // int rows = result_set.getRow();

                            // Reposicionando o ponteiro de registros
                            // result_set.beforeFirst();

                            while (resultSet.next()) {
                                List<HashMap<String, Object>> tableRow = new ArrayList<HashMap<String, Object>>();
                                HashMap<String, Object> tableColumn = new HashMap<String, Object>();

                                for (int i = 1; i <= tableMetadata.getColumnCount(); i++) {
                                    tableColumn.put(
                                        tableMetadata.getColumnLabel(i), 
                                        resultSet.getObject(tableMetadata.getColumnLabel(i))
                                    );
                                }
                                tableRow.add(tableColumn);
                                recordSet.add(tableRow);
                            }
                            resultSet.close();
                        }
                    }
                } else {
                    // DML - Data Manipulation Language (INSERT, UPDATE or DELETE).
                    this.connection.prepareStatement(sql).executeUpdate();

                    if (!this.connection.getAutoCommit()) {
                        this.connection.commit();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    if (!this.connection.getAutoCommit()) {
                        this.connection.rollback();
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return recordSet;
    }

    /**
     * @param sql
     * @param modelClass
     * @return
     */
    public <T extends Model> QuerySet<T> raw(String sql, Class<T> modelClass) {
        QuerySet<T> querySet = null;

        // Verificando existe conexão com o banco de dados.
        if (this.connection != null && sql != null && !sql.trim().equals("")) {
            try {
                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();
                querySet = new QuerySet();

                while (resultSet.next()) {
                    // Criando uma instância da classe de modelo (modelClass)
                    // informado.
                    T obj = modelClass.newInstance();

                    if (resultSet.getObject("id") != null) {
                        Field id = modelClass.getSuperclass().getDeclaredField("id");

                        if (this.connection.toString().startsWith("oracle")) {
                            id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
                        } else {
                            id.set(obj, resultSet.getObject(id.getName()));
                        }
                    }

                    if (obj != null) {
                        obj.isPersisted(true);
                    }

                    // Percorrendo os atributos da classe de modelo.
                    for (Field field : modelClass.getDeclaredFields()) {
                        // Configurando como acessíveis todos os atributos
                        // (evita problemas com private e etc).
                        field.setAccessible(true);
                        // Ignorando o atributo serialVersionUID durante a
                        // iteração.
                        if (field.getName().equals("serialVersionUID")) {
                            continue;
                        }

                        // Ignorando o atributo objects durante a iteração.
                        if (field.getName().equalsIgnoreCase("objects")) {
                            continue;
                        }

                        // Verificando se o atributo é anotado com a anotação
                        // ForeignKeyField.
                        ForeignKeyField foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);
                        ManyToManyField manyToManyAnnotation = field.getAnnotation(ManyToManyField.class);
                        Table tableAnnotation = modelClass.getAnnotation(Table.class);
                        String tableName = String.format("%ss", modelClass.getSimpleName()
                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());

                        if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty()) {
                            tableName = tableAnnotation.name().trim()
                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                        }
                        Manager manager = null;

                        // O atributo está anotado como ForeignKeyField?
                        if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references().isEmpty()) {

                            // Caso seja recupera a classe do atributo.
                            Class associated_model_class = Class.forName(field.getType().getName());

                            // Instanciando um model manager.
                            manager = new Manager(associated_model_class);

                            // Chamando o método esse método (get)
                            // recursivamente.
                            Model associatedModel = manager.get(
                                String.format("id"), 
                                resultSet.getObject(
                                    String.format(
                                        "%s_id", 
                                        field
                                            .getType()
                                            .getSimpleName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase()
                                    )
                                )
                            );
                            // Referenciando o modelo associado por foreign key.
                            field.set(obj, associatedModel);
                        } else if (manyToManyAnnotation != null && !manyToManyAnnotation.references().isEmpty()) {
                            Class associatedModelClass = Class.forName(
                                String.format("app.models.%s", manyToManyAnnotation.model())
                            );
                            manager = new Manager(associatedModelClass);
                            List<List<HashMap<String, Object>>> associatedModelsRecordSet = null;
                            // É um recordset.
                            associatedModelsRecordSet = manager.raw(
                                String.format(
                                    "SELECT %s_id FROM %s_%s WHERE %s_id = %d", 
                                    manyToManyAnnotation
                                        .model()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    tableName,
                                    manyToManyAnnotation
                                        .references()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    modelClass
                                        .getSimpleName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    obj.id()
                                )
                            );

                            if (associatedModelsRecordSet != null) {
                                String args = associatedModelsRecordSet.toString().toLowerCase();
                                args = args.replace("[", "");
                                args = args.replace("{", "");
                                args = args.replace("]", "");
                                args = args.replace("}", "");
                                args = args.replace("=", "");
                                args = args.replace(", ", ",");
                                args = args.replace(
                                    String.format(
                                        "%s_id",
                                        manyToManyAnnotation
                                            .model()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase()
                                    ), 
                                    ""
                                );
                                args = String.format("id__in=[%s]", args);
                                QuerySet qs = manager.filter(args);
                                field.set(obj, qs);
                            } else {
                                field.set(obj, null);
                            }
                        } else {
                            // Configurando campos que não são instancias de
                            // Model.
                            if ((field.getType().getSimpleName().equals("int") 
                                || field.getType().getSimpleName().equals("Integer"))
                                && this.connection.toString().startsWith("oracle")) {
                                if (resultSet.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                    .toLowerCase()) == null) {
                                    field.set(obj, 0);
                                } else {
                                    field.set(
                                        obj,
                                        ((java.math.BigDecimal) resultSet.getObject(
                                            field
                                                .getName()
                                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                                .toLowerCase()
                                        )).intValue()
                                    );
                                }
                            } else {
                                field.set(
                                    obj, 
                                    resultSet.getObject(
                                        field
                                            .getName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase()
                                    )
                                );
                            }
                        }
                        manager = null;
                    }
                    T model = (T) obj;

                    if (model != null) {
                        model.isPersisted(true);
                    }
                    querySet.add(model);
                }
                resultSet.close();
                querySet.setEntity(modelClass);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return querySet;
    }

    // DEVE SER ALTERADO POIS PODE RETORNAR MAIS DE UM OBJETO.
    /**
     * @param field
     * @param value
     * @param modelClass
     * @return
     */
    public <T extends Model> T get(String field, Object value, Class<T> modelClass) {
        T obj = null;
      
        if (this.connection != null && field != null && !field.trim().isEmpty()) {
            try {
                field = field.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
                String sql = "SELECT * FROM";
                String tableName = String.format("%ss", this.entity.getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase());
                Table tableAnnotation = (Table) entity.getAnnotation(Table.class);

                if (tableAnnotation != null) {
                    if (!tableAnnotation.name().trim().equals("")) {
                        tableName = tableAnnotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                            .toLowerCase();
                    }
                } else if (this.tableName != null && !this.tableName.trim().equals("")) {
                    tableName = this.tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                        .toLowerCase();
                }

                if (value != null) {
                    sql = String.format("%s %s WHERE %s = '%s'", sql, tableName, field, value.toString());
                } else {
                    sql = String.format("%s %s WHERE %s IS NULL", sql, tableName, field);
                }

                // Caso o valor passado seja de um tipo de dados numérico retira
                // a apóstofre.
                if (Integer.class.isInstance(value) || Float.class.isInstance(value) 
                    || Double.class.isInstance(value)) {
                    sql = sql.replaceAll("\'", "");
                }

                ResultSet resultSet = this.connection.prepareStatement(sql).executeQuery();

                while (resultSet.next()) {
                    obj = (T) entity.newInstance();

                    if (resultSet.getObject("id") != null) {
                        Field id = entity.getSuperclass().getDeclaredField("id");

                        // Tratando o tipo de dado BigDecimal que é retornado
                        // pelo Oracle.
                        // Nos MySQL e no PostgreSQL o valor retornado é do tipo
                        // Integer.

                        if (this.connection.toString().startsWith("oracle")) {
                            id.set(obj, ((java.math.BigDecimal) resultSet.getObject(id.getName())).intValue());
                        } else {
                            id.set(obj, resultSet.getObject(id.getName()));
                        }
                    }

                    for (Field f : entity.getDeclaredFields()) {
                        f.setAccessible(true);

                        if (f.getName().equals("serialVersionUID"))
                            continue;

                        if (f.getName().equalsIgnoreCase("objects"))
                            continue;

                        // Verificando se o atributo é anotado com a anotação
                        // ForeignKeyField.
                        ForeignKeyField foreignKeyAnnotation = f.getAnnotation(ForeignKeyField.class);
                        ManyToManyField manyToManyAnnotation = f.getAnnotation(ManyToManyField.class);
                        Manager manager = null;

                        if (manyToManyAnnotation != null && !manyToManyAnnotation.references().trim().isEmpty()) {
                            Class associatedModelClass = Class.forName(
                                String.format("app.models.%s", manyToManyAnnotation.model())
                            );
                            manager = new Manager(associatedModelClass);
                            QuerySet associatedModelsQuerySet = manager.raw(
                                String.format(
                                    "SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
                                    manyToManyAnnotation
                                        .references()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    manyToManyAnnotation
                                        .model()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    tableName,
                                    manyToManyAnnotation
                                        .references()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    obj
                                        .getClass()
                                        .getSimpleName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                        .toLowerCase(),
                                    obj.getId()
                                ),
                                associatedModelClass
                            );
                            // Configurando o campo (atributo) com a referência
                            // para o queryset criado anteriormente.
                            f.set(obj, associatedModelsQuerySet);
                        } else if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references()
                            .trim().isEmpty()) {
                            // Caso seja recupera a classe do atributo.
                            Class associatedModelClass = Class.forName(f.getType().getName());
                            // Instanciando um model manager.
                            manager = new Manager(associatedModelClass);
                            // Chamando o método esse método (get)
                            // recursivamente.
                            Model associatedModel = manager.get(
                                String.format("id"),
                                resultSet.getObject(
                                    String.format(
                                        "%s_id", 
                                        f
                                            .getType()
                                            .getSimpleName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase()
                                    )
                                )
                            );
                            // Atributo (campo) referenciando o modelo anotado
                            // como ForeignKeyField.
                            f.set(obj, associatedModel);
                        } else {
                            // Configurando campos que não são instancias de
                            // Model.
                            if ((f.getType().getSimpleName().equals("int") || f.getType().getSimpleName()
                                .equals("Integer")) && this.connection.toString().startsWith("oracle")) {
                                if (resultSet.getObject(f.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                    .toLowerCase()) == null) {
                                    f.set(obj, 0);
                                } else {
                                    f.set(obj, ((java.math.BigDecimal) resultSet.getObject(f.getName()
                                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase())).intValue());
                                }
                            } else {
                                f.set(obj, resultSet.getObject(f.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                    .toLowerCase()));
                            }
                        }
                        manager = null;
                    }
                }

                if (obj != null) {
                    obj.isPersisted(true);
                }
                resultSet.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (T) obj;
    }

    /**
     * @param field
     * @param value
     * @return
     */
    public <T extends Model> T get(String field, Object value) {
        return (T) this.get(field, value, this.entity);
    }

    /**
     * @param field
     * @param modelClass
     * @return
     */
    public <T extends Model> T latest(String field, Class<T> modelClass) {
        T model = null;

        if (this.connection != null && field != null && !field.trim().isEmpty()) {
            // Renomeando o atributo para ficar no mesmo padrão do nome da
            // coluna na tabela associada ao modelo.
            field = field.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
            Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);
            String tableName = String.format(
                "%ss", 
                this
                    .entity
                    .getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase()
            );

            if (tableAnnotation != null) {
                tableName = tableAnnotation.name().trim()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();

            }
            String sql = String.format("SELECT * FROM %s ORDER BY %s DESC LIMIT 1", tableName, field);

            if (this.connection.toString().startsWith("oracle")) {
                sql = String.format("SELECT * FROM %s WHERE ROWNUM < 2 ORDER BY %s DESC", tableName, field);
            }

            QuerySet querySet = this.raw(sql, entity);

            if (querySet != null) {
                model = (T) querySet.get(0);

                if (model != null) {
                    model.isPersisted(true);
                }
            }
        }
        return model;
    }

    /**
     * @param field
     * @return
     */
    public <T extends Model> T latest(String field) {
        return (T) latest(field, entity);
    }

    /**
     * @return
     */
    public <T extends Model> T latest() {
        return (T) latest("id", entity);
    }

    /**
     * @param field
     * @param modelClass
     * @return
     */
    public <T extends Model> T earliest(String field, Class<T> modelClass) {
        T model = null;

        if (this.connection != null && field != null && !field.trim().isEmpty()) {
            Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);
            String tableName = String.format(
                "%ss", 
                this
                    .entity
                    .getSimpleName()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase()
            );

            if (tableAnnotation != null) {
                tableName = tableAnnotation.name().trim()
                    .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
            }
            String sql = String.format("SELECT * FROM %s ORDER BY %s ASC LIMIT 1", tableName, field);

            if (this.connection.toString().startsWith("oracle")) {
                sql = String.format("SELECT * FROM %s WHERE ROWNUM < 2 ORDER BY %s ASC", tableName, field);
            }
            QuerySet querySet = this.raw(sql, entity);

            if (querySet != null) {
                model = (T) querySet.get(0);

                if (model != null) {
                    model.isPersisted(true);
                }
            }
        }
        return model;
    }

    /**
     * @param field
     * @return
     */
    public <T extends Model> T earliest(String field) {
        return (T) earliest(field, entity);
    }

    /**
     * @return
     */
    public <T extends Model> T earliest() {
        return (T) earliest("id", entity);
    }

    /**
     * @param associatedModelClass
     * @param id
     * @return
     */
    public <T extends Model> QuerySet<T> getSet(Class<T> associatedModelClass, int id) {
        QuerySet querySet = null;

        if (associatedModelClass != null && associatedModelClass.getSuperclass().getName()
            .equals("jedi.db.models.Model")) {
            String sql = "";

            Table tableAnnotation = (Table) this.entity.getAnnotation(Table.class);
            Table tableAnnotationAssociatedModel = (Table) associatedModelClass.getAnnotation(Table.class);
            String tableName = String.format(
                "%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()
            );
            String tableNameAssociatedModel = String.format(
                "%ss", associatedModelClass.getSimpleName().toLowerCase()
            );

            if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty()) {
                tableName = tableAnnotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase();
            }

            if (tableAnnotationAssociatedModel != null && !tableAnnotationAssociatedModel.name()
                .trim().isEmpty()) {
                tableNameAssociatedModel = tableAnnotationAssociatedModel.name().trim().
                    replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
            }

            // SELECT livros.* FROM livros, livros_autores WHERE livros.id =
            // livros_autores.livro_id AND livros_autores.autor_id = 1;

            ForeignKeyField foreignKeyAnnotation = null;

            for (Field field : this.entity.getDeclaredFields()) {
                foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);

                if (foreignKeyAnnotation != null && foreignKeyAnnotation.model()
                    .equals(associatedModelClass.getSimpleName())) {
                    querySet = this.filter(
                        String.format("%s_id=%d", associatedModelClass.getSimpleName().toLowerCase(), id)
                    );
                }
            }

            if (querySet == null) {
                sql = String.format(
                    "SELECT %s.* FROM %s, %s_%s WHERE %s.id = %s_%s.%s_id AND %s_%s.%s_id = %d",
                    tableName,
                    tableName,
                    tableName,
                    tableNameAssociatedModel,
                    tableName,
                    tableName,
                    tableNameAssociatedModel,
                    this
                        .entity
                        .getSimpleName()
                        .toLowerCase()
                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                        .toLowerCase(),
                    tableName,
                    tableNameAssociatedModel,
                    associatedModelClass
                        .getSimpleName()
                        .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                        .toLowerCase(),
                    id
                );
                querySet = this.raw(sql, this.entity);
            }
        }
        return querySet;
    }

    /**
     * Método que insere no banco de dados a lista de objetos fornecida de uma
     * maneira eficiente.
     * 
     * @param models
     */

    /*
     * public void bulk_create(List<Model> objects) {
     * 
     * // Esse deve ser executado como um SQL batch.
     * 
     * // Caso haja conexão com o banco de dados e exista uma lista não vazia
     * então
     * 
     * if (connection != null && objects != null && !objects.isEmpty() ) {
     * 
     * try {
     * 
     * for (Model object : objects) {
     * 
     * object.save();
     * }
     * 
     * } catch(Exception e) {
     * 
     * e.printStackTrace();
     * }
     * }
     * }
     */

    /*
     * public <T extends Model> QuerySet<T> get_or_create() {
     * 
     * QuerySet<T> query_set = null;
     * 
     * return query_set;
     * }
     */
}