/***********************************************************************************************
 * @(#)PaisTest.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/18
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

import jedi.db.engine.JediORMEngine;
import jedi.db.models.query.QuerySet;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import app.models.Pais;

public class PaisTest {

	@BeforeClass
	public static void testSetup() {
		JediORMEngine.FOREIGN_KEY_CHECKS = false;
		// Drops and generates tables respectively.
		JediORMEngine.flush();		
		QuerySet<Pais> paises = null;
		paises = new QuerySet<Pais>();
		paises.entity(Pais.class);
		paises.add(new Pais("Albania", "AL"));
		paises.add(new Pais("Armenia", "AM"));
		paises.add(new Pais("Netherlands Antilles", "AN"));
		paises.add(new Pais("Angola", "AO"));
		paises.add(new Pais("Antarctica", "AQ"));
		paises.add(new Pais("Argentina", "AR"));
		paises.add(new Pais("American Samoa", "AS"));
		paises.add(new Pais("Austria", "AT"));
		paises.add(new Pais("Australia", "AU"));
		paises.add(new Pais("Aruba", "AW"));
		paises.add(new Pais("Åland Islands", "AX"));
		paises.add(new Pais("Azerbaijan", "AZ"));
		paises.add(new Pais("Bosnia and Herzegovina", "BA"));
		paises.add(new Pais("Barbados", "BB"));
		paises.add(new Pais("Bangladesh", "BD"));
		paises.add(new Pais("Belgium", "BE"));
		paises.add(new Pais("Burkina Faso", "BF"));
		paises.add(new Pais("Bulgaria", "BG"));
		paises.add(new Pais("Bahrain", "BH"));
		paises.add(new Pais("Burundi", "BI"));
		paises.add(new Pais("Benin", "BJ"));
		paises.add(new Pais("Saint Barthélemy", "BL"));
		paises.add(new Pais("Bermuda", "BM"));
		paises.add(new Pais("Brunei Darussalam", "BN"));
		paises.add(new Pais("Bolivia, Plurinational State of", "BO"));
		// paises.add(new Pais("Brazil", "BR") );
		paises.add(new Pais("Bahamas", "BS"));
		paises.add(new Pais("Bhutan", "BT"));
		paises.add(new Pais("Bouvet Island", "BV"));
		paises.add(new Pais("Botswana", "BW"));
		paises.add(new Pais("Belarus", "BY"));
		paises.add(new Pais("Belize", "BZ"));
		paises.add(new Pais("Canada", "CA"));
		paises.add(new Pais("Cocos (Keeling) Islands", "CC"));
		paises.add(new Pais("Congo, the Democratic Republic of the", "CD"));
		paises.add(new Pais("Central African Republic", "CF"));
		paises.add(new Pais("Congo", "CG"));
		paises.add(new Pais("Switzerland", "CH"));
		paises.add(new Pais("Côte d'Ivoire", "CI"));
		paises.add(new Pais("Cook Islands", "CK"));
		paises.add(new Pais("Chile", "CL"));
		paises.add(new Pais("Cameroon", "CM"));
		paises.add(new Pais("China", "CN"));
		paises.add(new Pais("Colombia", "CO"));
		paises.add(new Pais("Costa Rica", "CR"));
		paises.add(new Pais("Cuba", "CU"));
		paises.add(new Pais("Cape Verde", "CV"));
		paises.add(new Pais("Christmas Island", "CX"));
		paises.add(new Pais("Cyprus", "CY"));
		paises.add(new Pais("Czech Republic", "CZ"));
		paises.add(new Pais("Germany", "DE"));
		paises.add(new Pais("Djibouti", "DJ"));
		paises.add(new Pais("Denmark", "DK"));
		paises.add(new Pais("Dominica", "DM"));
		paises.add(new Pais("Dominican Republic", "DO"));
		paises.add(new Pais("Algeria", "DZ"));
		paises.add(new Pais("Ecuador", "EC"));
		paises.add(new Pais("Estonia", "EE"));
		paises.add(new Pais("Egypt", "EG"));
		paises.add(new Pais("Western Sahara", "EH"));
		paises.add(new Pais("Eritrea", "ER"));
		paises.add(new Pais("Spain", "ES"));
		paises.add(new Pais("Ethiopia", "ET"));
		paises.add(new Pais("Finland", "FI"));
		paises.add(new Pais("Fiji", "FJ"));
		paises.add(new Pais("Falkland Islands (Malvinas)", "FK"));
		paises.add(new Pais("Micronesia, Federated States of", "FM"));
		paises.add(new Pais("Faroe Islands", "FO"));
		paises.add(new Pais("France", "FR"));
		paises.add(new Pais("Gabon", "GA"));
		paises.add(new Pais("United Kingdom", "GB"));
		paises.add(new Pais("Grenada", "GD"));
		paises.add(new Pais("Georgia", "GE"));
		paises.add(new Pais("French Guiana", "GF"));
		paises.add(new Pais("Guernsey", "GG"));
		paises.add(new Pais("Ghana", "GH"));
		paises.add(new Pais("Gibraltar", "GI"));
		paises.add(new Pais("Greenland", "GL"));
		paises.add(new Pais("Gambia", "GM"));
		paises.add(new Pais("Guinea", "GN"));
		paises.add(new Pais("Guadeloupe", "GP"));
		paises.add(new Pais("Equatorial Guinea", "GQ"));
		paises.add(new Pais("Greece", "GR"));
		paises.add(new Pais("South Georgia and the South Sandwich Islands", "GS"));
		paises.add(new Pais("Guatemala", "GT"));
		paises.add(new Pais("Guam", "GU"));
		paises.add(new Pais("Guinea-Bissau", "GW"));
		paises.add(new Pais("Guyana", "GY"));
		paises.add(new Pais("Hong Kong", "HK"));
		paises.add(new Pais("Heard Island and McDonald Islands", "HM"));
		paises.add(new Pais("Honduras", "HN"));
		paises.add(new Pais("Croatia", "HR"));
		paises.add(new Pais("Haiti", "HT"));
		paises.add(new Pais("Hungary", "HU"));
		paises.add(new Pais("Indonesia", "ID"));
		paises.add(new Pais("Ireland", "IE"));
		paises.add(new Pais("Israel", "IL"));
		paises.add(new Pais("Isle of Man", "IM"));
		paises.add(new Pais("India", "IN"));
		paises.add(new Pais("British Indian Ocean Territory", "IO"));
		paises.add(new Pais("Iraq", "IQ"));
		paises.add(new Pais("Iran, Islamic Republic of", "IR"));
		paises.add(new Pais("Iceland", "IS"));
		paises.add(new Pais("Italy", "IT"));
		paises.add(new Pais("Jersey", "JE"));
		paises.add(new Pais("Jamaica", "JM"));
		paises.add(new Pais("Jordan", "JO"));
		paises.add(new Pais("Japan", "JP"));
		paises.add(new Pais("Kenya", "KE"));
		paises.add(new Pais("Kyrgyzstan", "KG"));
		paises.add(new Pais("Cambodia", "KH"));
		paises.add(new Pais("Kiribati", "KI"));
		paises.add(new Pais("Comoros", "KM"));
		paises.add(new Pais("Saint Kitts and Nevis", "KN"));
		paises.add(new Pais("Korea, Democratic People's Republic of", "KP"));
		paises.add(new Pais("Korea, Republic of", "KR"));
		paises.add(new Pais("Kuwait", "KW"));
		paises.add(new Pais("Cayman Islands", "KY"));
		paises.add(new Pais("Kazakhstan", "KZ"));
		paises.add(new Pais("Lao People's Democratic Republic", "LA"));
		paises.add(new Pais("Lebanon", "LB"));
		paises.add(new Pais("Saint Lucia", "LC"));
		paises.add(new Pais("Liechtenstein", "LI"));
		paises.add(new Pais("Sri Lanka", "LK"));
		paises.add(new Pais("Liberia", "LR"));
		paises.add(new Pais("Lesotho", "LS"));
		paises.add(new Pais("Lithuania", "LT"));
		paises.add(new Pais("Luxembourg", "LU"));
		paises.add(new Pais("Latvia", "LV"));
		paises.add(new Pais("Libyan Arab Jamahiriya", "LY"));
		paises.add(new Pais("Morocco", "MA"));
		paises.add(new Pais("Monaco", "MC"));
		paises.add(new Pais("Moldova, Republic of", "MD"));
		paises.add(new Pais("Montenegro", "ME"));
		paises.add(new Pais("Saint Martin (French part)", "MF"));
		paises.add(new Pais("Madagascar", "MG"));
		paises.add(new Pais("Marshall Islands", "MH"));
		paises.add(new Pais("Macedonia, the former Yugoslav Republic of", "MK"));
		paises.add(new Pais("Mali", "ML"));
		paises.add(new Pais("Myanmar", "MM"));
		paises.add(new Pais("Mongolia", "MN"));
		paises.add(new Pais("Macao", "MO"));
		paises.add(new Pais("Northern Mariana Islands", "MP"));
		paises.add(new Pais("Martinique", "MQ"));
		paises.add(new Pais("Mauritania", "MR"));
		paises.add(new Pais("Montserrat", "MS"));
		paises.add(new Pais("Malta", "MT"));
		paises.add(new Pais("Mauritius", "MU"));
		paises.add(new Pais("Maldives", "MV"));
		paises.add(new Pais("Malawi", "MW"));
		paises.add(new Pais("Mexico", "MX"));
		paises.add(new Pais("Malaysia", "MY"));
		paises.add(new Pais("Mozambique", "MZ"));
		paises.add(new Pais("Namibia", "NA"));
		paises.add(new Pais("New Caledonia", "NC"));
		paises.add(new Pais("Niger", "NE"));
		paises.add(new Pais("Norfolk Island", "NF"));
		paises.add(new Pais("Nigeria", "NG"));
		paises.add(new Pais("Nicaragua", "NI"));
		paises.add(new Pais("Netherlands", "NL"));
		paises.add(new Pais("Norway", "NO"));
		paises.add(new Pais("Nepal", "NP"));
		paises.add(new Pais("Nauru", "NR"));
		paises.add(new Pais("Niue", "NU"));
		paises.add(new Pais("New Zealand", "NZ"));
		paises.add(new Pais("Oman", "OM"));
		paises.add(new Pais("Panama", "PA"));
		paises.add(new Pais("Peru", "PE"));
		paises.add(new Pais("French Polynesia", "PF"));
		paises.add(new Pais("Papua New Guinea", "PG"));
		paises.add(new Pais("Philippines", "PH"));
		paises.add(new Pais("Pakistan", "PK"));
		paises.add(new Pais("Poland", "PL"));
		paises.add(new Pais("Saint Pierre and Miquelon", "PM"));
		paises.add(new Pais("Pitcairn", "PN"));
		paises.add(new Pais("Puerto Rico", "PR"));
		paises.add(new Pais("Palestinian Territory, Occupied", "PS"));
		paises.add(new Pais("Portugal", "PT"));
		paises.add(new Pais("Palau", "PW"));
		paises.add(new Pais("Paraguay", "PY"));
		paises.add(new Pais("Qatar", "QA"));
		paises.add(new Pais("Réunion", "RE"));
		paises.add(new Pais("Romania", "RO"));
		paises.add(new Pais("Serbia", "RS"));
		paises.add(new Pais("Russian Federation", "RU"));
		paises.add(new Pais("Rwanda", "RW"));
		paises.add(new Pais("Saudi Arabia", "SA"));
		paises.add(new Pais("Solomon Islands", "SB"));
		paises.add(new Pais("Seychelles", "SC"));
		paises.add(new Pais("Sudan", "SD"));
		paises.add(new Pais("Sweden", "SE"));
		paises.add(new Pais("Singapore", "SG"));
		paises.add(new Pais("Saint Helena", "SH"));
		paises.add(new Pais("Slovenia", "SI"));
		paises.add(new Pais("Svalbard and Jan Mayen", "SJ"));
		paises.add(new Pais("Slovakia", "SK"));
		paises.add(new Pais("Sierra Leone", "SL"));
		paises.add(new Pais("San Marino", "SM"));
		paises.add(new Pais("Senegal", "SN"));
		paises.add(new Pais("Somalia", "SO"));
		paises.add(new Pais("Suriname", "SR"));
		paises.add(new Pais("Sao Tome and Principe", "ST"));
		paises.add(new Pais("El Salvador", "SV"));
		paises.add(new Pais("Syrian Arab Republic", "SY"));
		paises.add(new Pais("Swaziland", "SZ"));
		paises.add(new Pais("Turks and Caicos Islands", "TC"));
		paises.add(new Pais("Chad", "TD"));
		paises.add(new Pais("French Southern Territories", "TF"));
		paises.add(new Pais("Togo", "TG"));
		paises.add(new Pais("Thailand", "TH"));
		paises.add(new Pais("Tajikistan", "TJ"));
		paises.add(new Pais("Tokelau", "TK"));
		paises.add(new Pais("Timor-Leste", "TL"));
		paises.add(new Pais("Turkmenistan", "TM"));
		paises.add(new Pais("Tunisia", "TN"));
		paises.add(new Pais("Tonga", "TO"));
		paises.add(new Pais("Turkey", "TR"));
		paises.add(new Pais("Trinidad and Tobago", "TT"));
		paises.add(new Pais("Tuvalu", "TV"));
		paises.add(new Pais("Taiwan, Province of China", "TW"));
		paises.add(new Pais("Tanzania, United Republic of", "TZ"));
		paises.add(new Pais("Ukraine", "UA"));
		paises.add(new Pais("Uganda", "UG"));
		paises.add(new Pais("United States Minor Outlying Islands", "UM"));
		// paises.add(new Pais("United States", "US") );
		paises.add(new Pais("Uruguay", "UY"));
		paises.add(new Pais("Uzbekistan", "UZ"));
		paises.add(new Pais("Holy See (Vatican City State)", "VA"));
		paises.add(new Pais("Saint Vincent and the Grenadines", "VC"));
		paises.add(new Pais("Venezuela, Bolivarian Republic of", "VE"));
		paises.add(new Pais("Virgin Islands, British", "VG"));
		paises.add(new Pais("Virgin Islands, U.S.", "VI"));
		paises.add(new Pais("Viet Nam", "VN"));
		paises.add(new Pais("Vanuatu ", "VU"));
		paises.add(new Pais("Wallis and Futuna", "WF"));
		paises.add(new Pais("Samoa", "WS"));
		paises.add(new Pais("Yemen", "YE"));
		paises.add(new Pais("Mayotte", "YT"));
		paises.add(new Pais("South Africa", "ZA"));
		paises.add(new Pais("Zambia", "ZM"));
		paises.add(new Pais("Zimbabwe", "ZW"));		
		paises.save();		
	}
	
	@AfterClass
	public static void testCleanup() {
		// Deletes all the rows on the table after the tests.
		// Pais.objects.all().delete();
		JediORMEngine.droptables();
	}

	@Test
	public void testInsert() {
		Pais paisEsperado = new Pais("Brasil", "BR");
		paisEsperado.insert();
		Pais paisObtido = Pais.objects.get("nome", "Brasil");
		Assert.assertEquals(paisEsperado.getId(), paisObtido.getId());
	}

	@Test
	public void testUpdate() {
		Pais paisEsperado = Pais.objects.get("nome", "Brasil");
		paisEsperado.update("nome='Brazil'");
		Pais paisObtido = Pais.objects.get("sigla", "BR");
		Assert.assertTrue(paisEsperado.getNome().equals(paisObtido.getNome()));
	}

	@Test
	public void testDelete() {
		int esperado = 0;
		for (Pais pais : Pais.objects.<Pais>all()) {
			pais.delete();
		}
		int obtido = Pais.objects.count();
		Assert.assertEquals(esperado, obtido);
	}

	@Test
	public void testSaveInsert() {
		Pais paisEsperado = new Pais();
		paisEsperado.setNome("Estados Unidos da América");
		paisEsperado.setSigla("UU");
		paisEsperado.setCapital("Washington");
		paisEsperado.setContinente("Americano");
		paisEsperado.save();
		Pais paisObtido = Pais.objects.get("sigla", "UU");
		Assert.assertEquals(paisEsperado.getId(), paisObtido.getId());
	}

	@Test
	public void testSaveUpdate() {
		Pais paisEsperado = Pais.objects.get("sigla", "UU");
		paisEsperado.setSigla("US");
		paisEsperado.save();
		Pais paisObtido = Pais.objects.get("sigla", "US");
		Assert.assertTrue(paisEsperado.getNome().equals(paisObtido.getNome()));
	}
}