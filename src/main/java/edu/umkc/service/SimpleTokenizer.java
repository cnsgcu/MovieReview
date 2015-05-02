package edu.umkc.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleTokenizer implements Serializable
{
    private int minWordLength = 1;

    private Set<String> stopwords;
    private StanfordCoreNLP coreNLP;

    public SimpleTokenizer(JavaSparkContext jsc, String stopwordFile)
    {
        stopwords = jsc.textFile(stopwordFile).collect().stream()
                .flatMap(s -> Stream.of(s.split("\\s+")))
                .collect(Collectors.toSet());
    }

    public List<String> getWords(String text)
    {
        final StanfordCoreNLP pipeline = getCoreNLP();

        final Annotation doc = new Annotation(text);
        pipeline.annotate(doc);
        final List<CoreLabel> tokens = doc.get(CoreAnnotations.TokensAnnotation.class);

        return tokens.stream().map(CoreLabel::lemma)
                     .filter(word -> !stopwords.contains(word) && word.length() > minWordLength)
                     .collect(Collectors.toList());
    }

    private StanfordCoreNLP getCoreNLP()
    {
        if (coreNLP == null) {
            final Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma");

            this.coreNLP = new StanfordCoreNLP(props);
        }

        return this.coreNLP;
    }
}
