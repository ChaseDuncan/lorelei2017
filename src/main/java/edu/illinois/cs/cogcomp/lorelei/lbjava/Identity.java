// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B49CC2E4E2A4D294DA65A4B4CC92E4552D1505A292A2D455AA550FC494DCB29CC29A4D80FCF2A49084CCC225820D450B1D558A6500AAF2D2AC35820DB2A4D4B270A4AE5A61692053B6418E1887A96DA05B00342790C265000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class Identity extends Classifier
{
  public Identity()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "Identity";
  }

  public String getInputType() { return "edu.illinois.cs.cogcomp.ner.WordPair"; }
  public String getOutputType() { return "discrete"; }

  private static String[] __allowableValues = DiscreteFeature.BooleanValues;
  public static String[] getAllowableValues() { return __allowableValues; }
  public String[] allowableValues() { return __allowableValues; }


  public FeatureVector classify(Object __example)
  {
    return new FeatureVector(featureValue(__example));
  }

  public Feature featureValue(Object __example)
  {
    String result = discreteValue(__example);
    return new DiscretePrimitiveStringFeature(containingPackage, name, "", result, valueIndexOf(result), (short) allowableValues().length);
  }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof WordPair))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'Identity(WordPair)' defined on line 76 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    String __cachedValue = _discreteValue(__example);

    if (valueIndexOf(__cachedValue) == -1)
    {
      System.err.println("Classifier 'Identity' defined on line 76 of NamePairClassifier.lbj produced '" + __cachedValue  + "' as a feature value, which is not allowable.");
      System.exit(1);
    }

    return __cachedValue;
  }

  private String _discreteValue(Object __example)
  {
    WordPair p = (WordPair) __example;

    return "" + (p.refword.equals(p.word));
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'Identity(WordPair)' defined on line 76 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "Identity".hashCode(); }
  public boolean equals(Object o) { return o instanceof Identity; }
}

