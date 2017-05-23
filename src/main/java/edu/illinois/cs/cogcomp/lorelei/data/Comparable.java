package edu.illinois.cs.cogcomp.lorelei.data;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.lang3.StringUtils;
import edu.illinois.cs.cogcomp.transliteration.SPModel;
import edu.illinois.cs.cogcomp.utils.TopList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataReader;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by mayhew2 on 7/7/16.
 */
public class Comparable {

    static String dir = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/data/translation/comparable/clusters/";
    static String engdir = "/shared/corpora/ner/eval/column/set0-comparable/eng-anno/";
    static String il3dir = "/shared/corpora/ner/eval/column/set0-comparable/il3-translit/";


    class Cluster {

        public List<String> il3files = new ArrayList<>();
        public List<String> engfiles = new ArrayList<>();

        public Cluster(){
        }
        
        public Cluster(List<String> eng, List<String> il3){
            il3files = il3;
            engfiles = eng;
        }
    }
    
    
    public List<Cluster> readClusters() throws Exception{

        String clusterfname = "20110805_20110818_nw_clusters.xml";
        List<String> cluster = LineIO.read(dir + clusterfname);

        List<Cluster> clusters = new ArrayList<>();

        Cluster c = null;

        Pattern pat = Pattern.compile("docid=\\\"([^\"]*)\\\"");
        Pattern langpat = Pattern.compile("language=\\\"([^\"]*)\\\"");
        
        for(String line : cluster){
            if(line.contains("<cluster id")){
                c = new Cluster();
            }else if(line.contains("docid=")){
                Matcher m = pat.matcher(line);
                m.find();
                String docname = m.group(1);
                
                m = langpat.matcher(line);
                m.find();
                String lang = m.group(1);

                if(lang.equals("eng")){
                    c.engfiles.add(docname);
                }else{
                    c.il3files.add(docname);
                }
                
            }else if(line.contains("</cluster>")){
                clusters.add(c);
            }
        }

        return clusters;
        
        
    }

    public void process(List<Cluster> clusters) throws Exception{

        String ugprobs = "/home/mayhew2/IdeaProjects/illinois-transliteration/models/probs-ULYfix.txt";
        SPModel model = new SPModel(ugprobs);

        for(Cluster c : clusters){

            HashMap<String, HashSet<String>> entities = new HashMap<>();
            entities.put("GPE", new HashSet<String>());
            entities.put("LOC", new HashSet<String>());
            entities.put("ORG", new HashSet<String>());
            entities.put("PER", new HashSet<String>());
            
            
            for(String engfname : c.engfiles){
                NERDocument doc = TaggedDataReader.readFile(engdir + "/" + engfname, "-c", engfname);
                for(int i = 0; i < doc.sentences.size(); i++){
                    LinkedVector sentence = doc.sentences.get(i);
                    String fullw = null;
                    String currlabel = null;
                    for(int j = 0; j < sentence.size(); j++){
                        NEWord w = ((NEWord) sentence.get(j));

                        if(w.neLabel.startsWith("B-")){
                            // save last one,
                            //start new one.
                            if(fullw != null){
                                fullw = fullw.trim();
                                entities.get(currlabel).add(fullw);
                                for(String ss : fullw.split("\\s+")) {
                                    entities.get(currlabel).add(ss);
                                }
                            }

                            currlabel = w.neLabel.split("-")[1];
                            fullw = w.form + " ";
                        }else if(w.neLabel.startsWith("I-")){
                            fullw += w.form + " ";
                        }else{
                            if(fullw != null){
                                fullw = fullw.trim();
                                entities.get(currlabel).add(fullw);
                                for(String ss : fullw.split("\\s+")) {
                                    entities.get(currlabel).add(ss);
                                }
                            }

                            fullw = null;
                            currlabel = null;
                        }
                    }
                }
            }



            // now search for these entities in the IL3 docs.
            for(String il3fname : c.il3files) {
                NERDocument doc = TaggedDataReader.readFile(il3dir + "/" + il3fname, "-c", il3fname);
                // for each entity, look over the entire set of docs (all sentences, all words).
                // keep track of all scores, get the best one.
                
                for(String per : entities.get("PER")){
                    per = per.toLowerCase();
                    String best = "";
                    double bestprob = 0;
                    
                    for(int i = 0; i < doc.sentences.size(); i++){
                        LinkedVector sentence = doc.sentences.get(i);
                        for(int j = 0; j < sentence.size(); j++){
                            NEWord w = ((NEWord) sentence.get(j));
                            
                            // get some kind of distance between per and w.form
                            
                            // now try transliteration
                            double prob = model.Probability(per,w.form);
                            if(prob > bestprob){
                                best = w.form;
                                bestprob = prob;                                   
                            }
                            
                            
                            
                        }
                    }
                    System.out.println("Best for " + per + ": " + best + "(" + bestprob + ")");
                }
            }       

            break;  // after 1 cluster
        }
    }
    
    
    public static void main(String[] args) throws Exception {
        Comparable comp = new Comparable();
        comp.process(comp.readClusters());
    }
}
