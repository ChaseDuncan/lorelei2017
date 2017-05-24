package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static weka.core.converters.ConverterUtils.DataSource.read;

/**
 *
 *
 * TODO: move this to illinois-corpus-readers
 *
 * Created by mayhew2 on 4/18/16.
 */
public class AlignmentReaders {

    private static Logger logger = LoggerFactory.getLogger( AlignmentReaders.class );

    static Pattern pattern = Pattern.compile("(([\\w\\p{Punct}]*)\\s*\\(\\{([^}]*)\\}\\))");
    static Pattern sentpair = Pattern.compile("# Sentence pair \\((\\d+)\\)");

    /**
     * Length of alignment needs to match the length of sourcewords.
     * Each List of integers in alignment refers to the indices of the target words that THIS
     * source word aligns to.
     */
    public static class Alignment{

        private final List<String> targetwords;
        private final List<String> sourcewords;
        private final List<List<Integer>> alignment;
        private final double prob;
        private final int id;

        public Alignment(int id, List<String> sourcewords, List<String> targetwords, List<List<Integer>> alignment, double prob){
            this.id = id;
            this.sourcewords = sourcewords;
            this.targetwords = targetwords;
            this.alignment = alignment;
            this.prob = prob;
        }

        public int getID(){
            return this.getId();
        }

        public List<String> getTargetwords() {
            return targetwords;
        }

        public List<String> getSourcewords() {
            return sourcewords;
        }

        public List<List<Integer>> getAlignment() {
            return alignment;
        }

        public double getProb() {
            return prob;
        }

        public int getId() {
            return id;
        }
    }


    /**
     *
     * @param fname name of conditional probabilities (produced with the -p flag in fast_align)
     * @return a map of {English: (Forn, score), ...}
     * @throws FileNotFoundException
     */
    public static HashMap<String, List<Pair<String, Double>>> readConditionalProbabilities(String fname) throws FileNotFoundException {
        List<String> lines = LineIO.read(fname);

        HashMap<String, List<Pair<String, Double>>> ret = new HashMap<>();

        for(String line : lines){
            String[] sline = line.split("\t");
            String eng = sline[0];
            String forn = sline[1];
            double score = Double.parseDouble(sline[2]);

            Pair<String, Double> p = new Pair<>(forn, score);

            if(!ret.containsKey(eng)){
                ret.put(eng, new ArrayList<>());
            }
            ret.get(eng).add(p);
        }

        return ret;
    }

    /**
     * Reads Pharaoh alignments (style: 0-0 1-1 4-3)
     *
     * Intended to read output of fast_align
     *
     * @param
     * @return
     * @throws IOException
     */
    public static List<Alignment> readPharaohAlignments(String parallelfile, String alignfile) throws IOException {

        logger.debug("Reading alignments from: " + alignfile);

        List<Alignment> alignments = new ArrayList<>();

        int limit = -1;
        if(limit > -1) {
            logger.warn("USING ONLY {} alignments!", limit);
        }

        BufferedReader bralign = new BufferedReader(new FileReader(alignfile));
        BufferedReader brpar = new BufferedReader(new InputStreamReader(new FileInputStream(parallelfile), "UTF8"));

        int i = 1;
        String line;
        String[] parline;

        while ((line = bralign.readLine()) != null){

//            if (line.isEmpty()){
//                System.out.println("Skipping empty line...");
//                continue;
//            }



            
            parline = brpar.readLine().split(" \\|\\|\\| ");

            System.out.println(parline[0]);
            String[] eline = parline[0].split(" ");
            String[] fline = parline[1].split(" ");

            System.out.println(Arrays.asList(fline));

            List<List<Integer>> alignment = new ArrayList<>();
            // this is for the NULL token (to play nice with giza)
            alignment.add(new ArrayList<Integer>());
            for(String w : eline){
                alignment.add(new ArrayList<Integer>());
            }

            String[] sline = line.trim().split(" ");

            if(sline.length > 1) {
                // each p is of the form 0-0 (or some int)
                for (String p : sline) {

                    String[] pp = p.split("-");
                    int e = Integer.parseInt(pp[0]);
                    int f = Integer.parseInt(pp[1]);

                    // add 1 to e to make up for NULL entry
                    // add 1 to f because giza alignments are 1-based.
                    alignment.get(e + 1).add(f + 1);

                }
            }

            ArrayList<String> sourcewords = new ArrayList<>();
            sourcewords.add("NULL");
            for(String s : eline){
                sourcewords.add(s);
            }

            Alignment a = new Alignment(i, sourcewords, Arrays.asList(fline), alignment, i);
            alignments.add(a);
            i++;

        }

        return alignments;
    }


    public static List<Alignment> readGizaAlignments(String fname) throws FileNotFoundException,IOException {
        // takes an alignment file.

        logger.debug("Reading alignments from: " + fname);

        List<Alignment> alignments = new ArrayList<>();

        int limit = -1;
        if(limit > -1) {
            logger.warn("USING ONLY {} alignments!", limit);
        }

        //ArrayList<String> lines = LineIO.read(fname);

        BufferedReader br = new BufferedReader(new FileReader(fname));
        String comment;
        String target;
        String source;
        int i = 1;
        while ((comment = br.readLine()) != null){
            // process the line.
            target = br.readLine();
            source = br.readLine();

            // For each line in the Giza++ output file.
            //for (int i = 0; i < lines.size() - 3; i += 3) {
            //String comment = lines.get(i);
            //String target = lines.get(i + 1);
            //String source = lines.get(i + 2);

            String[] sc = comment.split(" ");
            double prob = Double.parseDouble(sc[sc.length-1]);

            ArrayList<String> sourcewords = new ArrayList<>();
            List<String> targetwords = Arrays.asList(target.split(" "));
            List<List<Integer>> alignment = new ArrayList<>();

            Matcher m = pattern.matcher(source);

            // check for characters that will mess up the parsing, and just ignore...
            if(target.contains("({")){
                i++;
                continue;
            }


            while (m.find()) {
                String sourceword = m.group(2).trim();
                String[] stringnums = m.group(3).trim().split(" ");

                List<Integer> targetnums = new ArrayList<>();
                for(String s : stringnums){
                    if(s.isEmpty()) continue;
                    try {
                        targetnums.add(Integer.parseInt(s));
                    }catch(NumberFormatException e){
                        System.out.println(e.getStackTrace());
                    }
                }

                alignment.add(targetnums);
                sourcewords.add(sourceword);
            }

            Alignment a = new Alignment(i, sourcewords, targetwords, alignment, prob);
            alignments.add(a);

            if (limit != -1 && i > limit){
                break;
            }
            i++;
        }

        logger.debug("Done reading alignments...");
        return alignments;
    }

    public static void main(String[] args) throws IOException {
        String d = "/shared/corpora/ner/parallel/bn/align-fa/";
        List<Alignment> aa = AlignmentReaders.readPharaohAlignments(d + "text.en-bn", d + "forward-bn.align");



    }

}
