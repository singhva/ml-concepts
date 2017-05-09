package com.ibm.watson;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queries.CustomScoreProvider;

public class WgaCustomScoreProvider extends CustomScoreProvider {

	public WgaCustomScoreProvider(LeafReaderContext context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public float customScore(int doc, float subQueryScore, float[] valSrcScores) throws IOException {
		// TODO Auto-generated method stub
		IndexReader r = context.reader();
		Terms tv = r.getTermVector(doc, "name");
		Document document = r.document(doc);
		System.out.println(document.get("code"));
		return 1.0f;
	}

}