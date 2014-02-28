/***********************************************************************************************
 * @(#)Pessoa.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/16
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

import java.util.Date;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.EmailField;
import jedi.db.annotations.fields.IntegerField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Pessoa extends Model {
    // Attributes
    private static final long serialVersionUID = 887615433470716542L;

    @CharField(max_length=50, unique=true, comment="Esse campo armazena o nome da pessoa.")
    private String nome;

    @IntegerField
    private int idade;

    @DateField(auto_now=true, auto_now_add=true, comment="Data do aniversário")
    private Date dataNascimento;
    
    @EmailField
    private String email;

    public static Manager objects = new Manager(Pessoa.class);

    // Constructors
    public Pessoa() {}

    public Pessoa(int id, String nome, int idade, String tipoPessoa) {
        this.id = id;
        this.nome = nome;
        this.idade = idade;
    }

    public Pessoa(String nome, int idade, String tipoPessoa) {
        this.nome = nome;
        this.idade = idade;
    }

    public Pessoa(int id, String nome, int idade, String cpf, String rg, String tipoPessoa, Date dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.idade = idade;
        this.dataNascimento = dataNascimento;
    }

    public Pessoa(String nome, int idade, String cpf, String rg, String tipoPessoa, Date dataNascimento) {
        this.nome = nome;
        this.idade = idade;
        this.dataNascimento = dataNascimento;
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public int getIdade() {
        return idade;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }
    
    public String getEmail() {
    	return email;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setIdade(int idade) {
        this.idade = idade;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }
    
    public void setEmail(String email) {
    	this.email = email;
    }

    public PessoaFisica getPessoaFisica() {
        return PessoaFisica.objects.get("pessoa_id", this.id);
    }
}