package com.ibm.watson;
import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.Query;

public class WgaCustomScoringQuery extends CustomScoreQuery {

	public WgaCustomScoringQuery(Query subQuery) {
		super(subQuery);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected CustomScoreProvider getCustomScoreProvider(LeafReaderContext context) throws IOException {
		// TODO Auto-generated method stub
		return new WgaCustomScoreProvider(context);
	}

	
}