package dto;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StreamDto {
    public static class EntryDto {
        private String id;
        private Map<String, String> kvPair;

        public EntryDto(String id) {
            this.id = id;
            this.kvPair = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, String> getKvPair() {
            return kvPair;
        }

        public void setKvPair(Map<String, String> kvPair) {
            this.kvPair = kvPair;
        }
    }

    /**
     * temp solution: map<key=unique event ID, value=key-value mapping>
     * correct solution: radix trie
     */
    private LinkedHashMap<String, EntryDto> streamMap;

    public StreamDto() {
        this.streamMap = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, EntryDto> getStreamMap() {
        return streamMap;
    }

    public void setStreamMap(LinkedHashMap<String, EntryDto> streamMap) {
        this.streamMap = streamMap;
    }
}
