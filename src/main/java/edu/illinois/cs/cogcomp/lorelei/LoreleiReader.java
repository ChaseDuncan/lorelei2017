package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lorelei.xml.*;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import org.apache.log4j.BasicConfigurator;
import org.apache.xerces.dom.DeferredElementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * This converts LAF format into CONLL format. Only used for annotated entity files (not present in surprise languages)
 *
 * Created by mayhew2 on 2/15/16.
 */
@SuppressWarnings("Duplicates")
public class LoreleiReader {

    private static Logger logger = LoggerFactory.getLogger( LoreleiReader.class );

    private static List<String> excludetags = Arrays.asList("TTL", "NAN", "TIME");

    public static String makeline(String label, int start_offset, int end_offset, int ind, String word){
        return label+"\t"+ind+"\t"+start_offset+"\t"+end_offset+"\tO\t"+word+"\tx\tx\t0";
    }
    
    /**
     * This creates a CONLL formatted file from laf formatted twitter data.
     * @param outdir directory to write output file to
     * @throws IOException
     * @throws XMLException
     */
    public static void getTwitter(String nerdir, String dtdpath, String twdir, String outdir) throws IOException, XMLException {
        logger.debug("laf directory: " + nerdir);
        String[] laf_files = (new File(nerdir)).list();

        logger.debug("Number of laf files: " + laf_files.length);

        for(String laf_fname : laf_files) {
            logger.debug(laf_fname);

            Document laf = SimpleXMLParser.getDocument(new File(nerdir + laf_fname), dtdpath);

            HashMap<Integer, String> annotationmap = new HashMap<>();
            NodeList els = laf.getElementsByTagName("ANNOTATION");
            for(int i = 0 ; i < els.getLength(); i++){
                Node annotation = els.item(i);
                NamedNodeMap nnmap = annotation.getAttributes();

                String label;
                if(nnmap.getNamedItem("type") != null) {
                    label = nnmap.getNamedItem("type").getNodeValue();
                }else{
                    NodeList t = ((DeferredElementImpl) annotation).getElementsByTagName("TAG");
                    Node tag = t.item(0);
                    label = tag.getTextContent();
                }

                Node extent = annotation.getFirstChild();
                while(extent.getNodeName() != "EXTENT"){
                    extent = extent.getNextSibling();
                }

                NamedNodeMap extentmap = extent.getAttributes();

                int start_char = Integer.parseInt(extentmap.getNamedItem("start_char").getNodeValue());
                int end_char = Integer.parseInt(extentmap.getNamedItem("end_char").getNodeValue());

                if(!excludetags.contains(label)) {
                    annotationmap.put(start_char, "B-" + label);
                    for (int j = start_char + 1; j < end_char; j++) {
                        annotationmap.put(j, "I-" + label);
                    }
                }
            }

            // Get the document name from the file.
            NodeList doc = laf.getElementsByTagName("DOC");
            if (doc.getLength() != 1) {
                logger.error("WHAT IS WRONG WITH THIS DOCUMENT??? Has more than one doc tag.");
                continue;
            }
            Node doctag = doc.item(0);
            NamedNodeMap docmap = doctag.getAttributes();
            String docname = docmap.getNamedItem("id").getTextContent();
            String fulldoc = twdir + docname + ".rsd.txt";

            File tw_file = new File(fulldoc);
            if(!tw_file.exists()){
                logger.debug("skipping...");
                if(docname.startsWith("SN_TWT")){
                    System.out.printf("paruse");
                }
                continue;
            }

            List<String> conlllines = new ArrayList<>();
            conlllines.add("O\t0\t0\tO\t-X-\t-DOCSTART-\tx\tx\t0");

            // Get words from twitter file.
            String tweet = LineIO.slurp(fulldoc);
            String[] tweet_toks = tweet.split("\\s+");

            int startind = 0;
            for(String tok : tweet_toks){
                // can I get token start from each tok?
                int start_char = tweet.indexOf(tok, startind);

                // to deal with a weird bug that doesn't include @ symbols in offsets.
//                if(tok.startsWith("@")){
//                    start_char += 1;
//                }

                // use these to get the annotations
                String label = "O";
                if(annotationmap.containsKey(start_char)){
                    label = annotationmap.get(start_char);
                }


                conlllines.add(CoNLLNerReader.conllline(label, 0, tok));
                startind = start_char;
            }


            LineIO.write(outdir + docname + ".conll", conlllines);
        }

    }


