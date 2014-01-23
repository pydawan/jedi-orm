/***********************************************************************************************
 * @(#)EditoraTest.java
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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import app.models.Editora;
import app.models.Pais;
import app.models.Uf;

public class EditoraTest {

    @AfterClass
    public static void testCleanup() {
        Pais.objects.all().delete();
    }

    @Test
    public void testInsert() {

        Editora editoraEsperada = new Editora();
        editoraEsperada.setNome("Editora Abril");

        Uf uf = new Uf();
        uf.setNome("Goiás");
        uf.setSigla("GO");
        
        Pais pais = new Pais();
        pais.setNome("Brasil");
        pais.setSigla("BR");
        
        uf.setPais(pais);
        
        editoraEsperada.setUf(uf);
        editoraEsperada.insert();

        Editora editoraObtida = Editora.objects.get("nome", "Editora Abril");

        Assert.assertEquals(editoraEsperada.getId(), editoraObtida.getId() );
    }

    @Test
    public void testUpdate() {

        Editora editoraEsperada = Editora.objects.get("nome", "Editora Abril");

        Uf uf = new Uf("São Paulo", "SP", Pais.objects.get("nome", "Brasil").as(Pais.class) );
        uf.save();

        editoraEsperada.update(String.format("uf_id=%d", uf.getId() ) );

        Editora editoraObtida = Editora.objects.get("nome", "Editora Abril");

        Assert.assertEquals(editoraEsperada.getUf().getId(), editoraObtida.getUf().getId() );
    }

    @Test
    public void testDelete() {
        
        // NOTE: DELETE or UPDATE CASCADE only works with referential integrity enabled.
        int quantidadeEditorasEsperada = 0;
        int quantidadeEditorasObtida = Editora.objects.all().delete().count();

        Assert.assertEquals(quantidadeEditorasEsperada, quantidadeEditorasObtida);
    }

    @Test
    public void testSaveInsert() {

        Editora editoraEsperada = new Editora();
        editoraEsperada.setNome("McGraw Hill");
        editoraEsperada.setUf(new Uf("New York", "NY", new Pais("Unitated States of America", "US") ) );
        editoraEsperada.save();

        Editora editoraObtida = Editora.objects.get("nome", "McGraw Hill");

        Assert.assertEquals(editoraEsperada.getId(), editoraObtida.getId() );
    }
}
