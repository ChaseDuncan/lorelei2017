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
import edu.illinois.cs.cogcomp.curator.CuratorConfigurator;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Probably not used. See AlignmentReader and AlignmentProjectors instead.
 *
 * The point of this class is to run NER on the English data, and to get alignments.
 * Created by mayhew2 on 10/26/15.
 */
@SuppressWarnings("Duplicates")
public class Munger {

    static String annotationdir = "/shared/corpora/corporaWeb/lorelei/LDC2015E70_BOLT_LRL_Hausa_Representative_Language_Pack_V1.2/data/annotation/entity_annotation/simple/";
    static String langfolder = "/scratch/lorelei/align/";

    // I think this matches the giza++ output format...
    static Pattern pattern = Pattern.compile("(([\\w\\p{Punct}]*)\\s*\\(\\{([^}]*)\\}\\))");
    static Pattern sentpair = Pattern.compile("# Sentence pair \\((\\d+)\\)");

    /**
     * Used to hold predictions from projection
     */
    private static class Prediction {
        int end;
        int start;
        String label;
        String word;

        public Prediction(String word, String label, int start, int end) {
            this.word = word;
            this.label = label;
            this.start = start;
            this.end = end;

        }

        @Override
        public String toString() {
            return "[" + this.label + ", " + this.start + "-" + this.end + "]";
        }
    }

    /**
     *
     * @throws Exception
     */
    public static void GetPredictionScore() throws Exception {
        ResourceManager rm = new CuratorConfigurator().getDefaultConfig();
        AnnotatorService curator = CuratorFactory.buildCuratorClient(rm);

        ArrayList<String> maplines = LineIO.read("/scratch/lorelei/map.txt");
        String alignfilename = "116-02-24.112443.mayhew2.A3.final";

        HashSet<String> pairs = new HashSet<>();

        ArrayList<String> lines = LineIO.read(langfolder + alignfilename);

        String lastdocname = "";
        String doctext = "";

        HashMap<String, ArrayList<Prediction>> predmap = new HashMap<>();

        // For each line in the Giza++ output file.
        for (int i = 0; i < lines.size() - 3; i += 3) {
            String comment = lines.get(i);
            String target = lines.get(i + 1);
            String source = lines.get(i + 2);

            String[] tline = target.split(" ");

            ArrayList<String> sourcewords = new ArrayList<>();
            ArrayList<String> targetwords = new ArrayList<>();

            String[] targetnums;
            String sourceword;

            Matcher m = pattern.matcher(source);

            while (m.find()) {
                sourceword = m.group(2).trim();
                targetnums = m.group(3).trim().split(" ");

                String targetword = "";
                for (String t : targetnums) {
                    if (t.length() > 0) {
                        int n = Integer.parseInt(t);
                        targetword += tline[n - 1] + " ";
                    }
                }
                sourcewords.add(sourceword);
                targetwords.add(targetword.trim());

            }
            String text = StringUtils.join(sourcewords, " ");

            String lafname = maplines.get(i/3) + ".laf.xml";

            String rawname = "/scratch/lorelei/parallel/" + maplines.get(i/3);
            if(new File(rawname + ".hau.rsd.txt").exists()){
                rawname += ".hau.rsd.txt";
            }else{
                rawname += ".rsd.txt";
            }

            File f = new File(annotationdir + lafname);
            if(!f.exists()){
                //System.out.println("No annotation...");
                continue;
            }

            // should save on document slurps.
            if(rawname != lastdocname){
                doctext = LineIO.slurp(rawname);
                lastdocname = rawname;
            }

            System.out.println("Annotation doc: " + lafname);

            // don't pass in NULL at the beginning...
            ArrayList<String> swAnnotate = new ArrayList<>(sourcewords);
            swAnnotate.remove(0);
            ArrayList<String[]> tokenizedtext = new ArrayList<>();
            tokenizedtext.add(swAnnotate.toArray(new String[swAnnotate.size()]));
            TextAnnotation ta = BasicTextAnnotationBuilder.createTextAnnotationFromTokens(tokenizedtext);

            try {
                System.out.println(target);
                System.out.println(text);
                curator.addView(ta, ViewNames.NER_CONLL);
            } catch (AnnotatorException|IllegalStateException e) {
                System.err.println("Oops... annotator exception.");
                continue;
            }

            View v = ta.getView(ViewNames.NER_CONLL);

            ArrayList<Prediction> predictions = new ArrayList<>();

            for (Constituent c : v.getConstituents()) {
                // The 1 is added to make up for the NULL token that was removed when we annotate.
                // getStartSpan refers to tokens in the constituent (NOT chars)
                int start = c.getStartSpan() + 1;
                int end = c.getEndSpan() + 1;

                String s = StringUtils.join(sourcewords.subList(start, end), " ");
                String t = StringUtils.join(targetwords.subList(start, end), " ");
                if (t.length() > -1) {

                    System.out.println(c.getLabel() + "\t" + "[" + s + "]\t[" + t + "]");
                    pairs.add(s + "\t" + t);

                    int ind = doctext.indexOf(t);
                    System.out.println(ind + "," + (ind + t.length() - 1));

                    if (ind > -1){
                        Prediction p = new Prediction(t, c.getLabel(), ind, ind+t.length()-1);
                        predictions.add(p);
                    }
                }
            }

            if(!predmap.containsKey(lafname)){
                // extend current list
                predmap.put(lafname, new ArrayList<Prediction>());
            }
            
            predmap.get(lafname).addAll(predictions);
            
            System.out.println();
        }

        double tp = 0;
        double fp = 0;
        double fn = 0;
        
        // loop over all predictions...
        for(String lafname : predmap.keySet()){
           ArrayList<Prediction> preds = predmap.get(lafname);
            System.out.println(lafname + ": " + preds);
            String[] tpfpfn = score(preds, lafname).split(":");
            tp += Integer.parseInt(tpfpfn[0]);
            fp += Integer.parseInt(tpfpfn[1]);
            fn += Integer.parseInt(tpfpfn[2]);
            System.out.println();
        }

        System.out.println(tp + ", " + fp + ", " + fn);
        double precision = tp / (tp + fp);
        double recall = tp / (tp + fn);
        double f1 = 2 * precision * recall / (precision + recall);
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F1: " + f1);
        
        LineIO.write(langfolder + "/pairs.txt", pairs);
    }


