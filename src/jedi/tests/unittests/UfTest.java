/***********************************************************************************************
 * @(#)UfTest.java
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

import app.models.Pais;
import app.models.Uf;

public class UfTest {

    @AfterClass
    public static void testCleanup() {
        for (Uf uf : Uf.objects.<Uf> all() ) {
            uf.delete();
            uf.getPais().delete();
        }
    }

    @Test
    public void testInsert() {
        Uf ufEsperada = new Uf();
        ufEsperada.setNome("Goiaz");
        ufEsperada.setSigla("GO");
        Pais pais = new Pais();
        pais.setNome("Brasil");
        pais.setSigla("BR");
        ufEsperada.setPais(pais);
        ufEsperada.insert();
        Uf ufObtida = Uf.objects.get("sigla", "GO");
        Assert.assertEquals(ufEsperada.getId(), ufObtida.getId() );
    }

    @Test
    public void testUpdate() {
        Uf ufEsperada = Uf.objects.get("sigla", "GO");
        ufEsperada.update("nome='Goiás'");
        Uf ufObtida = Uf.objects.get("sigla", "GO");
        Assert.assertTrue(ufEsperada.getNome().equals(ufObtida.getNome() ) );
    }

    @Test
    public void testDelete() {
        int esperado = 0;
        Pais.objects.all().delete();
        Uf.objects.all().delete();
        int obtido = Uf.objects.all().count();
        Assert.assertEquals(esperado, obtido);
    }

    @Test
    public void testSaveInsert() {
        Uf ufEsperada = new Uf();
        ufEsperada.setNome("Sao Paulo");
        ufEsperada.setSigla("SP");
        Pais pais = new Pais("Brasil", "BR");
        ufEsperada.setPais(pais);
        ufEsperada.save();
        Uf ufObtida = Uf.objects.get("sigla", "SP");
        Assert.assertEquals(ufEsperada.getId(), ufObtida.getId() );
    }

    @Test
    public void testSaveUpdate() {
        Uf ufEsperada = Uf.objects.get("sigla", "SP");
        ufEsperada.setNome("São Paulo");
        ufEsperada.save();
        Uf ufObtida = Uf.objects.get("sigla", "SP");
        Assert.assertTrue(ufEsperada.getNome().equals(ufObtida.getNome() ) );
    }
}