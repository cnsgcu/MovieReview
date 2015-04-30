package edu.umkc.api.models;

import java.util.List;

public class MovieSearchResponse
{
    private String query;
    private List<Movie> movies;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }
}
