package edu.illinois.cs.cogcomp.lorelei.edl;

/**
* Storage class for data corresponding to 
* and entity in the KB to be linked to.
**/
public class KBEntity{
  /**
  * @param attributes string array of data from KB that defines
  *                   the entity.
  * TODO: Need to add the rest of the data.
  **/
  public KBEntity(String[] attributes){
    setGeoID(attributes[0]);
    setNameUTF8(attributes[1]);
    setNameASCII(attributes[2]);
    setNumAltNames(attributes[3].split(",").length);
    wikiPrior = 0.0;
    popScore = 0.0;
  }

  public String toString(){
    return String.format("%s\t%s\t%s\t%f",
                         _geoID,_nameUTF8,_nameASCII,popScore);
  }

  public double getPopScore(){return popScore;}
  public int getNumAltNames(){return numAltNames;}
  
  public void setGeoID(String geoID){_geoID=geoID;}
  public void setNameUTF8(String nameUTF8){_nameUTF8 = nameUTF8;}
  public void setNameASCII(String nameASCII){_nameASCII=nameASCII;}
  public void setPopScore(double score){popScore = score;}
  public void setNumAltNames(int numAltNames){ this.numAltNames = numAltNames;}

  private String _geoID;
  private String _nameASCII;
  private String _nameUTF8;
  private int numAltNames;
  private double wikiPrior;
  private double popScore;
 } 
