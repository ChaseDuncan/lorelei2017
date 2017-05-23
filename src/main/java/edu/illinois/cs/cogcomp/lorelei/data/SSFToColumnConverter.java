package edu.illinois.cs.cogcomp.ner.data;

import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converter for the SSF format of the IJCAI 08 Workshop on South and South East Asian Languages.
 *
 * @author Christos Christodoulopoulos
 */
@SuppressWarnings("Duplicates")
public class SSFToColumnConverter {
    /* Source format:
<Sentence id="209">
0	((	SSF
1	۱۰۔
2	عظیم
3	مغل
4	تاجدار
5	و
6	((	NP	<ne=NEP>
6.1	((	NP	<ne=NED>
6.1.1	شہنشاہ
	))
6.2	((	NP	<ne=NEP>
6.2.1	اکبر
	))
	))
7	نے
8	((	NP	<ne=NETI>
8.1	۱۵۶۵
	))
9	؁ء
10	میں
11	((	NP	<ne=NEL>
11.1	آگرہ
	))
12	کے
13	قلعہ
14	کی
15	تعمیر
16	شروع
17	کرائی
18	تھی۔
	))
</Sentence>
     */

    /* Target format:
O	0	0	O	-X-	-DOCSTART-	x	x	0

O	0	0	I-NP	NNP	Rare	x	x	0
B-PER	0	1	I-NP	NNP	Hendrix	x	x	0
O	0	2	I-NP	NN	song	x	x	0
O	0	3	I-NP	NN	draft	x	x	0
O	0	4	I-VP	VBZ	sells	x	x	0
O	0	5	I-PP	IN	for	x	x	0
O	0	6	I-NP	RB	almost	x	x	0
O	0	7	I-NP	$	$	x	x	0
O	0	8	I-NP	CD	17,000	x	x	0
O	0	9	O	.	.	x	x	0
     */

    public static void urdu(String fname) throws IOException {
        List<String> outLines = new ArrayList<>();
        outLines.add("O\t0\t0\tO\t-X-\t-DOCSTART-\tx\tx\t0");
        outLines.add("");

        String neTag = "";
        boolean foundNE = false;
        boolean beginNE = false;
        int opened = 0;
        int wordIndex = 0;
        for (String line : LineIO.read(fname)) {
            if (line.isEmpty()) {
                wordIndex = 0;
                opened = 0;
                neTag = "";
                foundNE = false;
                beginNE = false;
                outLines.add("");
            }
            if (line.startsWith("<Sentence") || line.startsWith("0")) continue;
            // Found a NE
            if (line.contains("NP")) {
                ++opened;
                if (line.matches("^[0-9]+\\..*")) continue;
                foundNE = true;
                beginNE = true;
                neTag = line.substring(line.indexOf('=') + 1, line.indexOf('>'));
            }
            else if (line.contains("))") && (--opened)==0) foundNE = false;
            else if (line.matches("^[0-9].*")) {
                // NB: Ignore embedded NEs
                String word = line.split("\\s+")[1];
                String neTagString;
                if (foundNE) {
                    neTagString = ((beginNE) ? "B-" : "I-") + neTag;
                    beginNE = false;
                }
                else neTagString = "O";
                outLines.add(neTagString + "\t0\t" + wordIndex + "\tx\tx\t" + word + "\tx\tx\t0");
                wordIndex++;
            }
        }
        String out = IOUtils.stripFileExtension(fname) + "-conll.txt";
        LineIO.write(out, outLines);
        System.out.println("wrote: " + out);
    }

    /**
     * Read file from NERSSEAL and convert to CoNLL format. Use this for bengali files.
     * @param fname name of NERSSEAL file
     * @throws IOException
     */
    public static void bengali(String fname) throws IOException {
        List<String> outLines = new ArrayList<>();
        outLines.add("O\t0\t0\tO\t-X-\t-DOCSTART-\tx\tx\t0");
        outLines.add("");

        String neTag = "";
        boolean foundNE = false;
        boolean beginNE = false;
        int opened = 0;
        int wordIndex = 0;
        for (String line : LineIO.read(fname)) {
            if (line.isEmpty()) {
                wordIndex = 0;
                opened = 0;
                neTag = "";
                foundNE = false;
                beginNE = false;
                outLines.add("");
            }
            if (line.startsWith("<Sentence") || line.startsWith("0")) continue;
            // Found a NE
            if (line.contains("((")) {
                ++opened;
                if (line.matches("^[0-9]+\\..*")) continue;
                if(line.contains("ne=")){
                    foundNE = true;
                    beginNE = true;
                    neTag = line.substring(line.indexOf('=') + 1, line.indexOf('>'));
                }
            }
            else if (line.contains("))") && (--opened)==0) foundNE = false;
            else if (line.matches("^[0-9].*")) {
                // NB: Ignore embedded NEs
                String word = line.split("\\s+")[1];
                String postag = "x";
                if(line.split("\\s+").length > 2) {
                    postag = line.split("\\s+")[2];
                }
                String neTagString;
                if (foundNE) {
                    neTagString = ((beginNE) ? "B-" : "I-") + neTag;
                    beginNE = false;
                }
                else neTagString = "O";

                // ignore sentence start markers.
                if(word.matches("[0-9]*")) continue;

                outLines.add(neTagString + "\t0\t" + wordIndex + "\tx\t" + postag + "\t" + word + "\tx\tx\t0");
                wordIndex++;
            }
        }
        String out = IOUtils.stripFileExtension(fname) + "-conll.txt";
        LineIO.write(out, outLines);
        System.out.println("wrote: " + out);
    }


    public static void main(String[] args) throws IOException {

        //String fname = args[0];
        String dir = "/shared/corpora/ner/nersseal";

        //String fname = dir + "/bengali/test-data-bengali.txt";

        urdu(dir + "/urdu/training-urdu/AnnotatedCorpus-CRULP.txt");
        urdu(dir + "/urdu/training-urdu/Tr_0001_Ur_IIITA.txt");
        urdu(dir + "/urdu/training-urdu/Tr_0002_Ur_IIITA.txt");
        urdu(dir + "/urdu/training-urdu/Tr_0003_Ur_IIITA.txt");
        urdu(dir + "/urdu/training-urdu/Tr_0004_Ur_IIITA.txt");
        urdu(dir + "/urdu/test-data-urdu.txt");

        bengali(dir + "/bengali/test-data-bengali.txt");

        for(File f : new File(dir + "/bengali/training-bengali").listFiles()){
            if(f.getName().contains("conll.txt")) continue;
            bengali(f.getAbsolutePath());
        }

    }
}
