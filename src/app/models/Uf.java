/***********************************************************************************************
 * @(#)Uf.java
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

public class Uf extends Model {
    // Attributes
    private static final long serialVersionUID = -6945954961476788586L;

    @CharField(max_length=50, unique=true, comment="Nome da Unidade Federativa.")
    private String nome;

    @CharField(max_length=2, unique=true, comment="Sigla da Unidade Federativa.")
    private String sigla;

    @CharField(max_length=50, unique=true, required=false, comment="Capital da Unidade Federativa.")
    private String capital;

    @CharField(max_length=30, unique=false, required=false, comment="Região em que está localizada a Unidade Federativa.")
    private String regiao;

    @ForeignKeyField(model="Pais", constraint_name="fk_ufs_paises", references="paises", on_delete=Models.CASCADE, 
            on_update=Models.CASCADE, comment="País onde está localizada a Unidade Federativa.")
    private Pais pais;

    @CharField(max_length=1, default_value="A", comment="Situação da Unidade Federativa (A - Ativa / I - Inativa)")
    private String situacao;

    public static Manager objects = new Manager(Uf.class);

    // Constructors
    public Uf() {}

    public Uf(int id) {
        this.id = id;
    }

    public Uf(String nome) {
        this.nome = nome;
    }

    public Uf(String nome, String sigla) {
        this.nome = nome;
        this.sigla = sigla;
    }

    public Uf(String nome, String sigla, Pais pais) {
        this.nome = nome;
        this.sigla = sigla;
        this.pais = pais;
    }

    public Uf(String nome, String sigla, String situacao, Pais pais) {
        this.nome = nome;
        this.sigla = sigla;
        this.situacao = situacao;
        this.pais = pais;
    }

    public Uf(String nome, String sigla, String capital, String regiao, Model pais) {
        this.nome = nome;
        this.sigla = sigla;
        this.capital = capital;
        this.regiao = regiao;
        this.pais = (Pais) pais;
    }

    public Uf(int id, String nome, String sigla, String capital, String regiao, Model pais) {
        this.id = id;
        this.nome = nome;
        this.sigla = sigla;
        this.capital = capital;
        this.regiao = regiao;
        this.pais = (Pais) pais;
    }

    public Uf(String nome, String sigla, String capital, String regiao) {
        this.nome = nome;
        this.sigla = sigla;
        this.capital = capital;
        this.regiao = regiao;
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public String getSigla() {
        return sigla;
    }

    public String getCapital() {
        return capital;
    }

    public String getRegiao() {
        return regiao;
    }

    public Pais getPais() {
        return pais;
    }

    public String getSituacao() {
        return situacao;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public void setPais(Model pais) {
        this.pais = (Pais) pais;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet getEditoraSet() {
        return Editora.objects.getSet(Uf.class, this.id);
    }
}