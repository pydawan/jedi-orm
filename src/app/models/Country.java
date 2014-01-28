/***********************************************************************************************
 * @(#)Country.java
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

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

@Table(name="countries", engine="InnoDB", charset="utf8", comment="Table of countries.")
public class Country extends Model {
    // Attributes
    private static final long serialVersionUID = 8197600275231031642L;

    @CharField(max_length=50, unique=true, comment="Country\\'s Name")
    private String name;

    @CharField(max_length=2, unique=true)
    private String acronym;
    
    @CharField(max_length=1, required=false, default_value="A", comment="Country\\'s Status (A - Active / I - Inactive)")
    private String status;

    public static Manager objects = new Manager(Country.class);
    
    // Constructors
    public Country() {}
    
    public Country(String name, String acronym) {
        this.name = name;
        this.acronym = acronym;
    }
    
    public Country(String name, String acronym, String status) {
        this(name, acronym);
        this.status = status;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getAcronym() {
        return acronym;
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

    public void setStatus(String status) {
        this.status = status;
    }

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet stateSet() {
        return State.objects.getSet(Country.class, this.id);
    }
}