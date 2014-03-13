/***********************************************************************************************
 * @(#)Product.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/30
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

public class Product extends Model {
    // Attributes
    private static final long serialVersionUID = -5355826250714874543L;

    @CharField(max_length=30)
    private String name;

    @DecimalField(scale=7, precision=2, unique=true)
    private BigDecimal price;

    public static Manager objects = new Manager(Product.class);

    // Constructors
    public Product() {}

    public Product(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    // Getters
    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = new BigDecimal(price);
    }
}