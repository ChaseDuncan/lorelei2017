package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.classify.Classifier;
import edu.illinois.cs.cogcomp.lbjava.classify.FeatureVector;
import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.lbjava.learn.BatchTrainer;
import edu.illinois.cs.cogcomp.lorelei.WordPair;
import edu.illinois.cs.cogcomp.lorelei.WordPairReader;
import edu.illinois.cs.cogcomp.lorelei.lbjava.NamePairClassifier;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.nlp.util.SimpleCachingPipeline;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

import static edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader.conllline;

/**
 *
 * This class is the runner that converts a parallel set of files into conll format by annotating
 * English and learning a classifier that decides which candidate should be aligned.
 *
 * Created by mayhew2 on 4/5/16.
 */
public class ParallelToConll {
    private static Logger logger = LoggerFactory.getLogger(ParallelToConll.class);

    private static String GPEVIEW = "GPE";

    /**
     * Converts a list of annotated TextAnnotation into ConLL format. Must have NER_CONLL view.
     * @param tas
     * @param outfilename
     * @throws IOException
     */
    public static void TaToConll(List<TextAnnotation> tas, String outfilename, String viewname) throws IOException {

        //logger.warn("NOTICE: this is omitting sentences with no entities!!!");

        List<String> outlines = new ArrayList<>();

        for(TextAnnotation ta : tas) {

            List<String> talines = new ArrayList<>();

            View nerview = ta.getView(viewname);
            for (int i = 0; i < ta.getTokens().length; i++) {

                String label = "O";

                List<Constituent> constituents = nerview.getConstituentsCoveringToken(i);
                // should be just one constituent

                if(constituents.size() > 0) {
                    Constituent c = constituents.get(0);
                    if (c.getStartSpan() == i) {
                        label = "B-" + c.getLabel();
                    } else {
                        label = "I-" + c.getLabel();
                    }

                    if (constituents.size() > 1) {
                        logger.error("More than one label -- selecting the first.");
                        logger.error("Constituents: " + constituents);
                    }
                }
                talines.add(conllline(label, i, ta.getToken(i)));
            }


            if(nerview.getConstituents().size() > 0) {
                outlines.addAll(talines);
                outlines.add("");
            }
        }
        LineIO.write(outfilename, outlines);
    }



