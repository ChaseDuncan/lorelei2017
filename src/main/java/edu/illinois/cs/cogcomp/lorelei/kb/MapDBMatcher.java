package edu.illinois.cs.cogcomp.lorelei.kb;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by mayhew2 on 7/28/17.
 */
public class MapDBMatcher {

    HTreeMap<Integer, String> namemap;

    private static Logger logger = LoggerFactory.getLogger(MapDBMatcher.class);

    public MapDBMatcher() throws IOException {

        boolean readonly = true;

        DB db;
        DB namedb;
        HTreeMap<String, int[]> map;
        if(readonly){
            db = DBMaker.fileDB(new File("/shared/experiments/mayhew2/indices/ngrams.db"))
                    .fileMmapEnable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();

            map = db
                    .hashMap("map")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.INT_ARRAY)
                    .open();

            namedb = DBMaker.fileDB(new File("/shared/experiments/mayhew2/indices/namemap.db"))
                    .fileMmapEnable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();

            namemap = namedb.hashMap("map")
                    .keySerializer(Serializer.INTEGER)
                    .valueSerializer(Serializer.STRING)
                    .open();

        }else{

            File ngramfile = new File("/shared/experiments/mayhew2/indices/ngrams.db");
            if(ngramfile.exists()){
                logger.error("DB exists! "+ngramfile);
                System.exit(-1);
            }

            File namemapfile = new File("/shared/experiments/mayhew2/indices/namemap.db");
            if(namemapfile.exists()){
                logger.error("DB exists! "+namemap);
                System.exit(-1);
            }

            db = DBMaker.fileDB(ngramfile)
                    .fileMmapEnable()
                    .closeOnJvmShutdown()
                    .make();

            map = db
                    .hashMap("map")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.INT_ARRAY)
                    .create();

            namedb = DBMaker.fileDB(namemapfile)
                    .fileMmapEnable()
                    .closeOnJvmShutdown()
                    .make();