    // Given a set of predictions and the name of an annotation file, return a string that
    // encodes tp:fp:fn
    private static String score(ArrayList<Prediction> predictions, String lafname) throws Exception {

        Pattern p = Pattern.compile("type=\"(\\w+)\"");
        Pattern sc = Pattern.compile("start_char=\"(\\d+)\"");
        Pattern ec = Pattern.compile("end_char=\"(\\d+)\"");

        int tp = 0;
        int total = 0;
        
        // open file and get annotations.
        ArrayList<String> lines = LineIO.read(annotationdir + lafname);
        for(int i = 0; i < lines.size(); i++){
            String line = lines.get(i);
            if(line.trim().startsWith("<ANNOTATION")){
                total++;
                
                // get the label
                Matcher m = p.matcher(line);
                m.find();
                String label = m.group(1);
                                
                // get next line
                String extent = lines.get(i+1);
                // get start_char, end_char
                Matcher scm = sc.matcher(extent);
                scm.find();
                int start_char = Integer.parseInt(scm.group(1));
                Matcher ecm = ec.matcher(extent);
                ecm.find();
                int end_char = Integer.parseInt(ecm.group(1));

                System.out.println(label + ", " + start_char + "-" + end_char);

                for(Prediction pred : predictions){
                    //if(pred.start == start_char && pred.end == end_char){
                    if(pred.start == start_char && pred.end == end_char && pred.label.equals(label)){
                            //if(pred.end == end_char){
                        //if(pred.start == start_char){
                        System.out.println("WE HAVE A MATCH");
                        tp++;
                    }
                }
            }
        }

        int fp = predictions.size() - tp;
        int fn = total - tp;
        
        return tp + ":" + fp + ":" + fn;
    }


    public static void main(String[] args) throws Exception {
       GetPredictionScore();

    }

}
