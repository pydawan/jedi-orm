/***********************************************************************************************
 * @(#)Book.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/20
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
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;
import jedi.db.models.query.QuerySet;

@Table(name="books", engine="InnoDB", charset="utf8", comment="Table of books")
public class Book extends Model {
    // Attributes
    private static final long serialVersionUID = 9076408430303339094L;

    @CharField(max_length=30, required=true, unique=true, comment="This field stores the book\\'s title.")
    private String title;

    @ManyToManyField
    private QuerySet<Author> authors;

    @ForeignKeyField
    private Publisher publisher;

    @CharField(max_length=15, required=true)
    private String publicationDate;

    public static Manager objects = new Manager(Book.class);

    // Constructors
    public Book() {
    	authors = new QuerySet<Author>();
    	publisher = new Publisher();
    }

    public Book(String title, QuerySet<Author> authors, String publicationDate) {
    	this();
        this.title = title;
        this.authors = authors;
        this.publicationDate = publicationDate;
    }
    
    // Getters
    public String getTitle() {
        return title;
    }
    
    public QuerySet<Author> getAuthors() {
        return authors;
    }
    
    public Publisher getPublisher() {
        return publisher;
    }
    
    public String getPublicationDate() {
        return publicationDate;
    }
    
    // Setters
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setAuthors(QuerySet<Author> authors) {
        this.authors = authors;
    }
    
    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
    
    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }
}