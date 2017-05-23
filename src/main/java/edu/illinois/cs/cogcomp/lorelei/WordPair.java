package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;

/**
 *
 * This represents a single instance to be classified. The refword is usually English, and the word is the candidate.
 * If the refword should be aligned to the word, then match is True.
 *
 * Created by mayhew2 on 5/5/16.
 */
public class WordPair {

    public String fileid;
    public Pair<Integer, Integer> refspan;
    public String refword;
    public String reflabel;
    public Pair<Integer, Integer> span;
    public String word;
    public Double translationprob;
    public Double transliterationprob;
    public boolean match;

    public WordPair(String fileid, Pair<Integer, Integer> refspan, String refword, String reflabel, Pair<Integer, Integer> span, String word, double translationprob, double transliterationprob){
        this.fileid = fileid;
        this.refspan = refspan;
        this.refword = refword;
        this.reflabel = reflabel;
        this.span = span;
        this.word = word;
        this.translationprob = translationprob;
        this.transliterationprob = transliterationprob;
    }
    
    public void setMatch(boolean b){
        this.match = b;
    }

    /**
     * This is often used as a unique key for this WordPair
     * @return
     */
    public String getKey(){
        return String.format("%s-%s-%s-%s", fileid, reflabel, refspan, refword);
    }

    @Override
    public String toString() {
        return "WordPair{" +
                "refspan=" + refspan +
                ", refword='" + refword + '\'' +
                ", reflabel='" + reflabel + '\'' +
                ", span=" + span +
                ", word='" + word + '\'' +
                ", match=" + match +
                '}';
    }
}
