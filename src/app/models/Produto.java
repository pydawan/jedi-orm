/***********************************************************************************************
 * @(#)Produto.java
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

import java.math.BigDecimal;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DecimalField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;

public class Produto extends Model {
    // Attributes
    private static final long serialVersionUID = -5355826250714874543L;

    @CharField(max_length=30)
    private String nome;

    @DecimalField(scale=4, precision=2, unique=true)
    private BigDecimal preco;

    public static Manager objects = new Manager(Produto.class);

    // Constructors
    public Produto() {}

    public Produto(int id) {
        this.id = id;
    }

    public Produto(String nome) {
        this.nome = nome;
    }

    public Produto(int id, String nome, BigDecimal preco) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

}