    /**
     * This takes as input two parallel text files (eng.txt, and lang.txt), which must have the same
     * number of lines, and where corresponding lines in each file are parallel. This also can read
     * from a file called map.txt, in which each line is a document+segment id corresponding to the lines
     * in eng.txt and lang.txt.
     *
     *  {@code datadir} and {@code lang} specify where to find the files.
     *
     *  This creates a file called candlines.txt, which has the format (tab-sep):
     *  fileid label, Eng span, Eng word, (forn span, forn word)+
     *
     *  Eng word is a phrase recognized by the NER, and forn word is a possibility.
     *  Typically this has the same length, and has been slightly cleaned (e.g. no punctuation)
     *
     *
     * @param lang
     * @param datadir
     * @throws IOException
     * @throws AnnotatorException
     */
    public static void getCandidates(String lang, String datadir, String viewname) throws IOException, AnnotatorException {
        String docId = "NOTHING"; // arbitrary string identifier
        String textId = "body"; // arbitrary string identifier

        // these all have exactly the same number of lines.
        String fname = datadir + "/" + lang + "/eng.txt";
        List<String> eng = Files.readAllLines(new File(fname).toPath(), Charset.defaultCharset());
        List<String> forn = LineIO.read(datadir + "/" + lang + "/" + lang + ".txt");

        List<String> map = null;
        try{
            map = LineIO.read(datadir + "/" + lang + "/map.txt");
        }
        catch(IOException e){
            // it's ok. just map=null
        }

        if(eng.size() != forn.size()){
            logger.error("Size of eng (" + eng.size() +") doesn't match size of forn (" + forn.size() + ")");
            return;
        }

        // Load the illinois-nlp-pipeline, so we can use NER.
        ResourceManager rm = new ResourceManager( "config/pipeline-config.properties" );
        SimpleCachingPipeline pipeline = (SimpleCachingPipeline) IllinoisPipelineFactory.buildPipeline( rm );

//        IllinoisNerHandler inh = new IllinoisNerHandler(new ResourceManager("config/eval.config"), GPEVIEW);
//        pipeline.addAnnotator(inh);

        // just take the first <limit> lines
        int limit = Integer.MAX_VALUE;
        int looplimit = Math.min(limit, eng.size());

        List<String> candlines = new ArrayList<>();

        // Main loop. This loops over lines in parallel text files.
        for(int i = 0; i < looplimit; i++){
            if (i%100 == 0){
                logger.debug(i + "/" + looplimit);
            }
            String engline = eng.get(i);
            String fornline = forn.get(i);

            if(engline.trim().length() == 0 || fornline.trim().length() == 0){
                continue;
            }

            String mapline = "";
            if(map !=null){
                mapline = map.get(i);

            }

            // Annotate the English ta with NER.
            TextAnnotation ta = pipeline.createAnnotatedTextAnnotation( docId, textId, engline );

            View view = ta.getView( viewname );
            List<Constituent> constituents = view.getConstituents();

            String[] forntoks = fornline.split(" ");

            // loop through all constituents in english tagged text. This is a nested
            // loop -- the next loop goes over candidates, and attempts to match them up.
            for(Constituent c : constituents){
                String label = c.getLabel();
                int min_n = c.size();
                int max_n = c.size();

                // Organizations need special treatment...
//                if(label.equals("ORG")){
//                    min_n = Math.max(c.size() - 1, 1);
//                    max_n = c.size() + 1;
//                }

                //ignore misc label.
                if(label.equals("MISC")) continue;

                StringBuilder candline = new StringBuilder();
                candline.append(mapline);
                candline.append("\t" + c.getLabel());
                candline.append("\t" + c.getSpan());
                candline.append("\t" + c.getTokenizedSurfaceForm());

                for(int length = min_n; length<= max_n; length++ ) {
                    for (int m = 0; m <= forntoks.length - length; m++) {

                        String[] ngram = Arrays.copyOfRange(forntoks, m, m + length);

                        boolean punccand = false;
                        for (String ng : ngram) {
                            if (Pattern.matches("\\p{Punct}+", ng)) {
                                punccand = true;
                                break;
                            }
                        }

                        // if it's just punc... ignore it.. not a candidate.
                        if (!punccand) {
                            candline.append("\t" + String.format("(%d, %d)", m, m + length));
                            candline.append("\t" + StringUtils.join(ngram, " "));
                        }
                    }
                }
                candlines.add(candline.toString());
            }

        }

        logger.info("writing candlines.txt to dir: " + datadir + lang);
        LineIO.write(datadir + lang + "/candlines.txt", candlines);

    }

    /**
     * This gets results using features defined in NamePairClassifier.lbj
     *
     * This takes candlines, and creates edittrain.
     *
     */
    public static void applyUniform(String lang, String datadir, WordPairReader wpr) throws IOException {
        NamePairClassifier npc = new NamePairClassifier();

        HashMap<String, WordPair> bestwp = new HashMap<>();
        HashMap<String, Double> bestscore = new HashMap<>();

        WordPair wp;
        while((wp = (WordPair)wpr.next()) != null){

            Classifier extractor = npc.getExtractor();
            FeatureVector fv = extractor.classify(wp);

            double sum = 0;
            for(double d : fv.realValueArray()){
                sum += d;
            }

            if(!bestscore.containsKey(wp.getKey())){
                bestscore.put(wp.getKey(), sum);
                bestwp.put(wp.getKey(), wp);
            }else{

                if(sum > bestscore.get(wp.getKey())){
                    bestscore.put(wp.getKey(), sum);
                    bestwp.put(wp.getKey(), wp);
                }
            }

        }

        List<String> newTraining = new ArrayList<>();

        List<WordPair> sortedwp = new ArrayList<>(bestwp.values());
        Collections.sort(sortedwp, new Comparator<WordPair>() {
            @Override
            public int compare(WordPair o1, WordPair o2) {
                return o1.fileid.compareTo(o2.fileid);
            }
        });

        for(WordPair bwp : sortedwp){

//            if(bestscore.get(bwp.getKey()) < 0.5){
//                continue;
//            }

            List<String> trainline = new ArrayList<>();
            trainline.add(bwp.fileid);
            trainline.add(bwp.reflabel);
            trainline.add(bwp.refspan.toString());
            trainline.add(bwp.refword);
            trainline.add(bwp.span + "");
            trainline.add(bwp.word);

            newTraining.add(StringUtils.join(trainline, "\t"));
        }
        LineIO.write(datadir + lang + "/edittrain.txt", newTraining);
    }


