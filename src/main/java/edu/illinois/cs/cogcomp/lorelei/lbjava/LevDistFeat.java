// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B2A4D4CC150F94D2379CC2E217B4D4C21D80FCF2A49084CCC225820D450B1D558A6582A4D292D2AC350D50A2A4DCB2E294DCCC309AD4CCB4E45DB4F4D21C415D820DB2A4D4B270A13A3A050A702A53514F51C731B4234F2731B2012BA7939A979E5291A1A903560710D4B658A5009247212D89000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class LevDistFeat extends Classifier
{
  public LevDistFeat()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "LevDistFeat";
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
      System.err.println("Classifier 'LevDistFeat(WordPair)' defined on line 19 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    WordPair p = (WordPair) __example;

    return -LevensteinDistance.getLevensteinDistance(p.refword, p.word) / Math.max(p.refword.length(), p.word.length());
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'LevDistFeat(WordPair)' defined on line 19 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "LevDistFeat".hashCode(); }
  public boolean equals(Object o) { return o instanceof LevDistFeat; }
}

