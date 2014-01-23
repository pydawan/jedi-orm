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

import jedi.db.Models;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Editora extends Model {
    // Attributes
    private static final long serialVersionUID = -4632483608458698726L;

    @CharField(max_length=30)
    private String nome;

    @CharField(max_length=50, required=false)
    private String endereco;

    @ForeignKeyField(model="Uf", constraint_name="fk_editoras_ufs", references="ufs", on_delete=Models.CASCADE)
    private Uf uf;

    public static Manager objects = new Manager(Editora.class);

    // Constructors
    public Editora() {}

    public Editora(int id) {
        this.id = id;
    }

    public Editora(String nome) {
        this.nome = nome;
    }

    public Editora(String nome, Model uf) {
        this.nome = nome;
        this.uf = (Uf) uf;
    }

    public Editora(String nome, String endereco, Model uf) {
        this.nome = nome;
        this.endereco = endereco;
        this.uf = (Uf) uf;
    }

    public Editora(int id, String nome, String endereco, Model uf) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.uf = (Uf) uf;
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public Uf getUf() {
        return uf;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public void setUf(Model uf) {
        this.uf = (Uf) uf;
    }

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet livro_set() {
        return Livro.objects.get_set(Editora.class, this.id);
    }
}