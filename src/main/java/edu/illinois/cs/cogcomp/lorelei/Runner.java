package edu.illinois.cs.cogcomp.ner;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.*;
import java.lang.String;
import java.util.*;

import edu.illinois.cs.cogcomp.transliteration.Example;
import edu.illinois.cs.cogcomp.transliteration.SPModel;
import edu.illinois.cs.cogcomp.utils.TopList;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

class Runner {

    private static final String basedir = "/home/mayhew2/Downloads/stanford-segmenter-2015-04-20/data/";

    static String dataPath ="/shared/corpora/corporaWeb/tac/LDC2015E75_TAC_KBP_2015_Tri-Lingual_Entity_Discovery_and_Linking_Training_Data/data/";

    static String xmldata = dataPath + "source_docs/cmn/xml/";
    static String golddata = dataPath + "tac_kbp_2015_tedl_training_gold_standard_entity_mentions.tab";

    static String shyamTrainChinese = "/shared/corpora/transliteration/chinese/same.size.zh";

    static String chineseTrainFile = "en2zh_train.txt";
    static String chineseTestFile = "en2zh_test.txt";

    public static void main(String[] args) throws IOException {

        //Pair<List<Example>, List<Example>> p = GetChineseData();
        Pair<List<Example>, List<Example>> p = GetTamilData();


        List<Example> training = p.getFirst();
        List<Example> testing = p.getSecond();

        //List<String> candlist = LineIO.read("chineseCandidates.txt");
        HashSet<String> candidates = new HashSet<>();
        //candidates.addAll(candlist);

        int emiterations = 10;
//        SPModel model = Train(training, emiterations);
//        TestDiscovery(model, testing, candidates);

    }

    /**
     * This reads entity mentions from EDL in Chinese, gets the English equivalent from Freebase, and writes the results to file.
     * @throws IOException
     */
    static void WriteChineseGoldData() throws IOException {
        System.out.println("==== Reading the Gold Data to get Chinese-English Pairs ====");
        ArrayList<String> lines = LineIO.read(golddata);

        HashSet<String> englishNames = new HashSet<>();
        HashSet<String> dupes = new HashSet<>();

        HashMap<String, String> en2cn = new HashMap<>();

        QueryMQL q = new QueryMQL();

        int count = 0;

        for(String line : lines)
        {
            String[] sline = line.split("\t");
            String doc = sline[3];
            String freebaseId = sline[4];
            String ent = sline[5];

            if(sline[3].startsWith("CMN") && sline[4].startsWith("m.") && sline[5].equals("PER")) {
                count++;

                dupes.add(doc.split(":")[0] + freebaseId + ent);

                String n =q.lookupNameFromMid(freebaseId);
                if(n != null) {
                    englishNames.add(n);
                    en2cn.put(n, sline[2]);
                }
            }
        }
        System.out.println(dupes.size());
        System.out.println(englishNames.size());
        System.out.println(count);
        System.out.println(en2cn);

        ArrayList<String> outlines_train = new ArrayList<>();
        ArrayList<String> outlines_test = new ArrayList<>();
        Random r =new Random();
        for(String en : en2cn.keySet()){
            if(r.nextDouble() < 0.5) {
                outlines_train.add(en + "\t" + en2cn.get(en));
            }else{
                outlines_test.add(en + "\t" + en2cn.get(en));
            }
        }

        LineIO.write(chineseTrainFile, outlines_train);
        LineIO.write(chineseTestFile, outlines_test);
    }

    /**
     * This reads Chinese source docs from EDL and segments them to get candidates.
     * @throws IOException
     */
    static void segment() throws IOException {

        System.out.println("==== Segmenting Chinese to gather candidates ====");

        System.setOut(new PrintStream(System.out, true, "utf-8"));

        Properties props = new Properties();
        props.setProperty("sighanCorporaDict", basedir);
        props.setProperty("serDictionary", basedir + "/dict-chris6.ser.gz");
        props.setProperty("inputEncoding", "UTF-8");
        props.setProperty("sighanPostProcessing", "true");

        CRFClassifier<CoreLabel> segmenter = new CRFClassifier<>(props);
        segmenter.loadClassifierNoExceptions(basedir + "/ctb.gz", props);

        File[] files = new File(xmldata).listFiles();
        HashSet<String> candidates = new HashSet<>();
        for (File f : files) {
            ArrayList<String> lines = LineIO.read(f.getAbsolutePath());
            for (String line : lines) {
                if (line.startsWith("<")) {
                    continue;
                }
                // TODO: clean punctuation and short words.

                List<String> segments = segmenter.segmentString(line);
                for (String seg : segments) {
                    if (seg.length() == 0 || seg.startsWith("//") || seg.contains(".jpg") || seg.contains(".png")
                            || seg.contains("_relations") || seg.contains(".gif") || seg.contains(".com")
                            || seg.contains(".html") || StringUtils.isAlpha(seg) || StringUtils.isAlphanumeric(seg) || seg.contains("/") || seg.contains("amp")
                            || seg.contains("-") || seg.contains("_") || seg.contains("=") || seg.contains("a") || seg.contains("?") || seg.contains("&")) {
                        //System.out.println("Skip this one: " + seg);
                    } else {
                        candidates.add(seg);
                    }
                }
            }
        }

        LineIO.write("chineseCandidates.txt", candidates);
    }


