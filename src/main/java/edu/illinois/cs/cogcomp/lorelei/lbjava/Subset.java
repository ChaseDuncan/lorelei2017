// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000D4A814A008024150FA2F075905E50AE0114B86D69F3012C4EBA4B84FEE915B86733CC81B1656A84798D4FE1844B011931982892D2182633F1C664D691E5268E071AED98D1CB26ADECA15DA78B8ADAB0D875F8B44EC871FF9EBB56F8273FA7F34E947000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class Subset extends Classifier
{
  public Subset()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "Subset";
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
      System.err.println("Classifier 'Subset(WordPair)' defined on line 81 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    String __cachedValue = _discreteValue(__example);

    if (valueIndexOf(__cachedValue) == -1)
    {
      System.err.println("Classifier 'Subset' defined on line 81 of NamePairClassifier.lbj produced '" + __cachedValue  + "' as a feature value, which is not allowable.");
      System.exit(1);
    }

    return __cachedValue;
  }

  private String _discreteValue(Object __example)
  {
    WordPair p = (WordPair) __example;

    return "" + (p.refword.contains(p.word) || p.word.contains(p.refword));
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'Subset(WordPair)' defined on line 81 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "Subset".hashCode(); }
  public boolean equals(Object o) { return o instanceof Subset; }
}

