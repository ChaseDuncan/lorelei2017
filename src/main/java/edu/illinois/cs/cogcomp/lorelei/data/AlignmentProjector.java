package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.nlp.util.SimpleCachingPipeline;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


/**
 *
 * This class takes a giza alignment file (A3.final), where
 * English is the source lang. It tags the English, then projects labels
 * to the foreign language and produces CoNLL style output.

 *
 * Created by mayhew2 on 3/4/16.
 */
@SuppressWarnings("ALL")
public class AlignmentProjector {

    private static Logger logger = LoggerFactory.getLogger( AlignmentProjector.class );


    private static String mynerconfig = "config/tac.config";

    /**
     * Given a set of alignments (read by {@link AlignmentReaders}) of English-Foreign
     * alignments, annotate English and project to foreign. Generates
     * CoNLL tyle outputs for training NER.
     **/
    public void project(List<AlignmentReaders.Alignment> alignments, String outfile) throws Exception {
        String docId = "NOTHING"; // arbitrary string identifier
        String textId = "body"; // arbitrary string identifier

//        String TACVIEW = "NER_TAC";
        String TACVIEW = ViewNames.NER_CONLL;

        ResourceManager rm = new ResourceManager( "config/pipeline-config.properties" );
        SimpleCachingPipeline pipeline = (SimpleCachingPipeline) IllinoisPipelineFactory.buildPipeline( rm );

//        IllinoisNerHandler inh = new IllinoisNerHandler(new ResourceManager("config/tac.config"), TACVIEW);
//        pipeline.addAnnotator(inh);
//        pipeline.setForceUpdate(true);

        List<String> outlines = new ArrayList<>();
        List<String> debuglines = new ArrayList<>();
        outlines.add("O\t0\t0\tO\t-X-\t-DOCSTART-\tx\tx\t0");
        outlines.add("");

        for(AlignmentReaders.Alignment a : alignments) {

            if(a.getID()%1000 == 0) {
                logger.debug("Sent id: " + a.getID());
            }

            // don't pass in NULL at the beginning...
            ArrayList<String> swAnnotate = new ArrayList<>(a.getSourcewords());
            swAnnotate.remove(0);

            String[] arr = swAnnotate.toArray(new String[0]);
            List<String[]> toksent = new ArrayList<>();
            toksent.add(arr);

            TextAnnotation ta = BasicTextAnnotationBuilder.createTextAnnotationFromTokens(toksent);
            HashSet<String> viewsToAnnotate = new HashSet<>();
            viewsToAnnotate.add(TACVIEW);

            // shouldn't need to do this...
            try {
                //ta = pipeline.addViewsAndCache(ta, viewsToAnnotate);
                ta = pipeline.annotateTextAnnotation(ta, false);
            }catch (AnnotatorException e){
                logger.error(e.getStackTrace().toString());
            }

            String viewName = TACVIEW; // example using ViewNames class constants

            View view = ta.getView( viewName );
            List<Constituent> constituents = view.getConstituents();

            debuglines.add(a.getSourcewords().toString());
            
            // This maps from targetword index (0-based) to tag.
            HashMap<Integer, String> tagmap = new HashMap<>();

            for (Constituent c : view.getConstituents()) {
                // The 1 is added to make up for the NULL token that was removed when we annotate.
                // getStartSpan refers to tokens in the constituent (NOT chars)
                int start = c.getStartSpan() + 1;
                int end = c.getEndSpan() + 1;
                
                // look in alignment to see if this span maps to anything.
                for(int i = start; i < end; i++){
                    if(i == a.getAlignment().size()){
                        System.out.println("BJRELKJLKWJW");
                    }
                    List<Integer> al = a.getAlignment().get(i);

                    for(int j : al){
                        // because the alignment indices are 1-based.
                        tagmap.put(j-1, c.getLabel());

                    }
                }

                debuglines.add(c.getLabel() + " " + c.toString());
            }

            int wordIndex = 0;
            String prevtag = "O";
            String tgtann = "";

            for(String tword : a.getTargetwords()){
                String neTagString = "O";
                if(tagmap.containsKey(wordIndex)){
                    String currtag = tagmap.get(wordIndex);
                    neTagString = ((currtag.equals(prevtag)) ? "I-" : "B-") + currtag;
                    prevtag = currtag;
                }

                if(neTagString.equals("O")){
                    prevtag = "O";
                    tgtann += tword + " ";
                }else{
                    tgtann += "[" + neTagString + " " + tword + "] ";
                }

                outlines.add(neTagString + "\t0\t" + wordIndex++ + "\tx\tx\t" + tword + "\tx\tx\t0");
            }
            debuglines.add(tgtann);
            debuglines.add("");

            // empty line at the end of each sentence.
            outlines.add("");

        }

        logger.debug("Writing to {}", outfile);
        LineIO.write(outfile, outlines);
        LineIO.write("debug.txt", debuglines);

    }

