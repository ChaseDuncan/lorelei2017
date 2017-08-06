package edu.illinois.cs.cogcomp.lorelei.edl;

import edu.illinois.cs.cogcomp.lorelei.edl.CandidateList;
import edu.illinois.cs.cogcomp.lorelei.edl.KBEntity;
import edu.illinois.cs.cogcomp.lorelei.edl.KBManager;

import java.util.Collections;
import java.lang.Math;
import java.lang.StringBuilder;

/**
* Data structure for storing and maintaining
* a mention and its relevant data.
**/
public class EDLMention{
  /**
  * @param segID the segment ID of a LORELEI document from
  *              which the mention was extracted.
  * @param surface  the surface form in the text of the mention.
  * @param docID  the document id of the mention which also includes
  *               the character offsets of the surface form.
  * @param candidateIDs the preprocessed candidate IDs of entities
  *                     in the KB.
  **/
  public EDLMention(int segID, 
                    String surface, 
                    String docID, 
                    String[] candidateIDs){

    this.segID = segID;
    this.surface = surface;
    this.docID = docID;
    this.candidateIDs = candidateIDs;
    this.candidates = new CandidateList();
  }

  /**
  * Converts mention to string which
  * gives all pertinent information of the mention
  * in the source text as well as a list
  * of the candidate entities.
  **/
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%s\t%s\t%s\n",
                         String.valueOf(segID),
                         surface,
                         docID));
    for (String id : candidateIDs)
      sb.append(id+" ");
    sb.append("\n");
    for (KBEntity e : candidates)
      sb.append(e.toString() + "\n");
    return sb.toString(); 
  }

  /**
  * Uses the list of candidate entity ids
  * to create KBEntities which correspond
  * to the candidates.
  * @param manager  KBManager object corresponding
  *                 to the KB we intend to link to.
  * TODO: Should this functionality be in LORELEIEDL?
  **/
  public void populateCandidateList(KBManager manager){
    for(String id : candidateIDs){
      KBEntity e = manager.getEntity(id);
      if(e!=null)
        candidates.add(e);
    }
    normalizePopScores();
    sortCandidatesByPop();
  }
  
  /**
  * Normalizes the popularity scores of 
  * candidate entity according to the 
  * other candidates being considered for
  * this mention.
  **/
  private void normalizePopScores(){
    double sum = 0.0;
    for(KBEntity e : candidates){
      sum+=e.getNumAltNames();
    }
    for(KBEntity e : candidates){
      e.setPopScore(e.getNumAltNames() / sum);
    }
  }

  /**
  * Sorts candidates by popularity (prior)
  * in descending order.
  **/
  public void sortCandidatesByPop(){
    Collections.sort(candidates,
                     (c1,c2)->
                     -1*(int)Math
                     .signum(c1.getPopScore() - c2.getPopScore())); 
  }

  /**
  * @return top candidate according to EL system.
  * TODO: decouple top candidate from first candidate
  *       in candidate list.
  **/
  public KBEntity getTopCand(){
    if(candidates.size()>0)
      return candidates.get(0);
    return null;
  }
  
  public String getSurface(){return surface;}
  public String getDocID(){return docID;}
  public int getSegID(){return segID;}

  private String surface;
  private String docID;
  private int segID;
  private String[] candidateIDs;
  private CandidateList candidates;
}
