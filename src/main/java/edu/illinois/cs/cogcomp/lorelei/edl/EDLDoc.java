package edu.illinois.cs.cogcomp.lorelei.edl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.Integer;
import java.lang.StringBuilder;

/**
* Storage class for data corresponding
* to a document that is to be linked
* to the KB.
**/
public class EDLDoc{
  /**
  * @param candDoc  path to file of candidates entities
  *                 for each mention in a text.
  **/
  public EDLDoc(String candDoc){
    mentions = new MentionList();
    parseCandDoc(candDoc);
  }

  /**
  * see EDLMention::populateCandidateList
  * @param manager  KBManger corresponding to the KB
  *                 we intend to link to.
  **/
  public void populateCandidateLists(KBManager manager){
    for(EDLMention m : mentions){
      m.populateCandidateList(manager);
    }
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    for(EDLMention m : mentions)
      sb.append(m.toString());
    return sb.toString();
  }

  /**
  * Turns document of candidates into 
  * list of mentions.
  * @param candDoc  candidate file
  **/
  private void parseCandDoc(String candDoc){
    BufferedReader br = null;
    String line;
    try{
      br = new BufferedReader(new FileReader(candDoc));
      while((line = br.readLine())!=null){
        String[] spline = line.split("\t");
        int segID = Integer.parseInt(spline[1]);
        String surface = spline[2];
        String docID = spline[3];
        String[] cands = spline[4].split(",");
        EDLMention m = new EDLMention(segID,surface,docID,cands);
        mentions.add(m);
      }
    }catch(IOException e){
      e.printStackTrace();
    }
  }

  public MentionList getMentions(){return mentions;}
  private MentionList mentions;
}

