// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B2A4D4CC15550FB4CCD4D084CCC227EC94C2E2ECC4BCC4D22515134D80FCF2A41098A24186A28D8EA24841526E517E46694A615269466E7E50415E729E0450194000FE6C0B6B15000000

package edu.illinois.cs.cogcomp.ner.lbjava;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.ner.*;
import java.util.*;


public class NamePairClassifier$$1 extends Classifier
{
  private static final TransliterationProb __TransliterationProb = new TransliterationProb();
  private static final TranslationProb __TranslationProb = new TranslationProb();

  public NamePairClassifier$$1()
  {
    containingPackage = "edu.illinois.cs.cogcomp.ner.lbjava";
    name = "NamePairClassifier$$1";
  }

  public String getInputType() { return "edu.illinois.cs.cogcomp.ner.WordPair"; }
  public String getOutputType() { return "real%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof WordPair))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'NamePairClassifier$$1(WordPair)' defined on line 90 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    FeatureVector __result;
    __result = new FeatureVector();
    __result.addFeature(__TransliterationProb.featureValue(__example));
    __result.addFeature(__TranslationProb.featureValue(__example));
    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof WordPair[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'NamePairClassifier$$1(WordPair)' defined on line 90 of NamePairClassifier.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "NamePairClassifier$$1".hashCode(); }
  public boolean equals(Object o) { return o instanceof NamePairClassifier$$1; }

  public java.util.LinkedList getCompositeChildren()
  {
    java.util.LinkedList result = new java.util.LinkedList();
    result.add(__TransliterationProb);
    result.add(__TranslationProb);
    return result;
  }
}

