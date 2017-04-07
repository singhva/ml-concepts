package com.ibm.watson;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Ingest {

	public static void main(String[] args) throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		
		Path baseDir = Paths.get("/Users/singhv/Documents/Data/lucene");
		Path indexDir = baseDir.resolve("index");
		Path sourceDir = baseDir.resolve("source");

	    // Store the index in memory:
	    //Directory directory = new RAMDirectory();
	    // To store an index on disk, use this instead:
	    Directory directory = FSDirectory.open(indexDir);
	    IndexWriterConfig config = new IndexWriterConfig(analyzer);
	    IndexWriter iwriter = new IndexWriter(directory, config);
	    //Document doc = new Document();
	    //String text = "This is the text to be indexed.";
	    
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, "*.txt")) {
	    	for (Path filePath : stream) {
	    		System.out.println(filePath.toString());
	    		Document doc = new Document();
		    	String text = new String(Files.readAllBytes(filePath));
		    	doc.add(new Field("abstract", text, TextField.TYPE_STORED));
		    	iwriter.addDocument(doc);
	    	}
	    } catch (DirectoryIteratorException e) {
	    	System.err.println(e);
	    }
	   
	    iwriter.close();

	}

}