    public static Pair<List<Example>, List<Example>> GetTamilData() throws FileNotFoundException {

        List<Example> training = new ArrayList<>();
        List<Example> testing = new ArrayList<>();

        // File format is tamil -> english
        List<String> lines = LineIO.read("/shared/corpora/transliteration/tamil/wikidata.ta");
        Random r = new Random();
        for(String line : lines)
        {
            String[] parts = line.split("\t");

            String tamil = parts[0];
            String english = parts[1].toLowerCase();

            if(StringUtils.isAlpha(english)){

                String[] tamils = tamil.split(" ");
                String[] englishs = english.split(" ");

                if(tamils.length != englishs.length){
                    System.err.println("Lengths do not match! " + english + ", " + tamil);
                    continue;
                }

                for(int i = 0; i < tamils.length; i++) {
                    Example e = new Example(englishs[i], tamils[i]);

                    if (r.nextDouble() < 0.75) {
                        training.add(e);
                    } else {
                        testing.add(e);
                    }
                }
            }else{
                System.out.println("Skipping: " + line.trim());
            }



        }

        Pair<List<Example>, List<Example>> p = new Pair<>(training, testing);

        return p;
    }

    /**
     * This reads the appropriate files and gets training and testing examples for Chinese.
     * @return a pair, first element is training list, second is testing list.
     * @throws FileNotFoundException
     */
    public static Pair<List<Example>, List<Example>> GetChineseData() throws FileNotFoundException {

        List<Example> training = new ArrayList<>();
        List<Example> testing = new ArrayList<>();

        ArrayList<String> zhlines = LineIO.read("Data/zhExamples.txt");

        Random r = new Random();
        for(String line : zhlines){
            String[] parts = line.split("\t");
            Example e = new Example(parts[0], parts[1]);

            if(r.nextDouble() < 0.75){
                training.add(e);
            }else{
                testing.add(e);
            }
        }

        // NOTICE: This file is Chinese first, then English. Pairs are backwards!
        List<String> extras = LineIO.read(shyamTrainChinese);
        for(String line : extras)
        {
            String[] parts = line.split("\t");
            training.add(new Example(parts[1], parts[0]));
        }

        Pair<List<Example>, List<Example>> p = new Pair<>(training, testing);

        return p;
    }

    /**
     * This takes a set of data and trains a model on it, returns the model.
     * @param training
     * @return
     * @throws IOException
     */
    public static SPModel Train(List<Example> training, int emiterations) throws IOException {

        System.out.println("Training examples: " + training.size());

        java.util.Collections.shuffle(training);

        SPModel model = new SPModel(training);
        model.Train(emiterations, false, null);

        return model;

    }

//    public static void TestGenerate(SPModel model, List<Example> testing) {
//        double correctmrr = 0;
//        double correctacc = 0;
//        for (Example example : testing) {
//            int index = (model.Generate(example.sourceWord).indexOf(example.transliteratedWord));
//            if (index >= 0) {
//                correctmrr += 1.0 / (index + 1);
//                if(index == 0){
//                    correctacc += 1.0;
//                }
//            }
//
//            System.out.println();
//        }
//        System.out.println("MRR=" + correctmrr / testing.size());
//        System.out.println("ACC=" + correctacc / testing.size());
//    }

//    public static void TestDiscovery(SPModel model, List<Example> testing, HashSet<String> possibilities) throws IOException {
//
//        System.out.println("Testing examples: " + testing.size());
//
//        double correctmrr = 0;
//        double correctacc = 0;
//
//        for(Example e : testing){
//            possibilities.add(e.transliteratedWord);
//        }
//
//        List<String> outlines = new ArrayList<>();
//
//        for (Example example : testing) {
//
//
//            int topK = 10;
//            TopList<Double, String> ll = new TopList<>(topK);
//            for(String target : possibilities){
//                try {
//                    double prob = model.Probability(example.sourceWord, target);
//                    ll.add(prob, target);
//                }catch(ArrayIndexOutOfBoundsException e){
//                    System.out.println("Oops.. target=" + target);
//                }
//            }
//
//            outlines.add(example.sourceWord);
//            for(Pair<Double, String> p : ll){
//                String s = p.getSecond();
//
//                if(s.equals(example.transliteratedWord)){
//                    s = "**" + s + "**";
//                }
//
//                outlines.add(s);
//            }
//            outlines.add("");
//
//            int index = ll.indexOf(example.transliteratedWord);
//            if (index >= 0) {
//                correctmrr += 1.0 / (index + 1);
//                if(index == 0){
//                    correctacc += 1.0;
//                }
//            }
//        }
//
//        try {
//            LineIO.write("out.txt", outlines);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        model.WriteProbs("probs.txt", 0.001);
//
//        System.out.println("MRR=" + correctmrr / (double)testing.size());
//        System.out.println("ACC=" + correctacc / (double)testing.size());
//    }

}

