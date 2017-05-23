// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000D7E81BA02C040144F756ACD0A60535AADB5980616D762794E04FE2CED684129F777FC82A5885D2CCCECCB16B6EA8343C6E65B5321B0A370EAE86C13ABC0BD50EE8DB98DE9CA0268E9B4B56B6832670F6787B349D605706093F207AED273A76B8E276B5F0A579F5DAF64A5AC0B0CA3973BD1A8C4BAFBA23735554F944CEF2158D9F68CDC3504FF592322124288E6E726E736FF23D5D0AF6E691CB817E32DB27FB8C9273155B565967C806B2D3BF94655E105320C8C884100000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class Ngramfeats3 extends Classifier
{
  public Ngramfeats3()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "Ngramfeats3";
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
      System.err.println("Classifier 'Ngramfeats3(WordPair)' defined on line 58 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    WordPair p = (WordPair) __example;

    HashSet sourcechars = new HashSet();
    for (int i = 0; i < p.refword.length() - 2; i += 3)
    {
      sourcechars.add(p.refword.substring(i, i + 3));
    }
    int total = 0;
    for (int i = 0; i < p.word.length() - 2; i += 3)
    {
      if (sourcechars.contains(p.word.substring(i, i + 3)))
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
      System.err.println("Classifier 'Ngramfeats3(WordPair)' defined on line 58 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "Ngramfeats3".hashCode(); }
  public boolean equals(Object o) { return o instanceof Ngramfeats3; }
}

