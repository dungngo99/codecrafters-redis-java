package domain;

public class ZNodeDto {
    private final String member;
    private Double score;

    public ZNodeDto(String member) {
        this(member, null);
    }

    public ZNodeDto(String member, Double score) {
        this.member = member;
        this.score = score;
    }

    public String getMember() {
        return member;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
