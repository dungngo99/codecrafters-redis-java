package dto;

public class Cache {
    private String value;
    private Long px;
    private Long startTime;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getPx() {
        return px;
    }

    public void setPx(Long px) {
        this.px = px;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
}
