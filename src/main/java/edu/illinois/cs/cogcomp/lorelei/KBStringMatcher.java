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
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mayhew2 on 6/8/17.
 */
public class KBStringMatcher {
    public static void main(String[] args) throws IOException, ParseException {
        //String dir = "/shared/corpora/corporaWeb/lorelei/data/kb/LDC2017E19_LORELEI_EDL_Knowledge_Base_V0.1/data/";
        String dir = "./";
        String kbpath = dir + "allCountriesHeader.txt";

        String indexpath = "/tmp/kb-full-lucene/";

        buildindex(kbpath, indexpath);
        //testindex(indexpath);

        //stringmatcher("ensemble3.tab.uly.short", indexpath);

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
                    System.out.println("Progress: " + j);
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

        double i = 0;

        for(String line : lines){
            if(i % 10 == 0){
                System.out.println("Progress: " + (i / lines.size()));
            }

            i++;

            String[] sline = line.split("\t");

            String mention = sline[2];
            System.out.println(mention);

            String[] cands = getcands(mention, indexdir);

            if(cands.length > 0){
                // this should really be an ID, but for now, it is just this!
                sline[4] = cands[0];
            }else{
                sline[4] = "null";
            }

            outlines.add(StringUtils.join(sline, "\t"));

        }

        LineIO.write(subfile + ".linked", outlines);

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

    public static String[] getcands(String mention, String indexdir) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexdir)));
        IndexSearcher searcher = new IndexSearcher(reader);

        Query q;
        try {
            q = new QueryParser("asciiname", analyzer).parse(mention);
        } catch (ParseException e) {
            String[] results = new String[1];
            results[0] = "null";
            return results;
        }

        TopScoreDocCollector collector = TopScoreDocCollector.create(5);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        String[] results = new String[hits.length];

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            results[i] = d.get("asciiname");
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
