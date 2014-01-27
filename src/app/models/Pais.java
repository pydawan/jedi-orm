/***********************************************************************************************
 * @(#)Pais.java
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

@Table(name="paises", engine="InnoDB", charset="utf8", comment="Tabela de paises.")
public class Pais extends Model {
    // Attributes
    private static final long serialVersionUID = 8197600275231031642L;

    @CharField(max_length=50, unique=true, comment="Nome do país.")
    private String nome;

    @CharField(max_length=2, unique=true, comment="Sigla do país.")
    private String sigla;

    @CharField(max_length=50, required=false, unique=true, comment="Capital do país.")
    private String capital;

    @CharField(max_length=50, required=false, comment="Continente em que o país está localizado.")
    private String continente;

    @CharField(max_length=1, required=false, default_value="A", comment="Situação do País (A - Ativo / I - Inativo)")
    private String situacao;

    public static Manager objects = new Manager(Pais.class);

    // Constructors
    public Pais() {}

    public Pais(int id) {
        this.id = id;
    }

    public Pais(String nome) {
        this.nome = nome;
    }

    public Pais(int id, String nome, String sigla) {
        this.id = id;
        this.nome = nome;
        this.sigla = sigla;
    }

    public Pais(String nome, String sigla) {
        this.nome = nome;
        this.sigla = sigla;
    }

    public Pais(String nome, String sigla, String capital, String continente) {
        this.nome = nome;
        this.sigla = sigla;
        this.capital = capital;
        this.continente = continente;
    }

    public Pais(String nome, String sigla, String capital, String continente, String situacao) {
        this.nome = nome;
        this.sigla = sigla;
        this.capital = capital;
        this.continente = continente;
        this.situacao = situacao;
    }

    public Pais(int id, String nome, String sigla, String capital, String continente, String situacao) {
        this.id = id;
        this.nome = nome;
        this.sigla = sigla;
        this.capital = capital;
        this.continente = continente;
        this.situacao = situacao;
    }

    // Getters
    public String getNome() {
        return this.nome;
    }

    public String getCapital() {
        return capital;
    }

    public String getContinente() {
        return continente;
    }

    public String getSigla() {
        return sigla;
    }

    public String getSituacao() {
        return this.situacao;
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

    public void setContinente(String continente) {
        this.continente = continente;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet uf_set() {
        return Uf.objects.getSet(Pais.class, this.id);
    }
}