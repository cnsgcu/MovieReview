package edu.umkc.service;

import edu.stanford.nlp.util.ArrayUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.clustering.LDA;
import org.apache.spark.mllib.clustering.LDAModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Component
public class SparkLDA
{
    final static private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SparkLDA.class);

    public static void main(String... args)
    {
        final SparkLDA lda = new SparkLDA();

        lda.guess("The Lion King, complete with jaunty songs by Elton John and Tim Rice, is undeniably and fully worthy of its glorious Disney heritage. It is a gorgeous triumph -- one lion in which the studio can take justified pride.");
    }

    private SimpleTokenizer tokenizer;

    public void guess(String msg)
    {
        getTopicModel(Arrays.asList(msg));
    }

    private SimpleTokenizer getTokenizer(JavaSparkContext jsc)
    {
        if (tokenizer == null) {
            final String stopWords = Thread.currentThread().getContextClassLoader().getResource("lda/stopwords.txt").getPath();

            LOGGER.info("Stop words from file " + stopWords);

            this.tokenizer = new SimpleTokenizer(jsc, stopWords);
        }

        return this.tokenizer;
    }

    private void getTopicModel(List<String> doc)
    {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);

        LOGGER.info("Spark LDA topic discovery.");

        final SparkConf conf = new SparkConf().setMaster("local[4]").setAppName("SparkLDA");
        final JavaSparkContext jsc = new JavaSparkContext(conf);

        final SimpleTokenizer tokenizer = getTokenizer(jsc);
        final JavaRDD<String> plotsRDD = jsc.parallelize(doc);

        final JavaRDD<Tuple2<Long, List<String>>> tokenized = plotsRDD.zipWithIndex().map(t -> new Tuple2<>(t._2(), tokenizer.getWords(t._1())));
        tokenized.cache();

        final JavaPairRDD<String, Long> wordCounts = tokenized.flatMapToPair(
            t -> t._2().stream().collect(groupingBy(identity(), counting()))
                       .entrySet().stream()
                       .map(e -> new Tuple2<>(e.getKey(), e.getValue()))
                       .collect(Collectors.toList())
        ).reduceByKey(Long::sum);
        wordCounts.cache();

        final Iterator<Integer> idxGen = IntStream.iterate(0, i -> i + 1).iterator();
        final Map<String, Integer> vocab = wordCounts.collect().stream()
                                                     .sorted((t1, t2) -> -Long.compare(t1._2(), t2._2()))
                                                     .collect(Collectors.toMap(Tuple2::_1, t -> idxGen.next()));

        final JavaPairRDD<Long, Vector> corpus = tokenized.mapToPair(t -> {
            final Map<Integer, Long> wc = t._2().stream().filter(vocab::containsKey).collect(groupingBy(vocab::get, counting()));
            final Vector sb = Vectors.sparse(
                vocab.size(),
                ArrayUtils.asPrimitiveIntArray(wc.keySet()),
                ArrayUtils.asPrimitiveDoubleArray(wc.values().stream().map(Double::new).collect(toList()))
            );

            return new Tuple2<>(t._1(), sb);
        });

        final List<String> vocabArray = IntStream.range(0, vocab.size()).mapToObj(i -> "").collect(Collectors.toList());
        vocab.entrySet().forEach(e -> vocabArray.set(e.getValue(), e.getKey()));

        final LDA lda = new LDA().setK(5)
                                 .setMaxIterations(100)
                                 .setDocConcentration(-1)
                                 .setTopicConcentration(-1);

        final LocalDateTime start = LocalDateTime.now();
        final LDAModel model = lda.run(corpus);
        LOGGER.info("Execution time " + Duration.between(start, LocalDateTime.now()));

        Tuple2<int[], double[]>[] topicIndices = model.describeTopics(5);

        for (int i = 0; i < topicIndices.length; i++) {
            LOGGER.info("============ Topic " + (i+1) + " =============");
            for (Integer j : topicIndices[i]._1()) {
                LOGGER.info(vocabArray.get(j));
            }
        }

        jsc.stop();
    }
}