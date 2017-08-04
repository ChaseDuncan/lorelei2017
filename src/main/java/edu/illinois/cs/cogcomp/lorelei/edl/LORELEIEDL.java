package edu.illinois.cs.cogcomp.lorelei.edl;

import edu.illinois.cs.cogcomp.lorelei.edl.KBEntity;
import edu.illinois.cs.cogcomp.lorelei.edl.KBManager;
import edu.illinois.cs.cogcomp.lorelei.edl.CandidateList;
import edu.illinois.cs.cogcomp.lorelei.edl.EDLDoc;
import edu.illinois.cs.cogcomp.infer.ilp.GurobiHook;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
* LORELEIEDL is the entry point
* for the entity linking functionality
* of this project. 
*
* It expects preprocessed MapDB map 
* of entities for some KB, and a list
* of candidates for some mentions in 
* some text where the candidates correspond
* to ids of entities in the KB.
**/
public class LORELEIEDL{
  /**
  * @param dbPath path to location of preprocessed MapDB file
  * @param candDoc path to candidate file
  * @param outputFile path to where output should be written
  **/
  public LORELEIEDL(String dbPath, 
                    String candDoc, 
                    String outputFile){
    //gurobiHook = new GurobiHook(0);
    KBManager kb = new KBManager(dbPath);
    EDLDoc doc = new EDLDoc(candDoc);
    doc.populateCandidateLists(kb); 
    try{
      writeResultsToFile(outputFile,doc);     
    } catch (IOException e){
      e.printStackTrace();
    }
    //System.out.println(doc.toString());
  }
  
  /**
  * @param outputFile path to file to write output to
  * @param doc  EDLDoc whose values will be output
  **/
  public void writeResultsToFile(String outputFile,
      EDLDoc doc) throws IOException{
    System.out.println("Writing results to file.");
    PrintWriter out
      = new PrintWriter(
          new BufferedWriter(
            new FileWriter(outputFile)));

    for(EDLMention m : doc.getMentions()){
      out.write(String.format("%s\t%s\t%s\t%s\n",
            m.getDocID(),
            m.getSegID(),
            m.getSurface(),
            m.getTopCand().toString()));
    }
      out.write("##########################");
      out.write(doc.toString());
    out.flush();
  }
  
  GurobiHook gurobiHook = new GurobiHook();
}

