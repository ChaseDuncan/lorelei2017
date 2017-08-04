package edu.illinois.cs.cogcomp.lorelei.edl;

import edu.illinois.cs.cogcomp.lorelei.edl.LORELEIEDL;
import edu.illinois.cs.cogcomp.lorelei.edl.KBEntity;

import java.io.IOException;

public class CoherenceTests{
  public static void main(String[] args){
    String test,kbPath,dbPath,candDoc,outputFile;
    test = args[0];
    switch(test){
      case "kb": 
        dbPath = args[2];
        CoherenceTests.kbManagerTest(dbPath);
        break;
      case "map":
        kbPath = args[1];
        dbPath = args[2];
        CoherenceTests.entityMapTest(kbPath,dbPath);
        break;
      case "cand":
        dbPath = args[2];
        candDoc=args[3];
        outputFile=args[4];
        CoherenceTests.parseCandDocTest(dbPath,candDoc,outputFile);
        break;
      default:
        System.out.println("Test must be specified.");
    }
  }

  public static void parseCandDocTest(String dbPath,
                                      String candDoc,
                                      String outputFile){
    LORELEIEDL edl = new LORELEIEDL(dbPath,candDoc,outputFile);
  }

  public static void entityMapTest(String kbPath, String dbPath){
      KBManager kb = new KBManager(dbPath);
      kb.buildEntityMap(kbPath);
  }

  public static void kbManagerTest(String dbPath){
    KBManager kb = new KBManager(dbPath);
    KBEntity entity = kb.getEntity(337996); 
    System.out.println(entity.toString());
  }
}
