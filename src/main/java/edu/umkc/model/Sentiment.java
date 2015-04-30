package edu.umkc.model;

import java.util.Set;

public class Sentiment
{
    private String topic;
    private Set<Opinion> opinions;

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public Set<Opinion> getOpinions()
    {
        return opinions;
    }

    public void setOpinions(Set<Opinion> opinions)
    {
        this.opinions = opinions;
    }
}
