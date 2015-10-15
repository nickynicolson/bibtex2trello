package org.kew.bibtex2trello;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXFormatter;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.Value;

import com.julienvey.trello.Trello;
import com.julienvey.trello.domain.Board;
import com.julienvey.trello.domain.Card;
import com.julienvey.trello.domain.TList;
import com.julienvey.trello.impl.TrelloImpl;

public class App {

	// Names of properties read from file:
	private static final String TRELLO_API_KEY = "TRELLO_API_KEY";
	private static final String TRELLO_ACCESS_TOKEN = "TRELLO_ACCESS_TOKEN";
	private static final String TRELLO_BOARD_ID = "TRELLO_BOARD_ID";
	private static final String TRELLO_LIST_NAME = "TRELLO_LIST_NAME";
	private static final String BIBTEXFILE = "BIBTEXFILE";
	
    public static void main( String[] args ){
    	try{
    		// Read properties:
    		Properties prop = new Properties();
    		InputStream stream = new FileInputStream(new File("./bibtex2trello.properties"));
    		prop.load(stream);

    		// Connect to the Trello API:
    		Trello trelloApi = new TrelloImpl(prop.getProperty(TRELLO_API_KEY), prop.getProperty(TRELLO_ACCESS_TOKEN));
    		// Locate the board / list to which we will send cards:
    		Board board = trelloApi.getBoard(prop.getProperty(TRELLO_BOARD_ID));
    		List<TList> lists = board.fetchLists();
    		// Loop over the lists till we find the one with the right name:
    		TList list = null;
    		for (TList tlist : lists){
    			if (tlist.getName().equals(prop.getProperty(TRELLO_LIST_NAME))){
    				list = tlist;
    				break;
    			}
    		}
    		
    		// Read bibtex from the specified file:
    		InputStreamReader reader = new InputStreamReader(new FileInputStream(prop.getProperty(BIBTEXFILE)), "utf8");
	    	BibTeXParser bibtexParser = new org.jbibtex.BibTeXParser();
	    	BibTeXDatabase database = bibtexParser.parse(reader);
	    	
	    	// Loop over entries, reading from bibtex and writing to Trello:
	    	for (Entry<Key,BibTeXEntry> e : database.getEntries().entrySet()){
	    		System.out.println(e.getKey().toString());
	    		// Article title will be the card title
	    		String title = e.getValue().getField(BibTeXEntry.KEY_TITLE).toUserString().replaceAll("\\{","").replaceAll("\\}","");
	    		System.out.println("Title: " + title);
	    			    		
	    		// Ideally we'd add bibtex format citation as a file attachment to the card, 
	    		// ...but the Trello API wrapper doesn't support addition of attachments as yet
	    		
	    		Card card = new Card();
	    		// Clean up curly braces in title:
	    		card.setName(title);
	    		card.setDesc(bibtextEntry2Md(e.getValue()));
	    		
	    		list.createCard(card);
	    	}
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    /**
     * Convert bibtex entry into markdown formatted string.
     * @param bibTeXEntry
     * @return
     */
    private static String bibtextEntry2Md(BibTeXEntry bibTeXEntry){
    	StringBuffer sb = new StringBuffer();
		// Write a DOI link into the description, if one is available:
		String doi = null;
		if (bibTeXEntry.getField(BibTeXEntry.KEY_DOI)!=null){
			doi = bibTeXEntry.getField(BibTeXEntry.KEY_DOI).toUserString();
		}
		if (doi != null){
			sb.append("http://dx.doi.org/" + doi);
		}
		for (Entry<Key,Value> e2 : bibTeXEntry.getFields().entrySet()){
			if (sb.length() > 0) sb.append("\n");
			sb.append("**").append(e2.getKey().toString()).append(":**\t").append(e2.getValue().toUserString());
			// Would be nice to show a link back to zotero for the library entry, but this data not written into bibtex...
		}
		sb.append("\n");
    	return sb.toString();
    }
    
    /**
     * Construct bibtex string from entry. 
     * @param bibTeXEntry
     * @return
     */
    private static String bibTeXEntry2BibTeXString(BibTeXEntry bibTeXEntry){
    	String bibTeX = null;
    	try{
			BibTeXDatabase databaseOut = new BibTeXDatabase();
			databaseOut.addObject(bibTeXEntry);
			BibTeXFormatter bibtexFormatter = new org.jbibtex.BibTeXFormatter();
			StringWriter sw = new StringWriter();
			bibtexFormatter.format(databaseOut, sw);
			bibTeX = sw.toString();
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	return bibTeX;
    }
}