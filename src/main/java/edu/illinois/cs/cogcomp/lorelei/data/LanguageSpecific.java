package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mayhew2 on 7/6/16.
 */
public class LanguageSpecific {

    static String map = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/docs/ug_charmap.txt";


    public static String uey2uly(String uey) throws FileNotFoundException {

        ArrayList<String> lines = LineIO.read(map);

        HashMap<String, String> ar2lat = new HashMap<>();
        HashMap<String, String> lat2ar = new HashMap<>();
        for(String line : lines){
            String[] sline = line.split("\t");
            String archar = sline[0].trim().replaceAll("\\p{C}", "");
            String latchar = sline[1].trim().replaceAll("\\p{C}", "");

            ar2lat.put(archar, latchar);
            lat2ar.put(latchar, archar);
        }

        ar2lat.put(" ", " ");
        ar2lat.put(":", ":");
        lat2ar.put(" ", " ");

        System.out.println(ar2lat.keySet());

        String out = "";
        while(true) {
            boolean foundmatch = false;
            for (String ar : ar2lat.keySet()) {
                if(uey.startsWith(ar)){
                    out += ar2lat.get(ar);
                    uey = uey.substring(ar.length());
                    foundmatch = true;
                    break;
                }
            }

            if(!foundmatch){
                out += "?";
                uey = uey.substring(1);
            }

            if(uey.length() == 0){
                break;
            }
        }


//        char[] chars = uey.toCharArray();
//        String out = "";
//        for(char c : chars){
//            out += ar2lat.get(c + "");
//        }

        return out;
    }


    public static void main(String[] args) throws FileNotFoundException {
        System.out.println(uey2uly("ئه م ى ل ى : ب ۇ ن ې م ه ؟"));
    }


}
