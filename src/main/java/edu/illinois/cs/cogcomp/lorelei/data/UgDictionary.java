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
 * TODO: rewrite this to be a little less specific...
 *
 * TODO: consider using Lucene as the backend.
 *
 *
 * Created by mayhew2 on 7/7/16.
 */
public class UgDictionary {

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

    public UgDictionary(String fname) throws IOException {

        String dictdir = FilenameUtils.getPath(fname);
        List<String> conflines = LineIO.read("/" + dictdir + "/confident-dictionary.txt");
        for(String line : conflines){
            String[] sline = line.split("\t");
            if(sline.length == 2) {
                confidentdict.put(sline[0], sline[1]);
            }
        }

        List<String> lines = LineIO.read(fname);

        String word = "";
        for(String line : lines){
            String text = line.replaceAll("\\<[^>]*>","").trim();
            if(line.contains("<WORD")){
                if(!entries.containsKey(text)) {
                    entries.put(text, new ArrayList<String>());
                    word = text;
                    this.addToIndices(text);
                }

            }else if(line.contains("<DEFINITION")){
                entries.get(word).add(text);
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

        LineIO.write("ug-en.txt", wwlines);

        System.out.println("There are " + wordword + " entries out of " + entries.size());

        //List<String> stoplines = LineIO.readFromClasspath("stopwords.txt");
        //for(String stopline : stoplines){
        //    stopwords.add(stopline.trim());
       // }

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
        // beat the max wins. This will bias towards shorter elements.
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


//        if(def.startsWith("form of ")){
//            def = def.replace("form of ", "").trim();
//            def = cleandefinition(indexlookup(def));
//        }

//        if(def.startsWith("see ")){
//            // stupid self-loops in the dictionary...
//            String orig = def;
//            def = def.replace("see ", "").trim();
//            if(orig.equals(def)){
//                return def;
//            }
//            def = cleandefinition(indexlookup(def));
//        }

        return def;
    }


    public void processWikidata(String fname, String outfname) throws Exception {
        SPModel model = new SPModel(ugrevmodel);
        model.setMaxCandidates(1);

        List<String> lines = LineIO.read(fname);
        List<String> outlines = new ArrayList<>();

        for(String line : lines){
            String[] sline = line.split("\t");

            String name = sline[0];
            String[] sname = name.split("\\s+");

            String outname = "";

            for(String sn : sname){
                TopList<Double, String> cands = model.Generate(sn);
                if (cands.size() > 0) {
                    sn = cands.getFirst().getSecond();
                } else {
                    // don't do anything.
                }
                outname += sn + " ";
            }

            sline[0] = outname.trim();

            outlines.add(StringUtils.join(sline, "\t"));
        }

        LineIO.write(outfname, outlines);


    }

    public void processConll(String dir, String outdir) throws Exception {

        String[] fnames = (new File(dir)).list();
        SPModel model = new SPModel(ugmodel);
        model.setMaxCandidates(1);

        int translated = 0;
        int otherwise = 0;

        HashMap<String, String> memo = new HashMap<>();

        int i = 0;
        for(String fname : fnames) {
            List<String> lines = LineIO.read(dir + "/" + fname);
            if(i%10 == 0){
                System.out.println("On " + i + " out of " + fnames.length);
                System.out.println(translated + ", " + otherwise);
            }

            i++;

            List<String> outlines = new ArrayList<>();

            for (String line : lines) {
                String[] sline = line.split("\\s+");

                if (sline.length < 5) {
                    outlines.add("");
                    continue;
                }

                if(sline[5].length() > 1) {

                    String res = null;
//                    if(confidentdict.containsKey(sline[5])){
//                        sline[5] = confidentdict.get(sline[5]);
//                    }else {
//                        // get it from the dictionary
//                        res = this.indexlookup(sline[5], 0.7);
//                    }

                    if (res != null) {
                        sline[5] = sline[5]; // + ":" + cleandefinition(res);
                        translated++;

                    } else {

                        otherwise++;
                        if (memo.containsKey(sline[5])) {
                            sline[5] = memo.get(sline[5]);

                        } else {
                            String orig = sline[5];
                            TopList<Double, String> cands = model.Generate(sline[5]);
                            if (cands.size() > 0) {
                                sline[5] = cands.getFirst().getSecond();
                            } else {
                                // don't do anything.
                            }
                            memo.put(orig, sline[5]);
                        }

                    }

                    sline[5] = sline[5].replace(" ", "_");
                }

                outlines.add(StringUtils.join(sline, "\t"));
            }

            LineIO.write(outdir + "/" + fname, outlines);

        }
        System.out.println("Total: " + (translated+otherwise));
        System.out.println("Translated: " + translated);
        System.out.println("Transliterated+ignored: " + otherwise);

    }


    public void processDictionary() throws Exception{

        String dict = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/docs/categoryI_dictionary/IL3_dictionary.xml";

        SPModel model = new SPModel(ugrevmodel);
        model.setMaxCandidates(1);

        List<String> lines = LineIO.read(dict);
        List<String> outlines = new ArrayList<>();
        for(String line : lines){
            if(line.contains("</WORD>")){
                String text = line.replaceAll("\\<[^>]*>","");
                String[] stext = text.split("\\s+");
                String newst = "";
                for(String st : stext){
                    TopList<Double, String> cands = model.Generate(st);
                    if (cands.size() > 0) {
                        st = cands.getFirst().getSecond();
                    } else {
                        // don't do anything.
                    }
                    newst += st + " ";
                }
                newst = "    <WORD>" + newst.trim() + "</WORD>";
                outlines.add(newst);
            }else{
                outlines.add(line);
            }
        }

        LineIO.write(dict + ".ULY", outlines);
    }

    public void processTaFolder(String folder, String outfolder) throws Exception {

        File f = new File(folder);

        SPModel model = new SPModel(ugrevmodel);
        model.setMaxCandidates(1);

        String[] files = f.list();

        for(int i = 0; i < files.length; i++){
            System.out.println(files[i]);
            String file = files[i];
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(folder + "/" + file);

            String tokstring = "";

            for(Sentence sent : ta.sentences()){
                for(String tok : sent.getTokens()){
                    TopList<Double, String> cands = model.Generate(tok);
                    if (cands.size() > 0) {
                        tok = cands.getFirst().getSecond();
                    } else {
                        // don't do anything.
                    }
                    tokstring += tok + " ";
                }
            }

            TextAnnotation fornta = TextAnnotationUtilities.createFromTokenizedString(tokstring);
            View view = ta.getView(ViewNames.NER_CONLL);
            fornta.addView(ViewNames.NER_CONLL, view);
            SerializationHelper.serializeTextAnnotationToFile(fornta, outfolder + "/" + file, true);
        }
    }


    public void transferAnnotations(String origpath, String ulypath, String outfolder) throws Exception {
        File f = new File(ulypath);

        String[] files = f.list();

        for(int i = 0; i < files.length; i++){
            System.out.println(files[i]);
            String file = files[i];
            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(ulypath + "/" + file);
            View view = ta.getView("NER_CONLL");

            TextAnnotation origta = SerializationHelper.deserializeTextAnnotationFromFile(origpath + "/" + file);
            TextAnnotation fornta = TextAnnotationUtilities.createFromTokenizedString(origta.getTokenizedText());
            fornta.addView(ViewNames.NER_CONLL, view);

            SerializationHelper.serializeTextAnnotationToFile(fornta, outfolder + "/" + file, true);
        }
    }


    public void processbrown(String brownfile) throws Exception {
        SPModel model = new SPModel(ugrevmodel);
        model.setMaxCandidates(1);

        List<String> lines = LineIO.read(brownfile);

        List<String> outlines = new ArrayList<>();

        for(String line : lines){
            String[] sline = line.split("\t");
            TopList<Double, String> cands = model.Generate(sline[1]);
            if (cands.size() > 0) {
                sline[1] = cands.getFirst().getSecond();
            } else {
                // don't do anything.
            }
            outlines.add(StringUtils.join(sline, "\t"));

        }

        LineIO.write(brownfile + ".uly", outlines);
    }

    public void processtextfile(String file) throws Exception {
        SPModel model = new SPModel(ugmodel);
        model.setMaxCandidates(1);

        List<String> lines = LineIO.read(file);

        List<String> outlines = new ArrayList<>();

        for(String line : lines){
            String[] sline = line.split(" ");
            String outline = "";
            for(String tok : sline) {
                TopList<Double, String> cands = model.Generate(tok);
                if (cands.size() > 0) {
                    outline += cands.getFirst().getSecond() + " ";
                } else {
                    outline += tok + " ";
                }
            }
            outlines.add(outline);
        }

        LineIO.write(file + ".translit", outlines);
    }

    public static void processMasterlex(String file) throws Exception {
        SPModel model = new SPModel(ugrevmodel);
        model.setMaxCandidates(1);

        List<String> lines = LineIO.read(file);

        List<String> outlines = new ArrayList<>();

        for(String line : lines){
            String[] sline = line.trim().split("\t");

            String ug = sline[0];

            String[] toks = ug.split(" ");
            String outphrase = "";
            for(String tok : toks) {
                TopList<Double, String> cands = model.Generate(tok);
                if (cands.size() > 0) {
                    outphrase += cands.getFirst().getSecond() + " ";
                } else {
                    outphrase += tok + " ";
                }
            }

            outphrase = outphrase.trim();

            sline[0] = outphrase;

            outlines.add(StringUtils.join(sline, "\t"));
        }

        LineIO.write(file + ".uly", outlines);
    }


    public static void main(String[] args) throws Exception {

        UgDictionary d = new UgDictionary("/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/docs/categoryI_dictionary/IL3_dictionary.xml");

//        String res = d.indexlookup("shamoliy", 0.1);
//        System.out.println(res);

//        d.write("/shared/experiments/mayhew2/lexicons/uig-eng.lorelei.txt");

        d.processConll("/shared/corpora/ner/lorelei/ug/All-stem-uly/", "/shared/corpora/ner/lorelei/ug/All-stem-best/");
//        String dir = "/shared/corpora/ner/eval/column/";
        //dir = "/shared/corpora/ner/wikifier-features/ug/";
        //String dir = "/shared/corpora/ner/human/ug/";
        //dir = "/home/mayhew2/software/upparse/";

        //d.processConll(dir + "Train", dir + "Train-translit");

//        d.processtextfile(dir + "mono-all.txt");
        //d.processbrown("/shared/experiments/ctsai12/workspace/brown-cluster/combine-c1000-min5/paths");
        //d.processDictionary();

//        UgDictionary.processMasterlex("/shared/experiments/mayhew2/lexicons/uig-eng.masterlex.txt");

        //d.processTaFolder("/shared/corpora/ner/eval/ta/set0-NW1-ann6-gaz-annotation-NI/","/shared/corpora/ner/eval/ta/set0-NW1-ann6-gaz-annotation-NI-uly/");
        //d.transferAnnotations(dir + "ta/set0-NW1-ann6-gaz-annotation-NI/",dir + "ta/set0-NW1-ann6-gaz-annotation-NI-uly-annotation-swm2/", dir + "ta/set0-NW1-ann6-gaz-annotation-NI-uly-annotation-swm-fixed-noslf/");

        //d.processWikidata("/shared/corpora/transliteration/wikidata/wikidata.Uyghur", "wikidata.ULY");

//        System.out.println(d.indexlookup("ئونىۋېرسىتېتى"));


    }
}
