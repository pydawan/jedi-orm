/***********************************************************************************************
 * @(#)Author.java
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

package app.models;

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

@Table(name="authors")
public class Author extends Model {
    
    // Attributes
    private static final long serialVersionUID = -8520333625577424268L;

    @CharField(max_length=30)
    private String firstName;

    @CharField(max_length=30, required=false)
    private String lastName;

    @CharField(max_length=30, required=true, unique=true)
    private String email;

    public static Manager objects = new Manager(Author.class);

    // Constructors
    public Author() {}

    public Author(int id) {
        this.id = id;
    }

    public Author(String firstName) {
        this.firstName = firstName;
    }

    public Author(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public Author(int id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    // Setters
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet livro_set() {
        return Livro.objects.get_set(Author.class, this.id);
    }
}
