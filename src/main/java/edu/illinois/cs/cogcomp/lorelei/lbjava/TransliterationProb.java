// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B2A4D4CC158092A4CCB2EC9CC294D2A4C29CCCFCB082ACF42D80FCF2A49084CCC225820D450B1D558A6582A4D292D2AC35820DB21455D50045D6DA05B0002B87D61A94000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class TransliterationProb extends Classifier
{
  public TransliterationProb()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "TransliterationProb";
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
      System.err.println("Classifier 'TransliterationProb(WordPair)' defined on line 29 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    WordPair p = (WordPair) __example;

    return p.transliterationprob;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'TransliterationProb(WordPair)' defined on line 29 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "TransliterationProb".hashCode(); }
  public boolean equals(Object o) { return o instanceof TransliterationProb; }
}

