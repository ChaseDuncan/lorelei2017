// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B2A4D4CC158092A4CCB2EC94C29CCCFCB082ACF42D80FCF2A49084CCC225820D450B1D558A6582A4D292D2AC35820DB2148AC200AA4B658A50000371697614000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class TranslationProb extends Classifier
{
  public TranslationProb()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "TranslationProb";
  }

  public String getInputType() { return "edu.illinois.cs.cogcomp.ner.WordPair"; }
  public String getOutputType() { return "real"; }


  public FeatureVector classify(Object __example)
  {
    return new FeatureVector(featureValue(__example));
  }

  public Feature featureValue(Object __example)
  {
    double result = realValue(__example);
    return new RealPrimitiveStringFeature(containingPackage, name, "", result);
  }

  public double realValue(Object __example)
  {
    if (!(__example instanceof WordPair))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'TranslationProb(WordPair)' defined on line 24 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    WordPair p = (WordPair) __example;

    return p.translationprob;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'TranslationProb(WordPair)' defined on line 24 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "TranslationProb".hashCode(); }
  public boolean equals(Object o) { return o instanceof TranslationProb; }
}

