package service;

import constants.OutputConstants;
import dto.RESPResultDto;
import dto.TaskDto;
import enums.CommandType;
import enums.JobType;
import enums.PropagateType;
import enums.RESPResultType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class RESPUtils {

    public static String toArray(List<String> list) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.ASTERISK + list.size());
        for (String str: list) {
            joiner.add(OutputConstants.DOLLAR_SIZE + str.length());
            joiner.add(str);
        }
        return joiner.toString();
    }

    public static String toBulkString(String str) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.DOLLAR_SIZE + str.length());
        joiner.add(str);
        return joiner.toString();
    }

    public static String toByteStreamWithCRLF(byte[] bytes) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.DOLLAR_SIZE + bytes.length);
        return joiner.toString();
    }

    public static String toSimpleString(String str) {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.PLUS, OutputConstants.CRLF);
        joiner.add(str);
         return joiner.toString();
    }

    public static String getRESPPing() {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.ASTERISK, OutputConstants.CRLF);
        joiner.add(String.valueOf(OutputConstants.RESP_PING_ARRAY_LENGTH));
        joiner.add(OutputConstants.DOLLAR_SIZE + OutputConstants.PING.length());
        joiner.add(OutputConstants.PING);
        return joiner.toString();
    }

    public static String getRESPEcho(String echo) {
        return toSimpleString(echo);
    }

    public static String getBulkNull() {
        StringJoiner joiner = new StringJoiner(OutputConstants.CRLF, OutputConstants.EMPTY, OutputConstants.CRLF);
        joiner.add(OutputConstants.DOLLAR_SIZE + OutputConstants.NULL_BULK);
        return joiner.toString();
    }

    public static String getRESPOk() {
        return toSimpleString(OutputConstants.OK);
    }

    public static String requestRESPReplConfAck() {
        List<String> list = List.of(CommandType.REPLCONF.name(), CommandType.GETACK.name(), OutputConstants.ASTERISK);
        return toArray(list);
    }

    public static String respondRESPReplConfAckWithDefaultOffset() {
        List<String> list = List.of(CommandType.REPLCONF.name(), CommandType.ACK.name(), String.valueOf(OutputConstants.CLIENT_REPL_OFFSET_DEFAULT));
        return toArray(list);
    }

    public static byte[] fromByteList(List<Byte> list) {
        int l = list.size();
        byte[] array = new byte[l];
        for (int i=0; i<l; i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static byte[] fromStringList(List<String> list) {
        List<Byte> byteList = new ArrayList<>();
        for (String str: list) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            for (byte b: bytes) {
                byteList.add(b);
            }
        }
        byte[] ans = new byte[byteList.size()];
        for (int i=0; i<ans.length; i++) {
            ans[i] = byteList.get(i);
        }
        return ans;
    }

    public static byte[] combine2Bytes(byte[] b1, byte[] b2) {
        byte[] ans = new byte[b1.length + b2.length];
        for (int i=0; i<b1.length; i++) {
            ans[i] = b1[i];
        }
        for (int i=0; i<b2.length; i++) {
            ans[i+b1.length] = b2[i];
        }
        return ans;
    }

    public static boolean isValidRESPResponse(String resp) {
        return Objects.nonNull(resp)
                && !resp.isEmpty()
                && (resp.startsWith(OutputConstants.DOLLAR_SIZE)
                || resp.startsWith(OutputConstants.NULL_BULK)
                || resp.startsWith(OutputConstants.ASTERISK)
                || resp.startsWith(OutputConstants.PLUS));
    }

    public static boolean isValidHandshakeReplicationSimpleString(String resp) {
        return Objects.nonNull(resp) &&
                (resp.startsWith(OutputConstants.OK)
                        || resp.startsWith(OutputConstants.REPLICA_FULL_RESYNC)
                        || resp.startsWith(OutputConstants.PONG)
                        || resp.toLowerCase().contains(OutputConstants.MASTER_TO_REPLICA_ACK));
    }

    public static List<TaskDto> convertToTasks(RESPResultDto result) throws IOException {
        if (!RESPResultType.shouldProcess(result.getType())) {
            return List.of();
        }

        RESPResultType type = result.getType();
        if (Objects.equals(type, RESPResultType.STRING)) {
            return convertToTasksFromSimpleString(result);
        } else if (Objects.equals(type, RESPResultType.LIST)) {
            return convertToTasksFromList(result);
        } else {
            return List.of();
        }
    }

    private static List<TaskDto> convertToTasksFromSimpleString(RESPResultDto resultDto) {
        List<String> list = resultDto.getList();
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        String string = list.getFirst();
        JobType jobType = resultDto.getJobType();
        if (Objects.equals(jobType, JobType.RESP) && !RESPUtils.isValidRESPResponse(string)) {
            return List.of();
        }
        if (Objects.equals(jobType, JobType.HANDSHAKE) && !RESPUtils.isValidHandshakeReplicationSimpleString(string)) {
            return List.of();
        }
        TaskDto taskDto = new TaskDto.Builder()
                .addJobType(jobType)
                .addSocket(resultDto.getSocket())
                .addFreq(OutputConstants.THREAD_SLEEP_100_MICROS)
                .build();
        if (resultDto.isUseBytes()) {
            taskDto.setCommand(string.getBytes(StandardCharsets.UTF_8));
        } else {
            taskDto.setCommandStr(string);
        }
        return new ArrayList<>(List.of(taskDto));
    }

    private static List<TaskDto> convertToTasksFromList(RESPResultDto resultDto) throws IOException {
        List<String> list = resultDto.getList();
        if (list == null || list.isEmpty() || resultDto.getSocket() == null) {
            return List.of();
        }
        if (resultDto.isPipeline()) {
            byte[] bytes = fromStringList(list);
            if (bytes.length == 0) {
                return List.of();
            } else {
                TaskDto taskDto = new TaskDto.Builder()
                        .addJobType(resultDto.getJobType())
                        .addSocket(resultDto.getSocket())
                        .addCommand(bytes)
                        .addFreq(OutputConstants.THREAD_SLEEP_100_MICROS)
                        .build();
                return new ArrayList<>(List.of(taskDto));
            }
        } else {
            List<TaskDto> taskDtoList = new ArrayList<>();
            for (String ans: list) {
                RESPResultDto subResult = new RESPResultDto();
                subResult.setJobType(resultDto.getJobType());
                subResult.setType(RESPResultType.STRING);
                subResult.setList(List.of(ans));
                subResult.setSocket(resultDto.getSocket());
                subResult.setUseBytes(resultDto.isUseBytes());
                taskDtoList.addAll(convertToTasks(subResult));
            }
            return taskDtoList;
        }
    }
}
