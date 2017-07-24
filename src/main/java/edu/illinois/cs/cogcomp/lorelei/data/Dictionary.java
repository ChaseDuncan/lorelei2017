package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.LineIO;
//import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import edu.illinois.cs.cogcomp.transliteration.SPModel;
import edu.illinois.cs.cogcomp.utils.TopList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by mayhew2 on 6/22/17.
 */
@SuppressWarnings("ALL")
public class Dictionary {

    HashMap<String, List<String>> entries = new HashMap<>();

    //String[] suffixes = {"نىڭ","دىن", "دىكى", "نى", "ىنى", "تىكى"}; //{"ning", "diki", "din", "da", "ni", "mu", "gha", "tiki"};
    String[] suffixes = {"ning", "diki", "din", "da", "ni", "mu", "gha", "tiki", "lar", "ler", "lik", "qan", "larda", "ghan", "chi", "liri", "ni", "liqi", "ge", "ta"};

    HashMap<String, List<String>> index2 = new HashMap<>();
    HashMap<String, List<String>> index3 = new HashMap<>();
    HashMap<String, List<String>> index4 = new HashMap<>();

    HashMap<String, String> confidentdict = new HashMap<>();

    static final String ugrevmodel = "/shared/corpora/transliteration/lorelei/models/probs-ug-rev.txt";
    static final String ugmodel = "/shared/corpora/transliteration/lorelei/models/probs-ug.txt";

    private HashSet<String> stopwords = new HashSet<>();

    public Dictionary(String fname) throws IOException {

        List<String> lines = LineIO.read(fname);

        String word = "";
        for(String line : lines){
            String text = line.replaceAll("\\<[^>]*>","").trim().toLowerCase();
            if(line.contains("<LEMMA") || line.contains("<WORD")){
                if(!entries.containsKey(text)) {
                    entries.put(text, new ArrayList<String>());
                    word = text;
                    this.addToIndices(text);
                }

            }else if(line.contains("<GLOSS")){
                entries.get(word).add(text.toLowerCase());
            }
        }
        //System.out.println(entries.keySet());

        List<String> wwlines = new ArrayList<>();

        int wordword = 0;
        for(String entry : entries.keySet()){
            List<String> def = entries.get(entry);
            if(entry.split(" ").length == 1 && def.get(0).split(" ").length == 1){
                wordword++;
                wwlines.add(entry + "\t" + def.get(0));
            }else if(entry.split(" ").length == 2 &&
                    def.get(0).split(" ").length == 2 &&
                    !def.get(0).contains(",") &&
                    !def.get(0).startsWith("to ")){
                wordword++;

                for(int i = 0; i < 2; i ++) {
                    wwlines.add(entry.split(" ")[i] + "\t" + def.get(0).split(" ")[i]);
                }
            }
        }

        System.out.println("There are " + wordword + " entries out of " + entries.size());

    }

    private void addToIndices(String entry) {
        List<String> bigrams = getNgrams(entry, 2);
        List<String> trigrams = getNgrams(entry, 3);
        List<String> quadrigrams = getNgrams(entry, 4);

        for(String bi : bigrams){
            if(!index2.containsKey(bi)){
                index2.put(bi, new ArrayList<String>());
            }

            index2.get(bi).add(entry);
        }

        for(String tri : trigrams){
            if(!index3.containsKey(tri)){
                index3.put(tri, new ArrayList<String>());
            }
            index3.get(tri).add(entry);
        }

        for(String q : quadrigrams){
            if(!index4.containsKey(q)){
                index4.put(q, new ArrayList<String>());
            }
            index4.get(q).add(entry);
        }
    }

    /**
     * This Writes out to masterlex format.
     * @param outfname
     * @throws IOException
     */
    public void write(String outfname) throws IOException {
        List<String> outlines = new ArrayList<>();
        for(String entry : entries.keySet()){
            List<String> defs = entries.get(entry);
            for(String def : defs){

                def = def.trim().replaceAll("\\.$", "");
                def = def.trim().replaceAll("1$", "");
                def = def.replaceAll("\\(.*\\)", "");
                def = def.replaceAll("\\[.*\\]", "");
                def = def.replaceAll("«.*»", "").trim();


                String[] commadefs = { def };
                if(def.contains(",")) {
                    commadefs = def.split(",");
                }else if(def.contains(";")){
                    commadefs = def.split(";");
                }

                for(String cd : commadefs) {
                    cd = cd.trim().replaceAll("\\.$", "");
                    cd = cd.replaceAll("\\(.*\\)", "");
                    cd = cd.replaceAll("\\[.*\\]", "");
                    cd = cd.replaceAll("«.*»", "");
                    if(cd.trim().length() > 0) {
                        outlines.add(entry + "\tN/A\tN/A\tN/A\tN/A\t" + cd.trim() + "\tN/A\tN/A\tN/A\tloreleidict\tN/A");
                    }
                }
            }
        }

        LineIO.write(outfname, outlines);
    }


