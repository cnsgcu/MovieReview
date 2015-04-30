package edu.umkc.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Component
public class LDA
{
    final static private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LDA.class);

    @Autowired
    private ServletContext context;

    public void topicDiscover()
    {
        final String trainData = Thread.currentThread().getContextClassLoader().getResource("/lda/news.txt").getPath();
        final String stopWords = Thread.currentThread().getContextClassLoader().getResource("/lda/stopwords.txt").getPath();

        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);

        final SparkConf conf = new SparkConf().setMaster("local[4]").setAppName("LDA");
        final JavaSparkContext jsc = new JavaSparkContext(conf);

        final SimpleTokenizer tokenizer = new SimpleTokenizer(jsc, stopWords);
        final JavaRDD<String> plotsRDD = jsc.textFile(trainData);

        final JavaRDD<Tuple2<Long, List<String>>> tokenized = plotsRDD.zipWithIndex()
                .map(t -> new Tuple2<>(t._2(), tokenizer.getWords(t._1())));
        tokenized.cache();

        JavaRDD<Tuple2<String, Long>> wordCounts = tokenized.flatMap(
            t -> t._2().stream()
                 .collect(groupingBy(identity(), counting()))
                 .entrySet().stream()
                 .map(e -> new Tuple2<>(e.getKey(), e.getValue()))
                 .collect(Collectors.toList())
        );
        wordCounts.cache();

        wordCounts.collect().sort((t1, t2) -> Long.compare(t1._2(), t2._2()));
    }
}