    /**
     * Given a conll file created by projection, this will fix it by propagating tags to similar strings.
     * @param fname this should be a conll file (e.g. output of {@link #project(String, String)})
     * @throws FileNotFoundException
     */
    public static void fixfile(String fname) throws IOException {
        List<String> lines = LineIO.read(fname);

        // maps from word (cleaned) to {tag: freq, tag: freq ..}
        HashMap<String, HashMap<String, Integer>> hist = new HashMap<>();

        String[] tags = {"PER", "ORG", "MISC", "LOC", "O"};

        for(String line : lines){
            if(line.isEmpty()) continue;

            String[] sline = line.split("\t");
            String tag = sline[0];

            if(tag.contains("-")){
                tag = tag.substring(2); // cut off B- and I-
            }

            // remove punctuation
            String word = sline[5].replaceAll("[,.'!?\";:()]", "");

            HashMap<String, Integer> tagfreqs;
            if(!hist.containsKey(word)){
                tagfreqs = new HashMap<>();
                for(String tagname : tags){
                    tagfreqs.put(tagname, 0);
                }

                hist.put(word, tagfreqs);
            }else{
                tagfreqs = hist.get(word);
            }

            int tagfreq = tagfreqs.get(tag);
            tagfreqs.put(tag, tagfreq + 1);
        }

        List<String> outlines = new ArrayList<>();

        String prevtag = "O";
        for(String line : lines){
            if(line.isEmpty()){
                outlines.add("");
                continue;
            }

            String[] sline = line.split("\t");
            String tag = sline[0];

            if(tag.contains("-")){
                tag = tag.substring(2); // cut off B- and I-
            }

            // remove punctuation
            String word = sline[5].replaceAll("[,.'!?\";:()]", "");

            HashMap<String, Integer> tagfreq = hist.get(word);

            // get the best tag that is not O
            int maxval = 0;
            String besttag = "";
            for(String ft : tagfreq.keySet()){
                if(ft.equals("O")) continue;
                int val = tagfreq.get(ft);
                if(val > maxval) {
                    maxval = val;
                    besttag = ft;
                }
            }

            int numo = tagfreq.get("O");

            // set the prefix correctly.
            if(prevtag.equals("O")){
                besttag = "B-" + besttag;
            }else{
                besttag = "I-" + besttag;
            }

            // If there are twice as many O as others, then snap to that.
            if(numo > 2*maxval){
                besttag = "O";
            }

            sline[0] = besttag;
            outlines.add(StringUtils.join(sline, "\t"));
            prevtag = besttag;
        }

        LineIO.write(fname + ".fix", outlines);
    }




    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("src/main/resources/log4j-mine.properties");
        AlignmentProjector ap = new AlignmentProjector();

        String lang = "yo";
        String basedir = "/shared/corpora/ner/parallel/" + lang + "/align-fa/";
        String fname = basedir + "final-"+  lang + ".align";

        args = new String[4];
        args[0] = "fa";
        args[1] = basedir + "text.en-" + lang;
        args[2] = fname;
        args[3] = "/shared/corpora/ner/parallel/" + lang + "/" + lang + "-fa.conll";

        List<AlignmentReaders.Alignment> alignments = null;
        if(args[0].equals("giza")){
            // this should be a 
            alignments = AlignmentReaders.readGizaAlignments(args[1]);
            String outname = args[2];
            ap.project(alignments, outname);
        
        }else if(args[0].equals("fa")){
            // parallel file, align file
            alignments = AlignmentReaders.readPharaohAlignments(args[1], args[2]);
            String outname = args[3];
            ap.project(alignments, outname);

        }else{
            System.out.println("Must use option: giza or fa");
            System.exit(-1);
        }


        //ap.fixfile("/shared/corpora/ner/conll2003/deu/Train/german.conll.fix");
    }

}