            namemap = namedb.hashMap("map")
                    .keySerializer(Serializer.INTEGER)
                    .valueSerializer(Serializer.STRING)
                    .create();
        }

        String kbpath = "/shared/corpora/cddunca2/allCountriesHeader.txt";

        //buildindex(kbpath, map);
        //makenamemap(kbpath);

        testindex(map);

        //stringmatcher("/shared/corpora/edl/lorelei/amh-anno-all.txt", map);

        db.close();
        namedb.close();

    }

    /**
     * Returns all broken down terms for the mention (ngrams, as well as tokens, as well the full string)
     * @param mention
     * @return
     */
    private List<String> getallterms(String mention){
        List<String> ngrams = new ArrayList<>();

        int startngram = 2;
        int endngram = 5;

        // add exact match
        ngrams.add(mention);
        // add all terms
        ngrams.addAll(Arrays.asList(mention.split(" ")));

        for(int i = startngram; i < endngram+1; i++){
            ngrams.addAll(KBStringMatcher.getngrams(mention, i));
        }

        List<String> ret = ngrams.stream()
                .sorted(Comparator.comparing(String::length).reversed())
                .collect(Collectors.toList());

        return ret;
    }

    public void buildindex(String kbpath, ConcurrentMap<String, int[]> map) throws IOException {

        long startTime = System.nanoTime();
        long currTime = startTime;
        long prevTime;

        int j = 0;

        HashMap<String, TIntHashSet> localmap = new HashMap<>();


        try (BufferedReader br = new BufferedReader(new FileReader(kbpath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                if(j == 0){
                    j++;
                    continue;
                }

                if (j % 1000 == 0) {
                    prevTime = currTime;
                    currTime = System.nanoTime();
                    long last = (currTime - prevTime) / 1000000;
                    long elapsed = (currTime - startTime)/ 1000000;
                    double average = elapsed / (j);
                    System.out.println("Progress: " + j + "\t" + last + "\t" + elapsed + "\t" + average);
                }
                j++;

                String[] sline = line.split("\t");

                int entityid = Integer.parseInt(sline[0]);
                String name = sline[1];
                String asciiname = sline[2];
                String altnames = sline[3];

                List<String> ngrams = getallterms(asciiname);


                // ngrams is sorted
                for(String ngram : ngrams){
                    TIntHashSet ids = localmap.getOrDefault(ngram, new TIntHashSet());

                    ids.add(entityid);
                    localmap.put(ngram, ids);
                }

                //if(j > 100000){break;}
            }
        }

        float total = (float) localmap.size();
        j = 0;
        for(String ng : localmap.keySet()){
            if(j%1000 == 0){
                System.out.println("Progress: " + j / total);
            }

            TIntHashSet ints = localmap.get(ng);
            int[] intarray = ints.toArray(new int[ints.size()]);

            map.put(ng, intarray);
            j++;
        }


    }

    /**
     * This function builds the namemap. Can be run completely separately from building the ngrams.
     */
    public void makenamemap(String kbpath) throws IOException {
        HashMap<Integer, String> localnamemap = new HashMap<>();

        float total;
        int j = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(kbpath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                if (j == 0) {
                    j++;
                    continue;
                }
                String[] sline = line.split("\t");

                int entityid = Integer.parseInt(sline[0]);
                String name = sline[1];
                String asciiname = sline[2];
                String altnames = sline[3];

                localnamemap.put(entityid, asciiname);
            }
        }

        total = (float) localnamemap.size();
        j = 0;
        for(Integer id : localnamemap.keySet()){
            if(j%1000 == 0){
                System.out.println("Progress: " + j / total);
            }

            String asciiname = localnamemap.get(id);
            namemap.put(id, asciiname);
            j++;
        }
    }

    public LinkedHashMap<Integer, Float> retrieve(String query, ConcurrentMap<String, int[]> map, int numcands){
        List<String> ngrams = getallterms(query);

        HashMap<Integer, Float> ret = new HashMap<>();

        List<String> mentionngrams = KBStringMatcher.getngrams(query, 2);

        // ngrams is sorted, so the longest are first.
        for(String ngram : ngrams){
            int[] results = map.get(ngram);
            if(results == null) continue;
            for(int entityid : results){
                String candsurf = namemap.get(entityid);

                List<String> candngrams = KBStringMatcher.getngrams(candsurf, 2);
                float score = KBStringMatcher.jaccard(new HashSet<>(candngrams), new HashSet<>(mentionngrams));

                float i = ret.getOrDefault(entityid, score);
                ret.put(entityid, i+score);
            }

            // crucially, stop looking for more cands if we have reached the limit.
            if(ret.keySet().size() > numcands){
                break;
            }
        }


        LinkedHashMap<Integer, Float> result = ret.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(numcands)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return result;
    }

    public void testindex(ConcurrentMap<String, int[]> map) throws IOException {

        BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));

        String s = "";
        while (!s.equalsIgnoreCase("q")) {
            try {
                System.out.println("Enter the search query (q=quit): ");
                s = br.readLine();
                if (s.equalsIgnoreCase("q")) {
                    break;
                }

                // do retrieval here...
                LinkedHashMap<Integer,Float> ret = retrieve(s, map, 20);

                for(Integer i : ret.keySet()){
                    System.out.println(i + "\t" + namemap.get(i) + "\t" + ret.get(i));
                }


            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error searching " + s + " : " + e.getMessage());
            }
        }

        //System.out.println(map.keySet());
    }

    /**
     * This takes a submission file and links all the entries.
     * @param subfile
     */
    public void stringmatcher(String subfile, ConcurrentMap<String, int[]> map) throws IOException {

        List<String> lines = LineIO.read(subfile);
        ArrayList<String> outlines = new ArrayList<>();
        ArrayList<String> outlines2 = new ArrayList<>();

        int numcands = 10;
        double i = 0;
        int coverage = 0;
        int nils = 0;

        for(String line : lines){
            if(i % 10 == 0){
                System.out.println("Progress: " + (i / lines.size()));
            }

            i++;

            String[] sline = line.split("\t");

            int goldid;
            try{
                goldid = Integer.parseInt(sline[4].trim());
            }catch(NumberFormatException e){
                goldid = -1;
            }

            String mention = sline[2];
            ArrayList<String> mentionngrams = KBStringMatcher.getngrams(mention, 2);

            // this maps index to frequency.
            LinkedHashMap<Integer,Float> ret = retrieve(mention, map, numcands);

            List<Integer> result = ret.entrySet().stream()
                    .map((e) -> e.getKey())
                    .collect(Collectors.toList());

            int best = result.get(0);

            if(ret.keySet().contains(goldid)){
                coverage++;
            }

            sline[4] = best + "";
            sline[5] = namemap.get(best);

            outlines.add(StringUtils.join(sline, "\t"));

            String candstring = StringUtils.join(result.stream()
                    .map((r) -> namemap.get(r) + ":" + r)
                    .collect(Collectors.toList()), ",");

            sline[4] = candstring;
            sline[5] = "NAM";
            outlines2.add(StringUtils.join(sline, "\t"));
        }

        System.out.println("Coverage: " + coverage/((float)lines.size()-nils));

        LineIO.write(subfile + ".linked", outlines);
        LineIO.write(subfile + ".cands" + numcands, outlines2);
    }

    public static void main(String[] args) throws IOException {
        MapDBMatcher n = new MapDBMatcher();
    }
}
