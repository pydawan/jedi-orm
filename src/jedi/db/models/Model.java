/***********************************************************************************************
 * @(#)Model.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/26
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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jedi.db.ConnectionFactory;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;

/**
 * @author Thiago Alexandre Martins Monteiro
 * @version 1.0
 */
@SuppressWarnings({"rawtypes", "unused", "unchecked", "deprecation"})
public class Model implements Comparable<Model>, Serializable {
    //  Attributes   
    private static final long serialVersionUID = 3655866130678459258L;
    protected int id;
    protected transient boolean isPersisted;
    private transient Connection connection;
    private static String tableName;
    private boolean autoCloseConnection = true;
    
    //  Constructors  
    public Model() {}   
    
    public Model(Connection connection) {
        this.connection = connection;
    }   
    
    //  Destructor
    protected void finalize() {
        // If DEBUG mode is enabled show a message.        
        try {
            super.finalize();
            if (this.connection != null && !this.connection.isValid(10) ) {
                this.connection.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }   
    
    //  Getters 
    public Connection getConnection() {     
        return connection;
    }   
    
    public Connection connection() {        
        return connection;
    }   
    
    public String tableName() {        
        return tableName;
    }   
    
    public int getId() {        
        return id;
    }   
    
    public int id() {       
        return id;
    }   
    
    public boolean getAutoCloseConnection() {       
        return autoCloseConnection;
    }   
    
    public boolean autoCloseConnection() {        
        return autoCloseConnection;
    }   
    
    public boolean isPersisted() {      
        return isPersisted;
    }   
    
    public Object get(String field) {
        Object object = null;
        
        if (field != null && !field.trim().isEmpty() ) {
            try {
                Field f = null;

                if (field.trim().equalsIgnoreCase("id") ) {
                    f = this.getClass().getSuperclass().getDeclaredField(field);
                } else {
                    f = this.getClass().getDeclaredField(field);
                }
                f.setAccessible(true);
                object = f.get(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return object;
    }
    
    //  Setters 
    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    
    public Model connection(Connection connection) {    
        this.connection = connection;
        return this;
    }
    
    public void setTableName(String tableName) {
        Model.tableName = tableName.toLowerCase();
    }
    
    
    public Model table_name(String tableName) {
        Model.tableName = tableName.toLowerCase();
        return this;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Model id(int id) {
        this.id = id;
        return this;
    }
    
    public void setAutoCloseConnection(boolean autoCloseConnection) {
        this.autoCloseConnection = autoCloseConnection;
    }
    
    public Model auto_close_connection(boolean autoCloseConnection) {
        this.autoCloseConnection = autoCloseConnection;
        return this;
    }
    
    public void isPersisted(boolean isPersisted) {
        this.isPersisted = isPersisted;
    }
    
    public Model set(String field, Object value) {
        if (field != null && !field.trim().isEmpty() ) {
            try {
                Field f = this.getClass().getDeclaredField(field);
                f.setAccessible(true);
                f.set(this, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }
    
    /**
     * Método que insere o modelo invocador na tabela apropriada no banco de dados.
     *  
     * @author - Thiago Alexandre Martins Monteiro  
     * @param - nenhum 
     * @return - nenhum 
     * @throws - java.lang.Exception
     */
    
    public void insert() {
        if (this.connection == null) {      
            this.connection = ConnectionFactory.getConnection();    
        }
            
        try {
            String sql = "INSERT INTO";
            String fields = "";
            String values = "";
            String manyToManySQLFormatter = "INSERT INTO %s_%s (%s_id, %s_id) VALUES (%d,";
            List<String> manyToManySQLs =  new ArrayList<String>();
            String tableName = String.format("%ss", this.getClass().getSimpleName().toLowerCase() );
            Table tableAnnotation = (Table) this.getClass().getAnnotation(Table.class);
            ManyToManyField manyToManyAnnotation = null;
            Manager associatedModelManager = null;
            
            if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty() ) {
                tableName = tableAnnotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase();
            } else if (Model.tableName != null && !Model.tableName.trim().equals("") ) {
                tableName = Model.tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase();
            }
            
            for (Field field : this.getClass().getDeclaredFields() ) {
                field.setAccessible(true);
                
                if (field.getName().equals("serialVersionUID") )
                    continue;
                
                if (field.getName().equals("objects") )
                    continue;
                
                ForeignKeyField foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);
                
                if (field.getType().getSuperclass() != null && field.getType().getSuperclass()
                    .getSimpleName().equals("Model") ) {   
                    if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references().trim().isEmpty() ) {
                        fields += String.format(
                            "%s_id, ", 
                            field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() 
                        );                       
                    }
                } else if (field.getType().getName().equals("java.util.List") 
                    || field.getType().getName().equals("jedi.db.models.QuerySet") ) {
                    //  Não cria o field para esse.
                } else {
                    fields += String.format("%s, ", field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                        .toLowerCase() );
                }
                
                // Tratando values.             
                if (field.getType().toString().endsWith("String") ) {
                    if (field.get(this) != null) {
                        //  Substituindo ' por \' para evitar erro de sintaxe no SQL.                       
                        values += String.format("'%s', ", ( (String) field.get(this) ).replaceAll("'", "\\\\'") );
                    } else {
                        CharField charFieldAnnotation = field.getAnnotation(CharField.class);

                        if (charFieldAnnotation != null) {
                            if (charFieldAnnotation.default_value().trim().equals("\\0") ) {
                                fields = fields.replace(
                                    String.format(
                                        "%s, ", 
                                        field
                                            .getName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase() 
                                    ), 
                                    ""
                                );
                            } else {                                    
                                values += String.format("'%s', ", charFieldAnnotation.default_value()
                                    .replaceAll("'", "\\\\'") );
                            }
                        } else {
                            values += String.format("'', ", field.get(this) );
                        }
                    }
                    
                } else if (field.getType().toString().endsWith("Date") || field.getType().toString()
                    .endsWith("PyDate") ) {
                    Date date = (Date) field.get(this);
                                        
                    if (date != null) {
                        values += String.format(
                            "'%d-%02d-%02d %02d:%02d:%02d', ", 
                            date.getYear() + 1900, 
                            date.getMonth() + 1, 
                            date.getDate(),
                            date.getHours(), 
                            date.getMinutes(), 
                            date.getSeconds() 
                        );
                    } else {
                        DateField dateFieldAnnotation = field.getAnnotation(DateField.class);
                        
                        if (dateFieldAnnotation != null) {
                            if (dateFieldAnnotation.default_value().trim().equals("") ) {
                                if (dateFieldAnnotation.auto_now() ) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    values += String.format("'%s', ", sdf.format(new Date() ) );
                                } else {                                    
                                    fields = fields.replace(String.format("%s, ", field.getName().
                                        replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ), "");
                                }
                            } else {                                    
                                values += String.format(dateFieldAnnotation.default_value()
                                    .trim().equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", dateFieldAnnotation
                                        .default_value().trim() );
                            }
                        } else {
                            values += String.format("'', ", field.get(this) );
                        }                           
                    }
                } else {
                    if (foreignKeyAnnotation != null) {
                        // 1 - Obtendo a referencia para o campo do modelo atual.
                        // 2 - Recuperando o valor do id.
                        // OBS: Tratar o retorno de toString() do modelo e muito perigoso uma vez que
                        // o usuario pode ter a possibilidade de mudar a representacao do objeto e o codigo
                        // que faz o tratamento do retorno e altamente dependente do formato do retorno.
                        Object id = ( (Model) field.get(this) ).id;
                        
                        if (Integer.parseInt(id.toString() ) == 0) {
                            ( (Model) field.get(this) ).save();
                            id = ( (Model) field.get(this ) ).id;
                        }
                        // String id = "";
                        // Lancar excessao de campo nao existente informando qual o campo.
                        // Se o atributo pais nao for configurado em Uf por exemplo, ocorrera NullPointerException.
                        // id = field.get(this).toString();
                        // id = id.substring(id.indexOf("id"), id.indexOf(",") );
                        // id = id.replace(" ", "");
                        // id = id.substring(id.indexOf("=") + 1, id.length() );
                        // id = id.trim();
                        values += String.format("%s, ", id);
                    } else if (field.getType().getName().equals("java.util.List") 
                        || field.getType().getName().equals("jedi.db.models.QuerySet") ) {
                        manyToManyAnnotation = field.getAnnotation(ManyToManyField.class);
                        associatedModelManager = new Manager(
                            Class.forName(
                                String.format(
                                    "app.models.%s", 
                                    manyToManyAnnotation.model()
                                ) 
                            ) 
                        );
                        
                        if (field.getType().getName().equals("java.util.List") ) {
                            for (Object obj : (List) field.get(this) ) {
                                ( (Model) obj).insert();
                                manyToManySQLs.add(
                                    String.format(
                                        manyToManySQLFormatter,
                                        tableName,
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
                                        this
                                            .getClass()
                                            .getSimpleName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase(),
                                        ( (Model) obj).id()
                                    )
                                );                                  
                            }                               
                        }
                        
                        if (field.getType().getName().equals("jedi.db.models.QuerySet") ) {
                            for (Object obj : (QuerySet) field.get(this) ) {
                                ( (Model) obj).insert();
                                manyToManySQLs.add(
                                    String.format(
                                        manyToManySQLFormatter,
                                        tableName,
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
                                        this
                                            .getClass()
                                            .getSimpleName()
                                            .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                                            .toLowerCase(),
                                        ( (Model) obj).id()
                                    )
                                );  
                            }   
                        }   
                    } else {
                        values += String.format("%s, ", field.get(this) );
                    }
                }
            }
            fields = fields.substring(0, fields.lastIndexOf(',') );
            values = values.substring(0, values.lastIndexOf(',') );
            sql = String.format("%s %s (%s) VALUES (%s)", sql, tableName, fields, values);
            // Se o modo debug ou log estiver habilitado mostrar o SQL gerado na saída padrão.
            // System.out.println(sql);
            // this.connection.prepareStatement(sql).execute();
            this.connection.createStatement().executeUpdate(sql);
            
            if (!this.connection.getAutoCommit() ) {
                this.connection.commit();
            }
            // Verificar se o usuario optou por habilitar o recurso de auto-incremento.
            Manager manager = new Manager(this.getClass() );
            // Obtendo o id do último registro inserido e atribuindo ao id do objeto atual.
            this.id = manager.getLastInsertedID();
            
            if (manyToManyAnnotation != null) {
                for (String associatedModelSQL : manyToManySQLs) {
                    associatedModelSQL = String.format("%s %d) ", associatedModelSQL, this.id() );
                    // System.out.println(associated_model_sql);
                    associatedModelManager.raw(associatedModelSQL);
                }   
            }
        } catch (Exception e) {
            e.printStackTrace();
            
            try {
                if (!this.connection.getAutoCommit() ) {
                    this.connection.rollback();
                }                   
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            if (this.autoCloseConnection) {
                try {
                    this.connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Método que atualiza o modelo invocador na tabela apropriada no banco de dados.
     *  
     * @author - Thiago Alexandre Martins Monteiro  
     * @param - nenhum 
     * @return - nenhum 
     * @throws - java.lang.Exception
     */
    public void update(String... args) {
        if (this.connection == null) {
            this.connection = ConnectionFactory.getConnection();    
        }
            
        try {
            String sql = "UPDATE";
            String tableName = String.format("%ss", this.getClass().getSimpleName()
                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
            Table tableAnnotation = (Table) this.getClass().getAnnotation(Table.class);
            
            if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty() ) {
                tableName = tableAnnotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase();
            } else if (Model.tableName != null && !Model.tableName.trim().equals("") ) {
                tableName = Model.tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                    .toLowerCase();
            }
            sql = String.format("%s %s SET", sql, tableName);
            String fieldsAndValues = "";
            
            if (args.length == 0) {
                for (Field field : this.getClass().getDeclaredFields() ) {
                    field.setAccessible(true);
                    
                    if (field.getName().equals("serialVersionUID") )
                        continue;
                    
                    if (field.getName().equals("objects") )
                        continue;
                    
                    ForeignKeyField foreignKeyAnnotation = null;
                    
                    if (field.getType().getSuperclass() != null && field.getType().getSuperclass()
                        .getSimpleName().equals("Model") ) {  
                        foreignKeyAnnotation = field.getAnnotation(ForeignKeyField.class);
                        
                        if (foreignKeyAnnotation != null && !foreignKeyAnnotation.references().isEmpty() ) {
                            fieldsAndValues += String.format("%s_id = ", field.getType().getSimpleName()
                                .replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
                        }
                    } else {
                        fieldsAndValues += String.format("%s = ", field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2")
                            .toLowerCase() );
                    }
                                            
                    if (field.getType().toString().endsWith("String") ) {
                        if (field.get(this) != null) {
                            fieldsAndValues += String.format("'%s', ", ( (String) field.get(this) )
                                .replaceAll("'", "\\\\'") );
                        } else {
                            fieldsAndValues += "'', ";
                        }
                    } else if (field.getType().toString().endsWith("Date") || field.getType().toString()
                        .endsWith("PyDate") ) {
                        Date date = (Date) field.get(this);
                        fieldsAndValues += String.format(
                            "'%d-%02d-%02d %02d:%02d:%02d', ", 
                            date.getYear() + 1900, 
                            date.getMonth() + 1, 
                            date.getDate(),
                            date.getHours(), 
                            date.getMinutes(), 
                            date.getSeconds() 
                        );
                    } else {
                        if (foreignKeyAnnotation != null) {
                            Object id = ( (Model) field.get(this) ).id;
                            fieldsAndValues += String.format("%s, ", id);
                        } else {
                            fieldsAndValues += String.format("%s, ", field.get(this) );
                        }
                    }
                }
                fieldsAndValues = fieldsAndValues.substring(0, fieldsAndValues.lastIndexOf(',') );
            } else {
                if (args.length > 0) {
                    Field field = null;                 
                    String fieldName = "";
                    String fieldValue = "";
                    
                    for (int i = 0; i < args.length; i++) {
                        fieldName = args[i].split("=")[0];
                        
                        if (fieldName.endsWith("_id") ) {
                            fieldName = fieldName.replace("_id", "");
                        }
                        fieldValue = args[i].split("=")[1];
                        
                        if (fieldValue.startsWith("'") && fieldValue.endsWith("'") ) {
                            fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
                        }
                        field = this.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        
                        if (field.getType() == String.class) {
                            field.set(this, fieldValue);
                        } else if (field.getType() == Integer.class) {
                            field.set(this, Integer.parseInt(fieldValue) );
                        } else if (field.getType() == Float.class) {
                            field.set(this, Float.parseFloat(fieldValue) );
                        } else if (field.getType() == Double.class) {
                            field.set(this, Double.parseDouble(fieldValue) );
                        } else if (field.getType() == Date.class) {
                            field.set(this, Date.parse(fieldValue) );
                        } else if (field.getType() == Boolean.class) {
                            field.set(this, Boolean.parseBoolean(fieldValue) );
                        } else if (field.getAnnotation(ForeignKeyField.class) != null) {
                            if (field.get(this) != null) {
                                ( (Model) field.get(this) ).setId(Integer.parseInt(fieldValue) );
                            }
                        } else {
                        }
                        fieldsAndValues += args[i] + ", ";
                    }
                    fieldsAndValues = fieldsAndValues.substring(0, fieldsAndValues.lastIndexOf(",") );
                }
            }
            sql = String.format("%s %s WHERE id = %s", sql, fieldsAndValues, this.getClass().getSuperclass()
                .getDeclaredField("id").get(this) );
            // System.out.println(sql);
            this.connection.prepareStatement(sql).execute();
            
            if (!this.connection.getAutoCommit() ) {
                this.connection.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!this.connection.getAutoCommit() ) {
                    this.connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();                   
            }
        } finally {
            if (autoCloseConnection) {
                try {
                    this.connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void save() {
        try {
            if (!this.isPersisted) {               
                this.insert();
            } else {                
                this.update();
            }
            this.isPersisted(true);
        } catch (Exception e) {         
            e.printStackTrace();
        }
    }
    
    public <T extends Model> T save(Class<T> modelClass) {     
        this.save();        
        return this.as(modelClass);
    }
    
    public void delete() {
        if (this.connection == null) {      
            this.connection = ConnectionFactory.getConnection();    
        }
            
        try {
            String sql = "DELETE FROM";
            String tableName = String.format("%ss", this.getClass().getSimpleName().toLowerCase() );
            Table tableAnnotation = (Table) this.getClass().getAnnotation(Table.class);
            
            if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty() ) {
                tableName = tableAnnotation.name().trim().toLowerCase();
            } else if (Model.tableName != null && !Model.tableName.trim().equals("") ) {
                tableName = Model.tableName;
            }
            sql = String.format("%s %s WHERE", sql, tableName);
            sql = String.format("%s id = %s", sql, this.getClass().getSuperclass().getDeclaredField("id").get(this) );
            this.connection.prepareStatement(sql).execute();
            
            if (!this.connection.getAutoCommit() ) {
                this.connection.commit();
            }
            this.isPersisted(false);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!this.connection.getAutoCommit() ) {
                    this.connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            if (this.autoCloseConnection) {
                try {
                    this.connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int compareTo(Model model) {
        if (this.id < model.id) {
            return -1;
        }
        
        if (this.id > model.id) {
            return 1;
        }
        return 0;
    }
    
    public String toString() {
        String s = "";

        try {
            s = String.format(
                "{%s: id = %s, ", 
                this
                    .getClass()
                    .getSimpleName(), 
                this.
                    getClass()
                    .getSuperclass()
                    .getDeclaredField("id")
                    .get(this) 
            );
            Field[] fields = this.getClass().getDeclaredFields();
            
            for (Field f : fields) {
                f.setAccessible(true);
                
                if (f.getName().equals("serialVersionUID") )
                    continue;
                
                if (f.getName().equalsIgnoreCase("objects") )
                    continue;
                
                s += String.format("%s = %s, ", f.getName(), f.get(this) );
            }
            s = s.substring(0, s.lastIndexOf(",") );
            s += "}";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }
    
    @Override
    public boolean equals(Object o) {
        boolean r = false;
        
        if (this.id == ( (Model)o ).id) {
            r = true;
        }
        return r;
    }
    
    /**
     * @param i int
     * @return String
     */
    public String toJSON(int i) {
        // i - identation level       
        String json = "";        
        String identationToClass = "";        
        String identationToFields = "    ";       
        String identationToListItems = "        ";
        
        for (int j = 0; j < i; j++) {       
            identationToClass += "    ";          
            identationToFields += "    ";         
            identationToListItems += "    ";     
        }
        
        try {
            json = String.format(
                "%s%s {\n%sid: %s,",
                identationToClass,
                this
                    .getClass()
                    .getSimpleName(),
                identationToFields,
                this
                    .getClass()
                    .getSuperclass()
                    .getDeclaredField("id")
                    .get(this)
            );
            Field[] fields = this.getClass().getDeclaredFields();
            
            for (Field f : fields) {
                f.setAccessible(true);
                
                if (f.getName().equals("serialVersionUID") )
                    continue;
                
                if (f.getName().equalsIgnoreCase("objects") )
                    continue;
                
                if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName()
                    .equals("jedi.db.models.Model") ) {
                    if (f.get(this) != null) {
                        json += String.format("\n%s,", ( (Model) f.get(this) ).toJSON(i + 1) );
                    }
                } else if (f.getType().getName().equals("java.util.List") || f.getType().getName()
                    .equals("jedi.db.models.QuerySet") ) {
                    String strItems = "";
                    
                    for (Object item : (List) f.get(this) ) {
                        strItems += String.format("\n%s,", ( (Model) item ).toJSON( (i + 2) ) );
                    }
                    
                    if (strItems.lastIndexOf(",") >= 0) {
                        strItems = strItems.substring(0, strItems.lastIndexOf(",") );
                    }
                    
                    json += String.format("\n%s%s: [%s\n%s],", identationToFields, f.getName(), 
                        strItems, identationToFields);
                } else {
                    json += String.format("\n%s%s: %s,", identationToFields, f.getName(), f.get(this) );
                }
            }
            
            if (json.lastIndexOf(",") >= 0) {
                json = json.substring(0, json.lastIndexOf(",") );
            }
            json += String.format("\n%s}", identationToClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
    
    /**
     * @return String
     */
    public String toJSON() {
        return toJSON(0);
    }
    
    public String toXML(int i) {
        // i - identation level
        String xmlElement = this.getClass().getSimpleName().toLowerCase();
        StringBuilder xml = new StringBuilder();
        StringBuilder xmlElementAttributes = new StringBuilder();
        StringBuilder xmlChildElements = new StringBuilder();
        xmlElementAttributes.append("");
        String xmlElementString = "";     
        String identationToElement = "";      
        // String identationToAttributes = "    ";    
        String identationToChildElements = "    ";
        
        for (int j = 0; j < i; j++) {       
            identationToElement += "    ";    
            // identationToAttributes += "    ";          
            identationToChildElements += "    ";     
        }
        
        try {
            xmlElementAttributes.append(String.format("id=\"%d\"", this.getClass().getSuperclass().getDeclaredField("id")
                .getInt(this) ) );
            Field[] fields = this.getClass().getDeclaredFields();
            
            for (Field f : fields) {
                f.setAccessible(true);
                
                if (f.getName().equals("serialVersionUID") )
                    continue;
                
                if (f.getName().equalsIgnoreCase("objects") )
                    continue;
                
                if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName()
                    .equals("jedi.db.models.Model") ) {
                    xmlChildElements.append(String.format("\n%s\n", ( (Model) f.get(this) ).toXML(i + 1) ) );
                } else if (f.getType().getName().equals("java.util.List") || f.getType().getName()
                    .equals("jedi.db.models.QuerySet") ) {
                    String xmlChildOpenTag = "";
                    String xmlChildCloseTag = "";
                    Table tableAnnotation = null;
                    
                    if ( !( (List) f.get(this) ).isEmpty() ) {
                        tableAnnotation = ( (List) f.get(this) ).get(0).getClass().getAnnotation(Table.class);
                        
                        if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty() ) {
                            xmlChildOpenTag = String.format("\n%s<%s>", identationToChildElements, tableAnnotation.name()
                                .trim().toLowerCase() );
                            xmlChildCloseTag = String.format("\n%s</%s>", identationToChildElements, tableAnnotation.name().trim()
                                .toLowerCase() );
                        } else {
                            xmlChildOpenTag = String.format("\n%s<%ss>", identationToChildElements, ( (List) f.get(this) ).get(0)
                                .getClass().getSimpleName().toLowerCase() );
                            xmlChildCloseTag = String.format("\n%s</%ss>", identationToChildElements, ( (List) f.get(this) ).get(0)
                                .getClass().getSimpleName().toLowerCase() );
                        }
                        xmlChildElements.append(xmlChildOpenTag);
                        
                        for (Object item : (List) f.get(this) ) {                       
                            xmlChildElements.append(String.format("\n%s", ( (Model) item ).toXML(i + 2) ) );                         
                        }
                        xmlChildElements.append(xmlChildCloseTag);
                    }
                } else {
                    xmlElementAttributes.append(String.format(" %s=\"%s\"", f.getName(), f.get(this) ) );
                }
            }
                        
            if (xmlChildElements.toString().isEmpty() ) {
                xml.append(String.format("%s<%s %s />", identationToElement, xmlElement, xmlElementAttributes.toString() ) );
            } else {
                xml.append(
                    String.format(
                        "%s<%s %s>%s%s</%s>", 
                        identationToElement, 
                        xmlElement, 
                        xmlElementAttributes.toString(), 
                        xmlChildElements, 
                        identationToElement, 
                        xmlElement
                    ) 
                );              
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xml.toString();
    }
    
    public String toXML() {        
        StringBuilder xmlString = new StringBuilder();     
        xmlString.append(toXML(0) );
        return xmlString.toString();
    }
    
    
    public String toExtenseXML(int i) {
        // A IMPLEMENTAR: Fazer o tratamento de caracteres especiais como < ou > no conteudo dos atributos
        // ao produzir o xml de retorno.
        // i - nivel de identacao
        String xmlElement = this.getClass().getSimpleName().toLowerCase();
        StringBuilder xml = new StringBuilder();     
        StringBuilder xmlElementAttributes = new StringBuilder();     
        StringBuilder xmlChildElements = new StringBuilder();     
        xmlElementAttributes.append("");
        String xmlElementString = "";     
        String identationToElement = "";      
        String identationToAttributes = "    ";       
        String identationToChildElements = "    ";
        
        for (int j = 0; j < i; j++) {       
            identationToElement += "    ";            
            identationToAttributes += "    ";         
            identationToChildElements += "    ";     
        }
        
        try {
            xmlElementAttributes.append(
                String.format(
                    "\n%s<id>%d</id>\n", 
                    identationToAttributes, 
                    this
                        .getClass()
                        .getSuperclass()
                        .getDeclaredField("id")
                        .getInt(this) 
                ) 
            );
            Field[] fields = this.getClass().getDeclaredFields();
            
            for (Field f : fields) {
                f.setAccessible(true);
                
                if (f.getName().equals("serialVersionUID") )
                    continue;
                
                if (f.getName().equalsIgnoreCase("objects") )
                    continue;
                
                if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName()
                    .equals("jedi.db.models.Model") ) {
                    xmlChildElements.append(String.format("%s\n", ( (Model) f.get(this) ).toXML(i + 1) ) );
                } else if (f.getType().getName().equals("java.util.List") || f.getType().getName()
                    .equals("jedi.db.models.QuerySet") ) {
                    String xmlChildOpenTag = "";
                    String xmlChildCloseTag = "";
                    Table tableAnnotation = null;
                    
                    if ( !( (List) f.get(this) ).isEmpty() ) {
                        tableAnnotation = ( (List) f.get(this) ).get(0).getClass().getAnnotation(Table.class);
                        
                        if (tableAnnotation != null && !tableAnnotation.name().trim().isEmpty() ) {
                            xmlChildOpenTag = String.format("%s<%s>", identationToChildElements, tableAnnotation.name()
                                .trim().toLowerCase() );
                            xmlChildCloseTag = String.format("\n%s</%s>\n", identationToChildElements, tableAnnotation.name()
                                .trim().toLowerCase() );
                        } else {
                            xmlChildOpenTag = String.format("%s<%ss>", identationToChildElements, ( (List) f.get(this) ).get(0)
                                .getClass().getSimpleName().toLowerCase() );
                            xmlChildCloseTag = String.format("\n%s</%ss>\n", identationToChildElements, ( (List) f.get(this) ).get(0)
                                .getClass().getSimpleName().toLowerCase() );
                        }
                        xmlChildElements.append(xmlChildOpenTag);
                        
                        for (Object item : (List) f.get(this) ) {
                            xmlChildElements.append(String.format("\n%s", ( (Model) item ).toXML(i + 2) ) );
                        }
                        xmlChildElements.append(xmlChildCloseTag);
                    }
                } else {
                    xmlElementAttributes.append(String.format("%s<%s>%s</%s>\n", identationToAttributes, 
                        f.getName(), f.get(this), f.getName() ) );
                }
            }
                        
            if (xmlChildElements.toString().isEmpty() ) {
                xml.append(
                    String.format(
                        "%s<%s>%s%s</%s>", 
                        identationToElement, 
                        xmlElement, 
                        xmlElementAttributes.toString(),
                        identationToElement,
                        xmlElement
                    ) 
                );
            } else {
                xml.append(
                    String.format(
                        "%s<%s>%s%s%s</%s>", 
                        identationToElement, 
                        xmlElement, 
                        xmlElementAttributes.toString(), 
                        xmlChildElements,
                        identationToElement, 
                        xmlElement
                    ) 
                );  
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xml.toString();
    }
    
    public String toExtenseXML() {
        StringBuilder xml = new StringBuilder();     
        xml.append(toExtenseXML(0) );
        return xml.toString();
    }
    
    public <T extends Model> T as(Class<T> c) {
        return (T) this;
    }
}