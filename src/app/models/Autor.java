/***********************************************************************************************
 * @(#)Autor.java
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

@Table(name="autores")
public class Autor extends Model {
    // Attributes
    private static final long serialVersionUID = -8520333625577424268L;

    @CharField(max_length=30)
    private String nome;

    @CharField(max_length=30, required=false)
    private String sobrenome;

    @CharField(max_length=30, required=true, unique=true)
    private String email;

    public static Manager objects = new Manager(Autor.class);

    // Constructors
    public Autor() {}

    public Autor(int id) {
        this.id = id;
    }

    public Autor(String nome) {
        this.nome = nome;
    }

    public Autor(String nome, String sobrenome, String email) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
    }

    public Autor(int id, String nome, String sobrenome, String email) {
        this.id = id;
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }
    
    public String getEmail() {
        return email;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet livro_set() {
        return Livro.objects.get_set(Autor.class, this.id);
    }
}