    /**
     * This will train the NamePairClassifier
     */
    public static void trainClassifier(WordPairReader wpr){

        if(!wpr.islabeled){
            logger.error("The WordPairReader needs to be labeled (use wpr.labelPairs(...))");
            System.exit(-1);
        }

        NamePairClassifier npc = new NamePairClassifier();
        BatchTrainer bt = new BatchTrainer(npc, wpr);
        bt.train(5);

        npc.save();
    }


    /**
     * This takes the candlines.txt and condprob.txt files and selects the best
     * candidate from candlines. It also keeps only those above some arbitrary threshold
     * and saves them to traininglines2.txt
     *
     * The output is alllabeled.txt.
     *
     * The WordPairReader wpr needs to be labeled.
     *
     * This assumes trainClassifier has already been called.
     * @param lang
     * @param datadir
     * @throws IOException
     */
    public static void bootstrap(String lang, String datadir, WordPairReader wpr) throws IOException {
        NamePairClassifier npc = new NamePairClassifier();

        List<String> newTraining = new ArrayList<>();
        List<String> labeled = new ArrayList<>();

        if(!wpr.islabeled){
            logger.error("The WordPairReader needs to be labeled (use wpr.labelPairs(...))");
            System.exit(-1);
        }

        // For each candidate,
        int notmatched = 0;
        WordPair wp;
        while((wp = (WordPair)wpr.next()) != null){

            String label = npc.discreteValue(wp);
            ScoreSet ss = npc.scores(wp);
            double score = ss.get(label);

            //logger.debug(wp + " => " + label);
            //logger.debug(ss.toString());

            boolean matched = false;

            List<String> trainline = new ArrayList<>();
            trainline.add(wp.fileid);
            trainline.add(wp.reflabel);
            trainline.add(wp.refspan.toString());
            trainline.add(wp.refword);
            trainline.add(wp.span + "");
            trainline.add(wp.word);
            trainline.add(score + "");

            if(label.equals("true")) {
                labeled.add(StringUtils.join(trainline, "\t"));
                if(score > 40) {
                    newTraining.add(StringUtils.join(trainline, "\t"));
                }

                matched = true;
            }

            if (!matched){
                notmatched++;
            }
        }

        logger.debug("Not matched: " + notmatched);

        // this should update traininglines.txt
        LineIO.write(datadir + lang + "/traininglines2.txt", newTraining);
        LineIO.write(datadir + lang + "/alllabeled.txt", labeled);
    }