    /**
     * Given a string, and an integer, this returns all character ngrams
     * of that string.
     * @param s
     * @param n
     * @return
     */
    private List<String> getNgrams(String s, int n){
        List<String> ret = new ArrayList<>();

        if(s.length() < n){
            return ret;
        }

        for(int i = 0; i < s.length()-n+1; i++){
            ret.add(s.substring(i, i+n));
        }
        return ret;
    }

    public String indexlookup(String w, double ratio){
        return pairlookup(w, ratio).getSecond();
    }

    public String indexlookup(String w){
        return pairlookup(w).getSecond();
    }

    public Pair<String,String> pairlookup(String w){
        return pairlookup(w, 0.9);
    }

    public Pair<String,String> pairlookup(String w, double ratio){

        if(entries.containsKey(w)){
            return new Pair<>(w, entries.get(w).get(0));
        }

        for(String suffix : suffixes){
            if(w.endsWith(suffix)){
                String ws = StringUtils.removeEnd(w, suffix);
                return pairlookup(ws, ratio);
            }
        }


        List<String> all = new ArrayList<>();

        for(String b : getNgrams(w, 2)){
            if(index2.containsKey(b)) {
                all.addAll(index2.get(b));
            }
        }

        List<String> trigrams = getNgrams(w, 3);
        for(String b : trigrams){
            if(index3.containsKey(b)) {
                all.addAll(index3.get(b));
            }
        }

        List<String> quadrigrams = getNgrams(w, 4);
        for(String b : quadrigrams){
            if(index4.containsKey(b)) {
                all.addAll(index4.get(b));
            }
        }

        // order all by size --> shorter at the beginning.
        // this means that the first element in the list to
        // beat themax wins. This will bias towards shorter elements.
        Collections.sort(all, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.length() - o2.length();
            }
        });

        HashMap<String, Integer> freqs = new HashMap<>();

        for(String entry : all){
            if(!freqs.containsKey(entry)){
                freqs.put(entry, 0);
            }

            freqs.put(entry, freqs.get(entry)+1);
        }

        double max = 0;
        String best = null;

        for(String entry : freqs.keySet()){
            //double score = freqs.get(entry) / (float)entry.length();
            if(freqs.get(entry) > 1) {
                double score = freqs.get(entry) / LevensteinDistance.getLevensteinDistance(entry, w);
                if (score > max) {
                    max = score;
                    best = entry;
                }
            }
        }

        //double threshold =ratio*(trigrams.size() + quadrigrams.size()) / w.length();
        double threshold = ratio * w.length();

        //System.out.println(max);
        if(best != null && max > threshold) {
            return new Pair<>(best, entries.get(best).get(0));
        }else{
            return new Pair<>(null, null);
        }
    }


    /**
     * This is entirely in English. The goal is to return a single word.
     * @param def
     * @return
     */
    public String cleandefinition(String def){
        if(def == null){
            return null;
        }

        def = def.trim();

        //System.out.println(def);

        def = def.replaceAll("\\s*\\(.*\\)\\s*", "");
        def = def.replaceAll("\\s*\\[.*\\]\\s*", "");
        def = def.replaceAll("\\s*«.*»\\s*", "");
        def = def.replaceAll("\\.", "");
        def = def.split("[;,]")[0];

        for(String stopword : stopwords){
            def = def.replaceAll(" " + stopword + " ", " ");
            def = def.replaceAll("^" + stopword + " ", " ");
            def = def.replaceAll(" " + stopword + "$", " ");
        }

        def = def.trim();

        return def;
    }

    public static void main(String[] args) throws Exception {

        String dir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/";
        //String fname = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E87_LORELEI_Amharic_Representative_Language_Pack_Translation_Annotation_Grammar_Lexicon_and_Tools_V1.0/data/lexicon/amh_lexicon.v1.0.llf.xml";
        //String fname = dir + "BOLT_Turkish_RL_LDC2014E115_V2.1/data/lexicon/lexicon.llf.xml";
        String fname = dir + "REFLEX_Yoruba_LDC2015E91_V1.1/data/lexicon/Yoruba_Lexicon_without_tone.llf.xml";

        Dictionary d = new Dictionary(fname);

        d.write("/shared/experiments/mayhew2/lexicons-v2/yor-eng.lorelei.txt");



    }
}