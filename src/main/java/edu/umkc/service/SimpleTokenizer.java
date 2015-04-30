package edu.umkc.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleTokenizer implements Serializable
{
    private int minWordLength = 1;

    private Set<String> stopwords;

    public SimpleTokenizer(JavaSparkContext sc, String filePath)
    {

    }

    public List<String> getWords(String text)
    {
        final Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");

        final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Annotation doc = new Annotation(text);
        pipeline.annotate(doc);
        List<CoreLabel> tokens = doc.get(CoreAnnotations.TokensAnnotation.class);

        return tokens.stream().map(CoreLabel::lemma)
                     .filter(word -> !stopwords.contains(word) && word.length() > minWordLength)
                     .collect(Collectors.toList());
    }
}
