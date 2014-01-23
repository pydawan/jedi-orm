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

import java.util.List;

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;
import jedi.db.models.QuerySet;

@SuppressWarnings({"rawtypes", "unchecked"})
@Table(name="livros", engine="InnoDB", charset="utf8", comment="Tabela que armazena os livros do sistema de biblioteca.")
public class Livro extends Model {
    // Attributes
    private static final long serialVersionUID = 9076408430303339094L;

    @CharField(max_length=30, required=true, unique=true, comment="Esse campo armazena o nome do livro.")
    private String titulo;

    @ManyToManyField(model="Autor", references="autores")
    private QuerySet<Autor> autores;

    @ForeignKeyField(model="Editora", constraint_name="fk_livros_editoras", references="editoras")
    private Editora editora;

    @CharField(max_length=15, required=true, comment="Data de publicação do livro.")
    private String dataPublicacao;

    public static Manager objects = new Manager(Livro.class);

    // Constructors
    public Livro() {}

    public Livro(int id) {
        this.id = id;
    }

    public Livro(String titulo) {
        this.titulo = titulo;
    }

    public Livro(String titulo, List<Autor> autores, Model editora, String dataPublicacao) {
        this.titulo = titulo;
        this.autores = new QuerySet<Autor>();
        this.autores.add(autores);
        this.editora = (Editora) editora;
        this.dataPublicacao = dataPublicacao;
    }

    public Livro(int id, String titulo, List<Autor> autores, Model editora, String dataPublicacao) {
        this.id = id;
        this.titulo = titulo;
        this.autores = new QuerySet<Autor>();
        this.autores.add(autores);
        this.editora = (Editora) editora;
        this.dataPublicacao = dataPublicacao;
    }

    // Getters
    public String getTitulo() {
        return titulo;
    }

    public QuerySet getAutores() {
        return autores;
    }

    public Editora getEditora() {
        return editora;
    }

    public String getDataPublicacao() {
        return dataPublicacao;
    }

    // Setters
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setAutores(List<Autor> autores) {
        this.autores = new QuerySet<Autor>();
        this.autores.add(autores);
    }

    // More flexible alternative.
    public void setAutores(QuerySet autores) {
        this.autores = autores;
    }

    public void setEditora(Model editora) {
        this.editora = (Editora) editora;
    }

    public void setDataPublicacao(String dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

}
