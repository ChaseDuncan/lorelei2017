// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000D7E8DBE02C030138F55C3622865495B47762484C0CC1AD4BD84094579BAA30AEBB371E744704C4729D76F762B6EA834746E6DA53C17BAEC18A93A17481436791EE8DB98DF9C2326819A6B57F6822A287B3D7C15AB44B180AC976831F635AC9D168C9C6B394D5E75BEB3EE5964682297BAA0B5D25DB8ACCD43D8AF622E8798C4EC77ACDA3504EF58233212830BC6E726E736FF23D5B05B4E6D1C3B17E3AA77EE71935EE54596922DE891469742FF2954970BD7BD2F584100000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class Ngramfeats2 extends Classifier
{
  public Ngramfeats2()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "Ngramfeats2";
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
      System.err.println("Classifier 'Ngramfeats2(WordPair)' defined on line 40 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    WordPair p = (WordPair) __example;

    HashSet sourcechars = new HashSet();
    for (int i = 0; i < p.refword.length() - 1; i += 2)
    {
      sourcechars.add(p.refword.substring(i, i + 2));
    }
    int total = 0;
    for (int i = 0; i < p.word.length() - 1; i += 2)
    {
      if (sourcechars.contains(p.word.substring(i, i + 2)))
      {
        total += 1;
      }
    }
    return total;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'Ngramfeats2(WordPair)' defined on line 40 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "Ngramfeats2".hashCode(); }
  public boolean equals(Object o) { return o instanceof Ngramfeats2; }
}

