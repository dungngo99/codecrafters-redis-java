package service;

import constants.OutputConstants;
import dto.StreamDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StreamUtils {

    public static String formatEventId(Long[] eventIdArr) {
        return eventIdArr[0] + OutputConstants.DASH_DELIMITER + eventIdArr[1];
    }

    public static Long[] parseEventId(String eventId) {
        String[] arr = eventId.split(OutputConstants.DASH_DELIMITER);
        return new Long[]{Long.parseLong(arr[0]), Long.parseLong(arr[1])};
    }

    public static int getIndexFromStream(List<StreamDto.EntryDto> streamList, Long[] key, boolean isUpperBound) {
        Comparator<Long[]> comparator = (v1, v2) -> v1[0].compareTo(v2[0]) != 0 ? v1[0].compareTo(v2[0]) : v1[1].compareTo(v2[1]);
        return getIndexFromStream0(streamList, 0, streamList.size()-1, key, comparator, isUpperBound);
    }

    private static int getIndexFromStream0(List<StreamDto.EntryDto> streamList,
                                           int start,
                                           int end,
                                           Long[] key,
                                           Comparator<Long[]> comparator,
                                           boolean isUpperBound) {
        if (end < start) {
            return start;
        }
        int mid = start + (end-start)/2;
        StreamDto.EntryDto midEntryDto = streamList.get(mid);
        Long[] midKey = parseEventId(midEntryDto.getId());
        if (comparator.compare(key, midKey) == 0) {
            return isUpperBound ? mid+1 : mid;
        } else if (comparator.compare(key, midKey) > 0) {
            return getIndexFromStream0(streamList, mid+1, end, key, comparator, isUpperBound);
        } else {
            return getIndexFromStream0(streamList, start, mid-1, key, comparator, isUpperBound);
        }
    }

    public static List<Object> fromStreamEntryDto(List<StreamDto.EntryDto> streamList) {
        List<Object> var1 = new ArrayList<>();
        streamList.forEach(entryDto -> {
            List<Object> var2 = new ArrayList<>();
            var2.add(entryDto.getId());
            List<String> var3 = new ArrayList<>();
            entryDto.getKvPair().forEach((k,v) -> {
                var3.add(k);
                var3.add(v);
            });
            var2.add(var3);
            var1.add(var2);
        });
        return var1;
    }
}
