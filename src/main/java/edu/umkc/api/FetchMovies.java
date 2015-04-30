package edu.umkc.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.umkc.api.models.Movie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class FetchMovies {
    public List<Movie> getMovies(String message) throws IOException {
        final String movieList = apiMovies(message);

        final JsonParser jsonParser = new JsonParser();

        //fetch the movie id, name and release year for all the movies listed for the search
        final JsonObject mJsonObject = (JsonObject) jsonParser.parse(movieList);
        final JsonArray movieArray = (JsonArray) mJsonObject.get("movies");

        final List<Movie> movieContainer = new ArrayList<>(movieArray.size());
        for (JsonElement jel : movieArray) {
            final JsonObject child = (JsonObject) jel;
            final int mYear = child.get("year").getAsInt();
            final String mId = child.get("id").getAsString();
            final String mTitle = child.get("title").getAsString();
            final String synopsis = child.get("synopsis").getAsString();

            final Movie movie = new Movie();
            movie.setmId(mId);
            movie.setmYear(mYear);
            movie.setmTitle(mTitle);
            movie.setmSynopsis(synopsis);

            movieContainer.add(movie);
        }

        return movieContainer;
    }

    public static String apiMovies(String message) {
        try {
            final String URL = "http://api.rottentomatoes.com/api/public/v1.0/movies.json?q=" +
                    URLEncoder.encode(message, "UTF-8") + "&page_limit=50&page=1&apikey=zsnu54m8bqfzbct8ju4jz88g";

            final StringBuilder output = new StringBuilder();
            try {
                final URL url = new URL(URL);
                final BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

                for (String rw = br.readLine(); rw != null; rw = br.readLine()) {
                    output.append(rw);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return output.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "[]";
    }
}
