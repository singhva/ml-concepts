package com.ibm.watson.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

public class CustomSimilarity extends TFIDFSimilarity {

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}

	@Override
	public float coord(int overlap, int maxOverlap) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float tf(float freq) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float idf(long docFreq, long docCount) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float lengthNorm(FieldInvertState state) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float decodeNormValue(long norm) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long encodeNormValue(float f) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float sloppyFreq(int distance) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float scorePayload(int doc, int start, int end, BytesRef payload) {
		// TODO Auto-generated method stub
		return 0;
	}

}