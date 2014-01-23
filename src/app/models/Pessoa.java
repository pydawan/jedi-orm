/***********************************************************************************************
 * @(#)Pessoa.java
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

import java.util.Date;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
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

    @CharField(max_length=14, required=false)
    private String cpf;

    @CharField(max_length=10, required=false)
    private String rg;

    @CharField(max_length=1)
    private String tipoPessoa;

    @DateField(auto_now=true, auto_now_add=true, comment="Data do aniversário")
    private Date dataNascimento;

    public static Manager objects = new Manager(Pessoa.class);

    // Constructors
    public Pessoa() {}

    public Pessoa(int id, String nome, int idade, String tipoPessoa) {
        this.id = id;
        this.nome = nome;
        this.idade = idade;
        this.tipoPessoa = tipoPessoa;
    }

    public Pessoa(String nome, int idade, String tipoPessoa) {
        this.nome = nome;
        this.idade = idade;
        this.tipoPessoa = tipoPessoa;
    }

    public Pessoa(int id, String nome, int idade, String cpf, String rg, String tipoPessoa, Date dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.idade = idade;
        this.cpf = cpf;
        this.rg = rg;
        this.tipoPessoa = tipoPessoa;
        this.dataNascimento = dataNascimento;
    }

    public Pessoa(String nome, int idade, String cpf, String rg, String tipoPessoa, Date dataNascimento) {
        this.nome = nome;
        this.idade = idade;
        this.cpf = cpf;
        this.rg = rg;
        this.tipoPessoa = tipoPessoa;
        this.dataNascimento = dataNascimento;
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public int getIdade() {
        return idade;
    }

    public String getCpf() {
        return cpf;
    }

    public String getRg() {
        return rg;
    }

    public String getTipoPessoa() {
        return tipoPessoa;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setIdade(int idade) {
        this.idade = idade;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setRg(String rg) {
        this.rg = rg;
    }

    public void setTipoPessoa(String tipoPessoa) {
        this.tipoPessoa = tipoPessoa;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

}