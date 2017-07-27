package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by mayhew2 on 6/8/17.
 */
public class KBStringMatcher {
    public static void main(String[] args) throws IOException, ParseException {
        //String dir = "/shared/corpora/corporaWeb/lorelei/data/kb/LDC2017E19_LORELEI_EDL_Knowledge_Base_V0.1/data/";
        //String fname = "entities.tab";
        String kbpath = "/shared/corpora/cddunca2/allCountriesHeader.txt";
        String indexpath = "/shared/experiments/mayhew2/indices/allcountries-lucene";

        //buildindex(kbpath, indexpath);
        //testindex(indexpath);

        //stringmatcher("/shared/corpora/ner/eval/submission/ner/cp3/ensemble3.tab.uly.short", indexpath);
        stringmatcher("/shared/corpora/edl/lorelei/amh-anno-all.txt", indexpath);
    }

    private static Analyzer analyzer = new Analyzer() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer source = new NGramTokenizer(2,5);
            return new TokenStreamComponents(source);
        }
    };

    public static void buildindex(String kbpath, String indexDir) throws IOException {

        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter writer = new IndexWriter(dir, config);

        int j = 0;
        String[] headerfields = new String[10]; // just pick a random num...
        try (BufferedReader br = new BufferedReader(new FileReader(kbpath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                if(j == 0){
                    headerfields = line.split("\t");
                    j++;
                    continue;
                }

                if (j % 1000 == 0) {
                    System.out.println("Progress: " + j / 11000000.);
                }
                j++;

                String[] sline = line.split("\t");

                Document d = new Document();

                for (int i = 0; i < sline.length; i++) {
                    String field = headerfields[i];
                    String value = sline[i];

                    //                    StringReader sr = new StringReader(value);
                    StringField sf = new StringField(field, value, Field.Store.YES);
                    d.add(sf);

                    Reader reader = new StringReader(value);
                    TextField tf = new TextField(field, reader);
                    d.add(tf);

                    try {
                        if (field.equals("latitude")) {
                            double lat = Double.parseDouble(value);
                            double lon = Double.parseDouble(sline[i + 1]);
                            LatLonPoint llp = new LatLonPoint("latlon", lat, lon);
                            d.add(llp);
                        }
                    } catch (NumberFormatException e) {
                        // just don't do anything...
                    }


//                if(field.equals("entityid")){
//                    Field id = new StoredField(field, value);
//                    d.add(id);
//                }else {
//                }
                }

                writer.addDocument(d);

                //if(j > 100000){break;}
            }
        }

        writer.close();
    }


    public static void stringmatcherconll(String conllfolder){

        CoNLLNerReader cnr;
        TextAnnotation ta;
        for(String fname : (new File(conllfolder)).list()){
            cnr = new CoNLLNerReader(fname);
            ta = cnr.next();

            View ner = ta.getView(ViewNames.NER_CONLL);
            for(Constituent c : ner.getConstituents()){
                // now we will string match all constituents against a KB.
            }

        }
    }

    /**
     * This takes a submission file and links all the entries.
     * @param subfile
     */
    public static void stringmatcher(String subfile, String indexdir) throws IOException, ParseException {

        List<String> lines = LineIO.read(subfile);
        ArrayList<String> outlines = new ArrayList<>();
        ArrayList<String> outlines2 = new ArrayList<>();

        double i = 0;
        int coverage = 0;
        int nils = 0;

        for(String line : lines){
            if(i % 10 == 0){
                System.out.println("Progress: " + (i / lines.size()));
            }

            i++;

            String[] sline = line.split("\t");

            String mention = sline[2];

            Document[] cands = getcands(mention, indexdir, 20);

            List<String> candids = new ArrayList<>();
            List<String> candnames = new ArrayList<>();
            for(Document cand : cands){
                candids.add(cand.get("entityid"));
                candnames.add(cand.get("asciiname") + ":" + cand.get("entityid"));
            }
            String candstring = StringUtils.join(candnames, ",");

            String[] sline2 = line.split("\t");
            sline2[4] = candstring;
            outlines2.add(StringUtils.join(sline2, "\t"));

            String goldscore = sline[4];
            if(goldscore.equals("NIL")){
                System.out.println("NIL gold");
                nils++;
            }else if(candids.contains(goldscore)){
                coverage++;
            }else{
                System.out.println("No cands for: " + mention);
            }

            if(cands.length > 0){
                // this should really be an ID, but for now, it is just this!
                Document best = getbest(mention, cands);
                sline[4] = best.get("entityid");
                sline[5] = best.get("asciiname");
            }else{
                sline[4] = "null";
            }

            outlines.add(StringUtils.join(sline, "\t"));

        }

        System.out.println("Coverage: " + coverage/((float)lines.size()-nils));


        LineIO.write(subfile + ".linked", outlines);
        LineIO.write(subfile + ".cands", outlines2);

    }

    private static List<String> getngrams(String s, int n){
        List<String> ret = new ArrayList<>();
        for(int i = 0; i < s.length()- n+1; i++){
            ret.add(s.substring(i, i+n));
        }
        return ret;
    }

    private static float jaccard(Set<String> a, Set<String> b){
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return inter.size() / (float) union.size();

    }

    private static Document getbest(String mention, Document[] cands) {
        List<String> mentionngrams = getngrams(mention, 2);

        double mxjaccard = -1;
        double mxalts = -1;
        Document best = cands[0];
        for(Document d : cands){
            String candsurf = d.get("asciiname");

            List<String> candngrams = getngrams(candsurf, 2);
            double jaccard = jaccard(new HashSet<>(mentionngrams), new HashSet<>(candngrams));
            double alts = d.get("name2").split(",").length;
            double score = jaccard * alts;

            if(jaccard > mxjaccard){
                mxjaccard = jaccard;
            }
            if(alts > mxalts){
                mxalts = alts;
            }

            d.add(new DoublePoint("score", score));

        }

        double denom = mxalts * mxjaccard;

        double mxscore = -1;
        for(Document d : cands){
            DoublePoint scorefield = (DoublePoint) d.getField("score");
            double score = scorefield.numericValue().doubleValue();
            score = score / denom;
            if(score > mxscore){
                best = d;
                mxscore = score;
            }
        }
        return best;
    }


    public static void getnearest(IndexSearcher searcher, double lat, double lon) throws IOException {
        // How to query with a lat/long.
        TopFieldDocs tfd = LatLonPoint.nearest(searcher, "latlon", lat, lon, 10);
        ScoreDoc[] hits = tfd.scoreDocs;

        for(int i=0; i<hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);

            FieldDoc fd = (FieldDoc) hits[i];
            double dist = (double)fd.fields[0];

            System.out.println((i + 1) + ". " + d.get("entityid") + ", " + d.get("asciiname") + ", dist (m)=" + dist);
        }
    }

    public static Document[] getcands(String mention, String indexdir, int n) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
        IndexSearcher searcher = new IndexSearcher(reader);

        Query q;
        try {
            q = new QueryParser("asciiname", analyzer).parse(mention);
        } catch (ParseException e) {
            Document[] results = new Document[0];
            return results;
        }

        TopScoreDocCollector collector = TopScoreDocCollector.create(n);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        Document[] results = new Document[hits.length];

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            results[i] = d;
        }

        return results;
    }


    public static void testindex(String indexdir) throws IOException {
        //=========================================================
        // Now search
        //=========================================================
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
        IndexSearcher searcher = new IndexSearcher(reader);

        BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));

        String s = "";
        while (!s.equalsIgnoreCase("q")) {
            try {
                System.out.println("Enter the search query (q=quit): ");
                s = br.readLine();
                if (s.equalsIgnoreCase("q")) {
                    break;
                }
                if(s.startsWith("p ")){
                    String[] parts = s.split(" ");
                    getnearest(searcher, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }else {

                    Query q = new QueryParser("asciiname", analyzer).parse(s);

                    System.out.println(q);
                    TopScoreDocCollector collector = TopScoreDocCollector.create(5);
                    searcher.search(q, collector);
                    ScoreDoc[] hits = collector.topDocs().scoreDocs;

                    System.out.println("There are total of: " + searcher.count(q) + " hits.");

                    // 4. display results
                    System.out.println("Found " + hits.length + " hits.");
                    for (int i = 0; i < hits.length; ++i) {
                        int docId = hits[i].doc;
                        Document d = searcher.doc(docId);

//                    System.out.println(d.getFields());
                        System.out.println((i + 1) + ". " + d.get("entityid") + ", " + d.get("asciiname") + " score=" + hits[i].score);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error searching " + s + " : " + e.getMessage());
            }
        }
        reader.close();
    }
}
