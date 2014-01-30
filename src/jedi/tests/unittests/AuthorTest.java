/***********************************************************************************************
 * @(#)AuthorTest.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/29
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

import app.models.Author;

public class AuthorTest {

    @AfterClass
    public static void testCleanup() {
        for (Author author : Author.objects.<Author> all()) {
            author.delete();
        }
    }

    @Test
    public void testInsert() {
        Author expectedAuthor = new Author();
        expectedAuthor.setFirstName("Paulo");
        expectedAuthor.setLastName("Coelhoo");
        expectedAuthor.setEmail("paulocoelho@gmail.com");
        expectedAuthor.save();
        Author obtainedAuthor = Author.objects.get("email", "paulocoelho@gmail.com");
        Assert.assertEquals(expectedAuthor.getId(), obtainedAuthor.getId());
    }

    @Test
    public void testUpdate() {
        Author expectedAuthor = Author.objects.get("email", "paulocoelho@gmail.com");
        expectedAuthor.update("lastName='Coelho'");
        Author obtainedAuthor = Author.objects.get("email", "paulocoelho@gmail.com");
        Assert.assertTrue(expectedAuthor.getLastName().equals(obtainedAuthor.getLastName()));
    }

    @Test
    public void testDelete() {
        int expected = 0;
        Author.objects.all().delete();
        int obtained = Author.objects.all().count();
        Assert.assertEquals(expected, obtained);
    }

    @Test
    public void testSaveInsert() {
        Author expectedAuthor = new Author();
        expectedAuthor.setFirstName("John Ronald");
        expectedAuthor.setLastName("Reuel Tolkienn");
        expectedAuthor.setEmail("jrrtolkien@gmail.com");
        expectedAuthor.save();
        Author obtainedAuthor = Author.objects.get("email", "jrrtolkien@gmail.com");
        Assert.assertEquals(expectedAuthor.getId(), obtainedAuthor.getId());
    }

    @Test
    public void testSaveUpdate() {
        Author expectedAuthor = Author.objects.get("email", "jrrtolkien@gmail.com");
        expectedAuthor.setLastName("Reuel Tolkien");
        expectedAuthor.save();
        Author obtainedAuthor = Author.objects.get("email", "jrrtolkien@gmail.com");
        Assert.assertTrue(expectedAuthor.getLastName().equals(obtainedAuthor.getLastName()));
    }
}