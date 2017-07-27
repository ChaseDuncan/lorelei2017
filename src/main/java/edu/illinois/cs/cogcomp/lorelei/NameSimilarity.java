package edu.illinois.cs.cogcomp.lorelei;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

import java.io.IOException;

/**
 * Created by mayhew2 on 7/25/17.
 */
public class NameSimilarity extends Similarity {

    @Override
    public long computeNorm(FieldInvertState fieldInvertState) {
        return 0;
    }

    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStatistics, TermStatistics... termStatisticses) {

        return null;
    }

    @Override
    public SimScorer simScorer(SimWeight simWeight, LeafReaderContext leafReaderContext) throws IOException {
        return null;
    }
}
