/***********************************************************************************************
 * @(#)Publisher.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/28
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

package app.models;

import jedi.db.Models;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;

public class Publisher extends Model {
    // Attributes
    private static final long serialVersionUID = -4632483608458698726L;

    @CharField(max_length=30)
    private String name;

    @CharField(max_length=50, required=false)
    private String address;

    @ForeignKeyField(model="State", constraint_name="fk_publishers_states", references="states", on_delete=Models.CASCADE)
    private State state;

    public static Manager objects = new Manager(Publisher.class);

    // Constructors
    public Publisher() {}

    public Publisher(String name, State state) {
        this.name = name;
        this.state = state;
    }
    
    public Publisher(String name, String address, State state) {
        this(name, state);
        this.address = address;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public State getState() {
        return state;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    public jedi.db.models.query.QuerySet<Book> getBookSet() {
        return Book.objects.getSet(Publisher.class, this.id);
    }
}