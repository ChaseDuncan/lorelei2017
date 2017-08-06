package edu.illinois.cs.cogcomp.lorelei.edl;

import edu.illinois.cs.cogcomp.lorelei.edl.LORELEIEDL;
import edu.illinois.cs.cogcomp.lorelei.edl.KBEntity;
import edu.illinois.cs.cogcomp.lorelei.edl.Jaro;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CoherenceTests{
  public static void main(String[] args){
    String test,kbPath,dbPath,candDoc,outputFile;
    for(String a : args)
      System.out.println(a);
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
      case "names":
        kbPath = args[1];
        dbPath = args[2];
        CoherenceTests.namesMapTest(kbPath,dbPath);
        break;
      case "jaro":
        candDoc=args[3];
        outputFile=args[4];
        CoherenceTests.jaroTest(candDoc,outputFile);
      default:
        System.out.println("Test must be specified.");
    }
  }

  private static void parseCandDocTest(String dbPath,
      String candDoc,
      String outputFile){
    LORELEIEDL edl = new LORELEIEDL(dbPath,candDoc,outputFile);
  }

  private static void entityMapTest(String kbPath, String dbPath){
    KBManager kb = new KBManager();
    kb.buildEntityMap(dbPath,kbPath);
  }

  private static void namesMapTest(String kbPath, String dbPath){
    KBManager kb = new KBManager();
    kb.buildNameToIDsMap(dbPath,kbPath);
  }

  private static void kbManagerTest(String dbPath){
    KBManager kb = new KBManager();
    kb.initializeEntityMap(dbPath);
    KBEntity entity = kb.getEntity(337996); 
    System.out.println(entity.toString());
  }

  private static void jaroTest(String candDoc, 
      String outputFile){
    BufferedReader br = null;
    String line;
    try{
      br = new BufferedReader(new FileReader(candDoc));
      while((line = br.readLine())!=null){
        String[] split = line.split("\t");
        String s1 = split[2];
        String s2 = split[4];
        String origJaroSim = split[6];
        System.out.println(String.format("%s\t%s\t%s\t%f",
              s1, s2, origJaroSim,
              Jaro.similarity(s1,s2)));
      }
    }catch(IOException e){
      e.printStackTrace();
    }
  }
}
