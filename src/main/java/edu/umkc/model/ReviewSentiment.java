package edu.umkc.model;

import java.util.List;

public class ReviewSentiment
{
    private String review;
    private List<Sentiment> sentiments;

    public String getReview()
    {
        return review;
    }

    public void setReview(String review)
    {
        this.review = review;
    }

    public List<Sentiment> getSentiments()
    {
        return sentiments;
    }

    public void setSentiments(List<Sentiment> sentiments)
    {
        this.sentiments = sentiments;
    }
}
