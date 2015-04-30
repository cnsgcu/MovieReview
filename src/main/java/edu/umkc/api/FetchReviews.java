package edu.umkc.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.umkc.api.models.Review;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FetchReviews {

    public List<Review> getReviews(String message) throws IOException {
        final String reviewList = apiReviews(message);

        List<Review> reviewContainer = new ArrayList<>();

        JsonParser jsonParser = new JsonParser();

        JsonObject rJsonObject = (JsonObject) jsonParser.parse(reviewList);
        JsonArray reviewArray = (JsonArray) rJsonObject.get("reviews");
        for (JsonElement r : reviewArray) {
            final Review review = new Review();

            final JsonObject child = (JsonObject) r;
            final String critic = child.get("critic").getAsString();
            final String quote = child.get("quote").getAsString();
            review.setCritic(critic);
            review.setQuote(quote);

            reviewContainer.add(review);
        }

        return reviewContainer;
    }

    //returns review for selected movie.
    public static String apiReviews(String movieID) {

        String customURL = "http://api.rottentomatoes.com/api/public/v1.0/movies/" +
            movieID +
            "/reviews.json?review_type=top_critic&page_limit=50&page=1&country=us&apikey=zsnu54m8bqfzbct8ju4jz88g";

        StringBuilder output = new StringBuilder();
        String rw = "";
        try {
            URL url = new URL(customURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            rw = br.readLine();
            while (rw != null) {
                output.append(rw);
                rw = br.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return output.toString();
    }

}
