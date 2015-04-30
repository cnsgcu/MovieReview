package edu.umkc.service;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.umkc.model.Opinion;
import edu.umkc.model.ReviewSentiment;
import edu.umkc.model.Sentiment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class SentimentAnalysis
{
    @Autowired
    private StanfordCoreNLP coreNLP;

    final public Predicate<Set<CorefMention>> hasTopics = mentions -> !guessTopics(mentions).isEmpty();
    final public Predicate<CorefMention> nonPronominal = mention -> mention.mentionType != MentionType.PRONOMINAL;
    final public Comparator<CorefMention> byMentionSpanLen = (lhs, rhs) -> lhs.mentionSpan.length() - rhs.mentionSpan.length();

    public ReviewSentiment sentimentAnalyze(String message)
    {
        final Annotation doc = coreNLP.process(message);

        final List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
        final Map<Integer, CorefChain> graph = doc.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        final Function<CorefMention, Opinion> toOpinion = mention -> new Opinion(sentences.get(mention.sentNum - 1).toString(), RNNCoreAnnotations.getPredictedClass(sentences.get(mention.sentNum - 1).get(SentimentCoreAnnotations.AnnotatedTree.class)));

        final List<Sentiment> sentiments = getMentions(graph).stream()
            .filter(hasTopics).map(mentions -> {
                final Sentiment sentiment = new Sentiment();
                sentiment.setTopic(guessTopics(mentions).get(0).mentionSpan);
                sentiment.setOpinions(mentions.stream().map(toOpinion).collect(Collectors.toSet()));

                return sentiment;
            }).collect(Collectors.toList());

        final ReviewSentiment reviewSentiment = new ReviewSentiment();
        reviewSentiment.setReview(message);
        reviewSentiment.setSentiments(sentiments);

        return reviewSentiment;
    }

    private List<Set<CorefChain.CorefMention>> getMentions(Map<Integer, CorefChain> graph)
    {
        return graph.entrySet().stream()
            .filter(node -> !node.getValue().getMentionMap().isEmpty())
            .map(node -> node.getValue().getMentionMap().values().stream()
                             .flatMap(Set::stream)
                             .collect(Collectors.toSet())
            ).collect(Collectors.toList());
    }

    private List<CorefChain.CorefMention> guessTopics(Set<CorefChain.CorefMention> mentions)
    {
        return mentions.stream()
                       .filter(nonPronominal)
                       .sorted(byMentionSpanLen)
                       .collect(Collectors.toList());
    }
}
