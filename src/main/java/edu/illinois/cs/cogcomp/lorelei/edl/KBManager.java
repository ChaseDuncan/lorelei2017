package edu.illinois.cs.cogcomp.lorelei.edl;

import edu.illinois.cs.cogcomp.lorelei.edl.KBEntity;

import org.mapdb.DBMaker;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArrayTuple;
import org.mapdb.BTreeMap;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.NavigableSet;

/**
* Wraps functionality for querying the KB.
**/
public class KBManager{
  /**
  * @param dbPath path to MapDB file to either load
  *               or write to.
  **/
  public void initializeEntityMap(String dbPath){
    if(new File(dbPath).isFile()){
      entityDB = DBMaker
        .fileDB(dbPath)
        .closeOnJvmShutdown()
        .make();
      entityMap = entityDB.treeMap("LORELEI-kb")
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
    if(entityMap== null)
      System.out.println("entityMap is null");
    String e;
    if((e = entityMap.get(id))!=null)
      return new KBEntity(e.split("\t"));
    return null;
  }
  
  public void buildEntityMap(String dbPath, String kbPath){
    entityDB = DBMaker
      .fileDB(dbPath)
      .closeOnJvmShutdown()
      .make();
    entityMap = entityDB.treeMap("LORELEI-kb")
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
    entityDB.commit();
    entityDB.close(); 

    long end = System.currentTimeMillis();
    long execTime = (end - start) / 1000;
    System.out.println("Processing took " + execTime + " seconds.");
  }
  //multimap.add(new Object[]{"John",1});
  //multimap.add(new Object[]{"John",2});
  //multimap.add(new Object[]{"Anna",1});
  //
  //// print all values associated with John:
  //Set johnSubset = multimap.subSet(
  //new Object[]{"John"},         // lower interval bound
  //new Object[]{"John", null});

  public void buildNameToIDsMap(String dbPath, String kbPath) {
    nameToIDsDB = DBMaker
      .fileDB(dbPath)
      .closeOnJvmShutdown()
      .make();
    nameToIds = nameToIDsDB.treeSet("name-to-ids")
    .serializer(new SerializerArrayTuple(Serializer.STRING, Serializer.INTEGER))
    .createOrOpen();
    try{
    _buildNameToIDsMap(kbPath);    
    } catch (IOException e){
      e.printStackTrace();
    }
  }

  public void _buildNameToIDsMap(String kbPath) throws IOException{
    BufferedReader br = null;
    try{
      br = new BufferedReader(new FileReader(kbPath));
    } catch (FileNotFoundException e){
      e.printStackTrace();
    }
    String line;
    int count = 0;
    double total = 11480854.0; 
    long start = System.currentTimeMillis();
    while((line = br.readLine())!=null){
      String[] split = line.split("\t");
      String id = line.split("\t")[0];
      // don't forget alternate names
      // only latin chars split[1].matches("\\p{L}+")
      int intID = Integer.parseInt(split[0]);
      nameToIds.add(new Object[]{split[1],intID});
      for(String altName : split[3].split(","))
        nameToIds.add(new Object[]{altName,intID});
      count++;
      if(count % 1000 == 0)
        System.out.println((count/total)*100 + " processed.");
    }
    nameToIDsDB.commit();
    nameToIDsDB.close(); 

    long end = System.currentTimeMillis();
    long execTime = (end - start) / 1000;
    System.out.println("Processing took " + execTime + " seconds.");

  }

  String dbPath = null;
  DB entityDB = null;
  DB nameToIDsDB = null;
  BTreeMap<String, String> entityMap = null;
  NavigableSet<Object[]> nameToIds = null;
}
