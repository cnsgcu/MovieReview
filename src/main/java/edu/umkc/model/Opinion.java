package edu.umkc.model;

public class Opinion
{
    private Integer score;
    private String sentence;

    public Opinion(String sentence, Integer score)
    {
        this.score = score;
        this.sentence = sentence;
    }

    public Integer getScore()
    {
        return score;
    }

    public void setScore(Integer score)
    {
        this.score = score;
    }

    public String getSentence()
    {
        return sentence;
    }

    public void setSentence(String sentence)
    {
        this.sentence = sentence;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Opinion)) return false;

        final Opinion that = (Opinion) obj;

        return this.getScore().equals(that.getScore())
               && this.getSentence().equals(that.getSentence());
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 31 * hash + this.getScore().hashCode();
        hash = 31 * hash + this.getSentence().hashCode();

        return hash;
    }
}
