package dto;

public class CacheDto {
    private String value;
    private Long expireTime;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    @Override
    public String toString() {
        return "Cache{" +
                "value='" + value + '\'' +
                ", expireTime=" + expireTime +
                '}';
    }
}