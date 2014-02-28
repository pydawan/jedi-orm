/***********************************************************************************************
 * @(#)State.java
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
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class State extends Model {
    // Attributes
    private static final long serialVersionUID = -6945954961476788586L;

    @CharField(max_length=50, unique=true)
    private String name;

    @CharField(max_length=2, unique=true)
    private String acronym;

    @CharField(max_length=50, unique=true, required=false)
    private String capital;

    @ForeignKeyField(model="Country", 
        constraint_name="fk_states_countries", 
        references="countries", 
        on_delete=Models.CASCADE, 
        on_update=Models.CASCADE)
    private Country country;

    @CharField(max_length=1, default_value="A", comment="Status (A - Active / I - Inactive)")
    private String status;

    public static Manager objects = new Manager(State.class);

    // Constructors
    public State() {}
    
    public State(String name, String acronym, Country country) {
        this.name = name;
        this.acronym = acronym;
        this.country = country;
    }

    public State(String name, String acronym, String capital, Country country) {
        this(name, acronym, country);
        this.capital = capital;
    }
    
    public State(String name, String acronym, String capital, Country country, String status) {
        this(name, acronym, capital, country);
        this.status = status;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getAcronym() {
        return acronym;
    }
    
    public String getCapital() {
        return capital;
    }
    
    public Country getCountry() {
        return country;
    }
    
    public String getStatus() {
        return status;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    
    public void setCapital(String capital) {
        this.capital = capital;
    }
    
    public void setCountry(Country country) {
        this.country = country;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }  

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet getPublisherSet() {
        return Publisher.objects.getSet(State.class, this.id);
    }
}