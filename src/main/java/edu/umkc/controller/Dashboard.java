package edu.umkc.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.umkc.api.FetchMovies;
import edu.umkc.api.FetchReviews;
import edu.umkc.api.models.MovieSearchResponse;
import edu.umkc.api.models.Review;
import edu.umkc.service.LDA;
import edu.umkc.service.SentimentAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
public class Dashboard
{
    final static private Logger LOGGER = LoggerFactory.getLogger(Dashboard.class);

    final static Gson gson = new GsonBuilder().create();

    @Autowired
    private SentimentAnalysis sentimentAnalysis;

    @Autowired
    private LDA lda;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String get()
    {
        lda.topicDiscover();

        LOGGER.info("Get Dashboard page");

        return "dashboard";
    }

    @RequestMapping(value = "/movie", method = RequestMethod.GET)
    public @ResponseBody MovieSearchResponse searchMovie(@RequestParam String name)
    {
        try {
            final FetchMovies fetcher = new FetchMovies();
            final MovieSearchResponse res = new MovieSearchResponse();
            res.setQuery(name);
            res.setMovies(fetcher.getMovies(name));

            return res;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @RequestMapping(value = "/review/{movieId}", method = RequestMethod.GET)
    public void searchMovie(@PathVariable String movieId, HttpServletResponse response)
    {
        response.setContentType("text/event-stream");

        try {
            final PrintWriter writer = response.getWriter();
            final FetchReviews reviewFetcher = new FetchReviews();
            final List<Review> reviews = reviewFetcher.getReviews(movieId);

            LOGGER.info("Movie " + movieId + " has " + reviews.size() + " reviews.");

            for (Review review : reviews) {
                writer.write("data: " + gson.toJson(sentimentAnalysis.sentimentAnalyze(review.getQuote())) + "\n\n");
                writer.flush();
            }

            writer.write("event: eos\n");
            writer.write("data:\n\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
