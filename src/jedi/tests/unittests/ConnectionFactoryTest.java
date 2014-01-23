/***********************************************************************************************
 * @(#)ConnectionFactoryTest.java
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

package jedi.tests.unittests;

import java.sql.Connection;

import org.junit.Assert;
import org.junit.Test;

import jedi.db.ConnectionFactory;

public class ConnectionFactoryTest {

    @Test
    public void testConnectH2() {
        Connection conexao = ConnectionFactory.connect("engine=h2");
        Assert.assertNotNull(conexao);
    }

    @Test
    public void testConnectMySQL() {
        Connection conexao = ConnectionFactory.connect("engine=mysql", "user=jedi", "password=jedi", "database=jedi");
        Assert.assertNotNull(conexao);
    }

}