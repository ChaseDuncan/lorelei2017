package edu.illinois.cs.cogcomp.lorelei.kb;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

/**
 * Created by mayhew2 on 7/28/17.
 */
public class TestSimilarity extends SimilarityBase {
    @Override
    protected float score(BasicStats basicStats, float v, float v1) {

        return 0;
    }

    @Override
    public String toString() {
        return null;
    }
}