    /**
     * This takes the labeled bootstrap file, and converts into a conll file. This requires
     * the existence of eng.txt and lang.txt in datadir.
     *
     * This writes a file called lang-bootstrap.conll in datadir
     *
     * @param lang two letter code
     * @param datadir path to lang folder.
     * @param bootstrapfile is often called alllabeled.txt, and is created by {@link #bootstrap(String, String, WordPairReader)}.
     * @throws IOException
     * @throws AnnotatorException
     */
    public static void annotateFromBootstrap(String lang, String datadir, String bootstrapfile, String outputfile) throws IOException, AnnotatorException {

        // I expect this to be a set of Conll files.
        String annotationdir = "/shared/corpora/ner/lorelei/" +lang+ "/All/";
        HashSet<String> annotatedids = new HashSet<>();
        if(annotationdir != null){
            String[] filenames = new File(annotationdir).list();
            if(filenames != null) {
                for (String fname : filenames) {
                    String id = fname.split("\\.")[0];
                    annotatedids.add(id);
                }
            }
        }

        // these all have exactly the same number of lines.
        String fname = datadir + "/" + lang + "/eng.txt";
        List<String> eng = Files.readAllLines(new File(fname).toPath(), Charset.defaultCharset());
        List<String> forn = LineIO.read(datadir + "/" + lang + "/" + lang + ".txt");

        if(eng.size() != forn.size()){
            logger.error("Size of eng (" + eng.size() +") doesn't match size of forn (" + forn.size() + ")");
            return;
        }

        List<String> map = null;
        try{
            map = LineIO.read(datadir + "/" + lang + "/map.txt");
        }
        catch(IOException e){
            // it's ok. just map=null
        }

        // create a map from segment+refspan to... label, target span (just take the last one)
        HashMap<String, List<Pair<String, String>>> bootstrapmap = new HashMap<>();
        List<String> bootstrap = LineIO.read(bootstrapfile);
        for(String bootline : bootstrap){
            String[] sline = bootline.split("\t");
            // map from segment+refspan -> label, span

            String key = sline[0];

            if(!bootstrapmap.containsKey(key)){
                bootstrapmap.put(key, new ArrayList<Pair<String, String>>());
            }

            bootstrapmap.get(key).add(new Pair<>(sline[1], sline[4]));
        }

        List<TextAnnotation> annotatedfornTas = new ArrayList<>();
        // maps from file to TA, so I can output GoldPred files...
        HashMap<String, List<TextAnnotation>> id2tas = new HashMap<>();

        // we just need a value that is consistent between here and TaToConll.
        String MYVIEW = "MYNER";

        // loop over eng/forn/map file.
        for(int i = 0; i < eng.size(); i++){
            if (i%100 == 0){
                logger.debug(i + "/" + eng.size());
            }
            String engline = eng.get(i);
            String fornline = forn.get(i);

            boolean goldfile = false;
            String mapline = map.get(i);
            String fileid = mapline.split(":")[0];
            if(annotatedids.contains(fileid)){
                goldfile = true;
            }

            TextAnnotation fornta = TextAnnotationUtilities.createFromTokenizedString(fornline);
            SpanLabelView emptyview = new SpanLabelView(MYVIEW, "UserSpecified", fornta, 1d);
            fornta.addView(MYVIEW, emptyview);

            if(engline.trim().length() == 0 || fornline.trim().length() == 0){
                continue;
            }

            // this line is annotated
            if(bootstrapmap.containsKey(mapline)){

                List<Pair<String, String>> lp = bootstrapmap.get(mapline);

                for(Pair<String,String> p : lp) {

                    String label = p.getFirst();
                    Pair<Integer, Integer> bestspan = WordPairReader.parseSpan(p.getSecond());

                    Constituent fornc = new Constituent(label, MYVIEW, fornta, bestspan.getFirst(), bestspan.getSecond());
                    emptyview.addConstituent(fornc);
                }
            }

            // important: this adds both annotated and unannotated tas to the list.

            if(goldfile){
                // add fornta to list of tas associated with fileid, which is nonempty.
                if(!id2tas.containsKey(fileid)){
                    id2tas.put(fileid, new ArrayList<TextAnnotation>());
                }

                id2tas.get(fileid).add(fornta);

            }

            // FIXME: should this be only if it is NOT gold?
            annotatedfornTas.add(fornta);
        }

        // write the lists out to file.
        logger.info("writing to dir: " + outputfile);
        TaToConll(annotatedfornTas, outputfile, MYVIEW);

        for(String fid : id2tas.keySet()){
            TaToConll(id2tas.get(fid), datadir + lang + "/GoldPred/" + fid + ".conll", MYVIEW);
        }

    }


    /**
     * This expects 2 arguments: language, datadir
     **/
    public static void main(String[] args) throws IOException, AnnotatorException {
        PropertyConfigurator.configure("src/main/resources/log4j-mine.properties");

        String datadir = "/shared/corpora/ner/parallel/";
        String lang = "ug";

//        String datadir = args[0];
//        String lang = args[1];

        // note: you will need to compile NamePairClassifier.lbj first.

//         Gather the candidates first.
        //String view = ViewNames.NER_CONLL;
        String view = GPEVIEW;
        //getCandidates(lang, datadir, view);

        WordPairReader wpr = new WordPairReader(datadir + lang + "/candlines-filter.txt", datadir + lang + "/align-fa/condprob.txt");
        // Create (weak) training data by applying the uniform model, produces edittrain.txt
        applyUniform(lang, datadir, wpr);

        // Label the word pairs with weak labels.
        //wpr.labelPairs(datadir + lang + "/edittrain.txt");

        wpr.reset();
        // Train on the (weak) training data.
        //trainClassifier(wpr);

        wpr.reset();
        // Annotates the candidate file, produces alllabeled.txt
        //bootstrap(lang, datadir, wpr);

        // Produces a lang-bootstrap.conll file in datadir.
        //annotateFromBootstrap(lang, datadir, datadir + lang + "/alllabeled.txt", datadir + lang + "/" + lang + "-bootstrap.conll");
        annotateFromBootstrap(lang, datadir, datadir + lang + "/edittrain.txt", datadir + lang + "/" + lang + "-edit.conll");

        //System.out.println("Don't forget to copy " + lang + "-bootstrap.conll to Train-bootstrap/");
        System.out.println("Don't forget to copy " + lang + "-edit.conll to Train-edit/");
    }

}
