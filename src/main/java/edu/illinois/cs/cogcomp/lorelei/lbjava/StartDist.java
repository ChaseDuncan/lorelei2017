// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B2A4D4CC1580E294C2A2179CC2E21D80FCF2A49084CCC225820D450B1D558A6582A4D292D2AC350D5FD4C29C0DB4C4A26D820DB2E284CC3DB4F4D217BCC220AE0D450D55820DB2A4D434316D4B658A50005385A3AFD5000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class StartDist extends Classifier
{
  public StartDist()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "StartDist";
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
      System.err.println("Classifier 'StartDist(WordPair)' defined on line 10 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    WordPair p = (WordPair) __example;

    return -Math.abs(p.span.getFirst() - p.refspan.getFirst());
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'StartDist(WordPair)' defined on line 10 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "StartDist".hashCode(); }
  public boolean equals(Object o) { return o instanceof StartDist; }
}