    /**
     * This creates a CONLL formatted file from laf formatted data (excluding twitter data)
     * @param outdir directory to write output file to
     * @throws XMLException
     * @throws IOException
     */
    public static void readFiles(String nerdir, String dtdpath, String ltfdir, String outdir) throws XMLException, IOException {
        logger.debug("laf directory: " + nerdir);
        String[] laf_files = (new File(nerdir)).list();

        logger.debug("Number of laf files: " + laf_files.length);

        for(String laf_fname : laf_files) {

            if(laf_fname.startsWith("SN_TWT")){
                continue;
            }

            logger.debug(laf_fname);
            logger.debug(nerdir + laf_fname);


            Document laf = SimpleXMLParser.getDocument(new File(nerdir + laf_fname), dtdpath);

            HashMap<Integer, String> annotationmap = new HashMap<>();
            NodeList els = laf.getElementsByTagName("ANNOTATION");
            for(int i = 0 ; i < els.getLength(); i++){
                Node annotation = els.item(i);
                NamedNodeMap nnmap = annotation.getAttributes();

                String label;
                if(nnmap.getNamedItem("type") != null) {
                    label = nnmap.getNamedItem("type").getNodeValue();
                }else{
                    NodeList t = ((DeferredElementImpl) annotation).getElementsByTagName("TAG");
                    Node tag = t.item(0);
                    label = tag.getTextContent();
                }

                Node extent = annotation.getFirstChild();
                while(extent.getNodeName() != "EXTENT"){
                    extent = extent.getNextSibling();
                }

                NamedNodeMap extentmap = extent.getAttributes();

                int start_char = Integer.parseInt(extentmap.getNamedItem("start_char").getNodeValue());
                int end_char = Integer.parseInt(extentmap.getNamedItem("end_char").getNodeValue());

                if(!excludetags.contains(label)) {
                    annotationmap.put(start_char, "B-" + label);
                    for (int j = start_char + 1; j < end_char; j++) {
                        annotationmap.put(j, "I-" + label);
                    }
                }
            }


            // Get the document name from the file.
            NodeList doc = laf.getElementsByTagName("DOC");
            if (doc.getLength() != 1) {
                logger.error("WHAT IS WRONG WITH THIS DOCUMENT??? Has more than one doc tag.");
                continue;
            }
            Node doctag = doc.item(0);
            NamedNodeMap docmap = doctag.getAttributes();
            String docname = docmap.getNamedItem("id").getTextContent();
            if(docname.startsWith("doc-")){
                docname = laf_fname.split("\\.")[0];
            }

            String fulldoc = ltfdir + docname + ".ltf.xml";

            File ltf_file = new File(fulldoc);
            if(!ltf_file.exists()){
                logger.debug("skipping...");
                continue;
            }
            Document ltf = SimpleXMLParser.getDocument(ltf_file, dtdpath);

            List<String> conlllines = new ArrayList<>();
            conlllines.add("O\t0\t0\tO\t-X-\t-DOCSTART-\tx\tx\t0");

            // each segment is a sentence.
            NodeList segs = ltf.getElementsByTagName("SEG");
            for (int s = 0; s < segs.getLength(); s++) {
                Node seg = segs.item(s);
                NamedNodeMap segattr = seg.getAttributes();

                NodeList segchildren = seg.getChildNodes();
                for (int n = 0; n < segchildren.getLength(); n++) {
                    Node child = segchildren.item(n);

                    if (child.getNodeName() == "TOKEN") {

                        String word = child.getTextContent();
                        NamedNodeMap childattr = child.getAttributes();
                        String id = childattr.getNamedItem("id").getNodeValue();

                        String[] idsplit = id.split("-");
                        int tokenid = Integer.parseInt(idsplit[2]);

                        int start_char = Integer.parseInt(childattr.getNamedItem("start_char").getNodeValue());
                        int end_char = Integer.parseInt(childattr.getNamedItem("end_char").getNodeValue());

                        // use these to get the annotations
                        String label = "O";
                        if(annotationmap.containsKey(start_char)){
                            label = annotationmap.get(start_char);
                        }
                        String outline = makeline(label, start_char, end_char, tokenid, word);
                        conlllines.add(outline);
                    }
                }

                // empty line between sentences.
                conlllines.add("");
            }

            LineIO.write(outdir + docname + ".conll", conlllines);



        }

    }

    public static void main(String[] args) throws XMLException, IOException {
        BasicConfigurator.configure();

        String loreleidir = "/shared/corpora/corporaWeb/lorelei/";

        //static String basedir = "/shared/corpora/corporaWeb/lorelei/LDC2015E70_BOLT_LRL_Hausa_Representative_Language_Pack_V1.2/";
        //static String basedir = "/shared/corpora/corporaWeb/lorelei/LDC2016E29_BOLT_LRL_Uzbek_Representative_Language_Pack_V1.0/";
        //static String basedir = "/shared/corpora/corporaWeb/lorelei/LDC2014E115_BOLT_LRL_Turkish_Representative_Language_Pack_V2.2/";
        //static String basedir = "/shared/corpora/corporaWeb/lorelei/20150908-kickoff-release/BOLT_Hausa_RL_LDC2015E70_V1.1/";
        //static String basedir = "/shared/corpora/corporaWeb/lorelei/uzbek/";

//        String basedir = loreleidir + "evaluation-20160705/LDC2016E70_LoReHLT_IL3_Incident_Language_References_for_Year_1_Eval_Unsequestered_V1.1/setE/";
//        String origbasedir = loreleidir + "evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/setE/";

        String monolingualbasedir = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E86_LORELEI_Amharic_Representative_Language_Pack_Monolingual_Text_V1.1/";
        String basedir = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E87_LORELEI_Amharic_Representative_Language_Pack_Translation_Annotation_Grammar_Lexicon_and_Tools_V2.0/";

        // This folder contains LAF files.
        String nerdir =  basedir + "data/annotation/entity/simple/";

        // This folder contains LTF files, corresponding to the LAF files in nerdir.
        String ltfdir = monolingualbasedir + "data/monolingual_text/zipped/ltf/";

        String twdir = monolingualbasedir + "tools/twitter-processing/twitter-data/";

        String dtdpath = "/shared/corpora/corporaWeb/lorelei/data/LDC2016E87_LORELEI_Amharic_Representative_Language_Pack_Translation_Annotation_Grammar_Lexicon_and_Tools_V2.0/dtds/";

        // Directory where conll output files will be written.
        String outdir = "/shared/corpora/ner/lorelei/am/All-offsets/";

        readFiles(nerdir, dtdpath, ltfdir, outdir);
//        getTwitter(nerdir, dtdpath, twdir, outdir);

    }

}
