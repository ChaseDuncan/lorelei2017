package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by mayhew2 on 6/8/17.
 */
public class KBStringMatcher {
    public static void main(String[] args) throws IOException {
        String dir = "/shared/corpora/corporaWeb/lorelei/data/kb/LDC2017E19_LORELEI_EDL_Knowledge_Base_V0.1/";
        String kbpath = dir + "data/entities.tab";

        String indexpath = "/tmp/kb-lucene/";

        //buildindex(kbpath, indexpath);
        testindex(indexpath);
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

        List<String> kblines = LineIO.read(kbpath);

        String header = kblines.get(0);
        String[] headerfields = header.split("\t");

        int j = 0;
        for(String line : kblines){

            if(j % 1000 == 0){
                System.out.println("Progress: " + j / (float) kblines.size());
            }
            j++;

            if(line.equals(header)) continue;

            String[] sline = line.split("\t");

            Document d = new Document();

            for(int i = 0; i < sline.length; i++){
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
                }catch (NumberFormatException e){
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

        writer.close();
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
