package edu.illinois.cs.cogcomp.lorelei.edl;

import edu.illinois.cs.cogcomp.lorelei.edl.KBEntity;

import org.mapdb.DBMaker;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.mapdb.BTreeMap;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
* Wraps functionality for querying the KB.
**/
public class KBManager{
  /**
  * @param dbPath path to MapDB file to either load
  *               or write to.
  **/
  public KBManager(String dbPath){
    this.dbPath = dbPath;

    if(new File(dbPath).isFile()){
      db = DBMaker
        .fileDB(dbPath)
        .closeOnJvmShutdown()
        .make();
      entityMap = db.treeMap("LORELEI-kb")
        .keySerializer(Serializer.STRING)
        .valueSerializer(Serializer.STRING)
        .open();
    }
  }

  public KBEntity getEntity(String id){
    return _getEntity(id);
  }

  public KBEntity getEntity(int id){
    return _getEntity(String.valueOf(id));
  }

  private KBEntity _getEntity(String id){
    return new KBEntity(entityMap.get(id).split("\t"));
  }
  
  public void buildEntityMap(String kbPath){
    db = DBMaker
      .fileDB(dbPath)
      .closeOnJvmShutdown()
      .make();
    entityMap = db.treeMap("LORELEI-kb")
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .createOrOpen();
      try{
        _buildEntityMap(kbPath);  
      }catch(IOException e){
        e.printStackTrace();
      }
  }
  
  private void _buildEntityMap(String kbPath) throws IOException{
    BufferedReader br = null;
    try{
      br = new BufferedReader(new FileReader(kbPath));
    } catch (FileNotFoundException e){
      e.printStackTrace();
    }
    String line;
    int count = 0;
    int total = 11480854; 
    long start = System.currentTimeMillis();
    while((line = br.readLine())!=null){
      String id = line.split("\t")[0];
      //System.out.println(id);
      entityMap.put(id,line);
      count++;
      if(count % 1000 == 0)
        System.out.println(count+" of " + total + " processed.");
    }
    db.commit();
    db.close(); 

    long end = System.currentTimeMillis();
    long execTime = (end - start) / 1000;
    System.out.println("Processing took " + execTime + " seconds.");
  }

  String dbPath = null;
  DB db = null;
  BTreeMap<String, String> entityMap = null;
}
