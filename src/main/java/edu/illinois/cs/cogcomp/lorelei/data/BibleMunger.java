package edu.illinois.cs.cogcomp.ner.data;


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
import weka.classifiers.evaluation.Prediction;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The point of this class is to run NER on the English data, and to get alignments.
 *
 * This has been edited heavily to match with
 *
 * Created by mayhew2 on 10/26/15.
 */
public class BibleMunger {

    /**
     * Change this one, not the others.
     */
    //public static final String langfolder = "Chinese_English/";

    public static final String basedir = "/shared/experiments/mayhew2/bibles/";
    //public static final String outfile = basedir + langfolder + "pairs.txt";


    public static void main(String[] args) throws Exception {
        ResourceManager rm = new CuratorConfigurator().getDefaultConfig();
        AnnotatorService curator = CuratorFactory.buildCuratorClient(rm);

        // I think this matches the giza++ output format...
        Pattern pattern = Pattern.compile("(([\\w\\p{Punct}]*)\\s*\\(\\{([^}]*)\\}\\))");

        Pattern sentpair = Pattern.compile("# Sentence pair \\((\\d+)\\)");

        File bd = new File(basedir);

        Path bdpath = bd.toPath();

        String[] langfolders = bd.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("_English");
            }
        });

        for(String langfolder : langfolders) {

            System.out.println(langfolder);

            langfolder = langfolder + "/";

            HashSet<String> pairs = new HashSet<>();
            File d = new File(basedir + langfolder);

            if (new File(basedir + langfolder + "/pairs.txt").exists()) {
                System.err.println("pairs.txt already exists in the folder: " + langfolder + ". Quitting...");
                continue;
            }
            System.out.println(langfolder);

            System.out.println(d);
            String[] flist = d.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains("17.mayhew2.A3.final");
                }
            });

            if (flist.length != 1) {
                System.err.println("OOOPS. Can't find the file I want in " + langfolder);
                continue;
            }

            String alignfilename = flist[0];

            ArrayList<String> lines = LineIO.read(basedir + langfolder + alignfilename);
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

                ArrayList<String[]> tokenizedtext = new ArrayList<>();
                tokenizedtext.add(sourcewords.toArray(new String[sourcewords.size()]));
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

                for (Constituent c : v.getConstituents()) {
                    int start = c.getStartSpan();
                    int end = c.getEndSpan();

                    String s = StringUtils.join(sourcewords.subList(start, end), " ");
                    String t = StringUtils.join(targetwords.subList(start, end), " ");
                    if (t.length() > -1) {

                        System.out.println(c.getLabel() + "\t" + "[" + s + "]\t[" + t + "]");
                        pairs.add(s + "\t" + t);

                    }
                }

                System.out.println();
            }

            LineIO.write(basedir + langfolder + "/pairs.txt", pairs);
        }
    }
}
