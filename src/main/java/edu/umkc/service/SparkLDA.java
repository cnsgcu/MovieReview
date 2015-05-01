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

    private LDAModel model;

    public void guess(String msg)
    {
        getTopicModel();
    }

    private LDAModel getTopicModel()
    {
        if (model == null) {
            LOGGER.info("Spark LDA topic discovery.");

            final String trainData = Thread.currentThread().getContextClassLoader().getResource("/lda/news.txt").getPath();
            final String stopWords = Thread.currentThread().getContextClassLoader().getResource("/lda/stopwords.txt").getPath();

            Logger.getLogger("org").setLevel(Level.OFF);
            Logger.getLogger("akka").setLevel(Level.OFF);

            final SparkConf conf = new SparkConf().setMaster("local[4]").setAppName("SparkLDA");
            final JavaSparkContext jsc = new JavaSparkContext(conf);

            final SimpleTokenizer tokenizer = new SimpleTokenizer(jsc, stopWords);
            final JavaRDD<String> plotsRDD = jsc.textFile(trainData);

            final JavaRDD<Tuple2<Long, List<String>>> tokenized = plotsRDD.zipWithIndex().map(t -> new Tuple2<>(t._2(), tokenizer.getWords(t._1())));
            tokenized.cache();

            final JavaRDD<Tuple2<String, Long>> wordCounts = tokenized.flatMap(
                    t -> t._2().stream()
                            .collect(groupingBy(identity(), counting()))
                            .entrySet().stream()
                            .map(e -> new Tuple2<>(e.getKey(), e.getValue()))
                            .collect(Collectors.toList())
            );
            wordCounts.cache();

            final Iterator<Integer> ints = IntStream.iterate(0, i -> i + 1).iterator();
            final Map<String, Integer> vocab = wordCounts.collect().stream()
                    .sorted((t1, t2) -> Long.compare(t1._2(), t2._2()))
                    .collect(Collectors.toMap(Tuple2::_1, t -> ints.next()));

            final JavaPairRDD<Long, Vector> corpus = tokenized.mapToPair(t -> {
                final Map<Integer, Long> wc = t._2().stream().filter(vocab::containsKey).collect(groupingBy(vocab::get, counting()));
                final Vector sb = Vectors.sparse(
                        vocab.size(),
                        ArrayUtils.asPrimitiveIntArray(wc.keySet()),
                        ArrayUtils.asPrimitiveDoubleArray(wc.values().stream().map(Double::new).collect(toList()))
                );

                return new Tuple2<>(t._1(), sb);
            });

            final LDA lda = new LDA().setK(10)
                    .setMaxIterations(100)
                    .setDocConcentration(-1)
                    .setTopicConcentration(-1);

            final LDAModel model = lda.run(corpus);

//            Tuple2<int[], double[]>[] topicIndices = model.describeTopics(5);

            jsc.stop();
            this.model = model;
        }

        return this.model;
    }
}