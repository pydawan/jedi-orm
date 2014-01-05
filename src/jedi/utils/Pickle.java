package jedi.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

/**
 * Classe responsável pela serialização e deserialização de objetos.
 * Sua implementação foi baseada no módulo Pickle da linguagem Python.
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 */
public abstract class Pickle {
	
	/**
	 * Método responsável pela serialização de um objeto em um arquivo.
	 * 
	 * @param o - objeto a ser serializado.
	 * @param f - arquivo onde ocorrerá a serialização.
	 * @return void
	 */
	public static void dump(Object o, File f) {
		dump(o, f, false);		
	}
	
	/**
	 * Método responsável pela serialização de um objeto em um arquivo.
	 * 
	 * @param o - objeto a ser serializado.
	 * @param f - arquivo onde ocorrerá a serialização.
	 * @param append - define se a gravação deve ou não ocorrer de forma a anexar o novo contéudo.
	 */
	public static void dump(Object o, File f, boolean append) {
		
		if (o != null && f != null) {
			
			try {
				
				// Arquivo onde os dados serão escritos.
				FileOutputStream fos = new FileOutputStream(f, append);
				
				// Objeto responsável pela escrita (writer) dos dados no arquivo.
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				
				oos.flush();
				
				// Escrevendo os dados.
				oos.writeObject(o);
				
				oos.close();
				
				fos.close();
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Método responsável por serializar um objeto na memória principal e retornar a
	 * sequência de bytes obtida como uma String.
	 * 
	 * @param Object object
	 * @return String
	 */
	
	public static String dumps(Object o) {
		
		String s = "";
		
		try {
			
			// Serializando.
			
			// Referência para uma sequência de bytes na memória.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			// Referência para o serializador (o processo pode ocorrer na memória principal ou secundária).
			// Nesse caso ocorre na memória principal.
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			
			oos.writeObject(o);
			
			oos.close();
			
			// Transformando o array de bytes em String.
			// A conversão padrão para String corrompe alguns bytes por isso é utilizada a Base64.
			s = new String( Base64.encodeBase64( baos.toByteArray() ) );
			
			baos.close();
			
		} catch (IOException e) {

			e.printStackTrace();
			
		}
		
		return s;
	}
	
	/**
	 * Método responsável por deserializar objetos.
	 * 
	 * @param f
	 * @return Object
	 */
	public static Object load(File f) {

		Object o = null;
		
		if (f != null) {
		
			try {
				
				FileInputStream fis = new FileInputStream(f);
				
				// Objeto responsável pela leitura (reader) dos dados armazenados no arquivo.
				ObjectInputStream ois = new ObjectInputStream(fis);

				o = ois.readObject();
				
				ois.close();
				
				fis.close();
				
			} catch (IOException e) {
				
				e.printStackTrace();
				
			} catch (ClassNotFoundException e) {
				
				e.printStackTrace();
			}
		}
		
		return o;
	}
	
	
	/**
	 * Método que recebe deserializa um objeto a partir de uma sequência de bytes codificados.
	 * 
	 * @param s
	 * @return Object
	 */
	public static Object loads(String s) {
		
		Object o = null;
		
		ByteArrayInputStream bais;
		
		try {
			
			bais = new ByteArrayInputStream( Base64.decodeBase64(s.getBytes() ) );
			
			ObjectInputStream ois = new ObjectInputStream(bais);
			
			o = ois.readObject();
			
			ois.close();
			
			bais.close();
		
		} catch (UnsupportedEncodingException e) {
		
			e.printStackTrace();
			
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return o;
	}
}
