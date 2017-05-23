package edu.illinois.cs.cogcomp.ner;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.BasicTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotationUtilities;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.CoNLLNerReader;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.nlp.util.SimpleCachingPipeline;

import javax.xml.soap.Text;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * Created by mayhew2 on 6/7/16.
 */
public class Sandbox {
    public static void main(String[] args) throws IOException, AnnotatorException {

        CoNLLNerReader c = new CoNLLNerReader("/home/mayhew2/IdeaProjects/ner-annotation/data/eng-conll/");

        TextAnnotation ta = c.next();
        System.out.println(ta);

        System.out.println(ta.getSpansMatching("Somerset"));
        System.out.println(ta.getSpansMatching("Somerset"));
        System.out.println(ta.getSpansMatching("Somerset"));
        System.out.println(ta.getSpansMatching("Somerset"));

    }
}
