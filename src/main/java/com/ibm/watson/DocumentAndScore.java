package com.ibm.watson;

import org.apache.lucene.document.Document;

public class DocumentAndScore {
	private Document document;
	private float score;
	
	public DocumentAndScore(Document document, float score) {
		this.document = document;
		this.score = score;
	}
	
	public Document getDocument() {
		return document;
	}
	public void setDocument(Document document) {
		this.document = document;
	}
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}
}