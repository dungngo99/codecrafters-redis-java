package handler.job.impl;

import constants.OutputConstants;
import constants.ParserConstants;
import dto.JobDto;
import dto.RESPResultDto;
import dto.TaskDto;
import enums.JobType;
import enums.ReplHandshakeType;
import handler.job.JobHandler;
import service.RESPParser;
import service.RESPUtils;
import service.ServerUtils;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HandshakeHandler implements JobHandler {
    public static final ConcurrentHashMap<String, Integer> REPLICA_STATUS_MAP = new ConcurrentHashMap<>();

    @Override
    public void registerJob(JobDto jobDto) {
        String jobId = ServerUtils.formatIdFromSocket(jobDto.getSocket());
        JobHandler.JOB_MAP.put(jobId, jobDto);
        HandshakeHandler.REPLICA_STATUS_MAP.put(jobId, ReplHandshakeType.DEFAULT.getStatus());
        new Thread(() -> listenTask(jobId)).start();
        new Thread(() -> processTask(jobId)).start();
    }

    @Override
    public void listenTask(String jobId) {
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        Socket handshakeSocket = jobDto.getSocket();

        try {
            while (!handshakeSocket.isClosed()) {
                RESPResultDto result = new RESPParser.Builder()
                        .addClientSocket(handshakeSocket)
                        .addBufferSize(ParserConstants.RESP_PARSER_BUFFER_SIZE)
                        .build()
                        .process();
                result.setPipeline(Boolean.FALSE);
                result.setUseBytes(Boolean.FALSE);
                result.setJobType(JobType.HANDSHAKE);
                List<TaskDto> taskDtoList = RESPUtils.convertToTasks(result);
                if (!taskDtoList.isEmpty()) {
                    jobDto.getTaskQueue().addAll(taskDtoList);
                }
                Thread.sleep(Duration.of(jobDto.getFreq(), ChronoUnit.MICROS));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            JobHandler.JOB_MAP.remove(jobId);
            try {
                if (!handshakeSocket.isClosed()) {
                    handshakeSocket.close();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public static void registerTask(String jobId, String task) {
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        if (task == null || task.isEmpty()) {
            return;
        }
        TaskDto taskDto = new TaskDto.Builder()
                .addJobType(JobType.HANDSHAKE)
                .addCommandStr(task)
                .addSocket(jobDto.getSocket())
                .addFreq(OutputConstants.THREAD_SLEEP_100_MICROS)
                .build();
        jobDto.getTaskQueue().add(taskDto);
    }

    public static void incrementMasterStatus(String jobId) {
        HandshakeHandler.REPLICA_STATUS_MAP.put(jobId, HandshakeHandler.REPLICA_STATUS_MAP.get(jobId)+1);
    }

    @Override
    public void processTask(String jobId) {
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        Socket clientSocket = jobDto.getSocket();

        try {
            while (!clientSocket.isClosed()) {
                ConcurrentLinkedQueue<TaskDto> taskQueue = jobDto.getTaskQueue();
                while(!taskQueue.isEmpty()) {
                    TaskDto taskDto = taskQueue.poll();
                    if (canProcessTask(taskDto)) {
                        HandshakeHandler.incrementMasterStatus(jobId);
                        if (canWriteTask(taskDto)) {
                            ServerUtils.writeThenFlushString(taskDto.getSocket(), taskDto.getCommandStr());
                        }
                    } else {
                        taskQueue.add(taskDto);
                    }
                    Thread.sleep(Duration.of(taskDto.getFreq(), ChronoUnit.MICROS));
                }
                Thread.sleep(Duration.of(jobDto.getFreq(), ChronoUnit.MICROS));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            JobHandler.JOB_MAP.remove(jobId);
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    private boolean canProcessTask(TaskDto taskDto) {
        if (taskDto == null
                || taskDto.getSocket() == null
                || taskDto.getCommandStr() == null
                || taskDto.getCommandStr().isEmpty()
                || !Objects.equals(taskDto.getJobType(), JobType.HANDSHAKE)) {
            return false;
        }
        String jobId = ServerUtils.formatIdFromSocket(taskDto.getSocket());
        return ReplHandshakeType.canProcessTask(taskDto.getCommandStr(), HandshakeHandler.REPLICA_STATUS_MAP.get(jobId));
    }


    private boolean canWriteTask(TaskDto taskDto) {
        if (taskDto == null
                || taskDto.getCommandStr() == null
                || taskDto.getCommandStr().isEmpty()
                || !Objects.equals(taskDto.getJobType(), JobType.HANDSHAKE)) {
            return false;
        }
        return ReplHandshakeType.canWriteTask(taskDto.getCommandStr());
    }
}
