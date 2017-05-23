package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mayhew2 on 3/1/16.
 */
public class TagEnglish {

    /**
     * This just removes all names and replaces them with **NAME**. Intended for a
     * sort of embeddings exercise. I don't think it worked.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String dir = "/shared/corpora/ratinov2/NER/Data/GoldData/Reuters/BracketsFormatDocumentsSplitMyTokenization/Train/";

        File f = new File(dir);

        String[] ls = f.list();

        Pattern pat = Pattern.compile("\\[\\w\\w\\w[^\\]]+\\]");

        List<String> lines = new ArrayList<>();

        for(String fname : ls){
            List<String> text = LineIO.read(dir + fname);


            for(String line : text){
                // text replace all tags...
                Matcher m = pat.matcher(line);
                String s = m.replaceAll("**NAME**");
                //System.out.println(s);
                lines.add(s);
            }

        }

        LineIO.write("out.txt", lines);



    }
}
