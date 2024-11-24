package handler.job.impl;

import dto.JobDto;
import dto.TaskDto;
import enums.JobType;
import enums.PropagateType;
import handler.job.JobHandler;
import service.ServerUtils;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PropagateHandler implements JobHandler {
    private static final ConcurrentHashMap<String, Integer> MASTER_STATUS_MAP = new ConcurrentHashMap<>();

    public static void registerTask(String jobId, TaskDto task) {
        JobDto jobDto = JobHandler.JOB_MAP.get(jobId);
        if (task == null || jobDto == null) {
            return;
        }
        jobDto.getTaskQueue().add(task);
    }

    @Override
    public void registerJob(JobDto jobDto) {
        String jobId = ServerUtils.formatIdFromSocket(jobDto.getSocket());
        JobHandler.JOB_MAP.put(jobId, jobDto);
        PropagateHandler.MASTER_STATUS_MAP.put(jobId, PropagateType.DEFAULT.getStatus());
        new Thread(() -> processTask(jobId)).start();
        new Thread(() -> listenTask(jobId)).start();
    }

    @Override
    public void listenTask(String jobId) {
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
                    Thread.sleep(Duration.of(taskDto.getFreq(), ChronoUnit.MICROS)); // issue: fullresync comes after empty rdb
                    if (canProcessTask(taskDto)) {
                        if (canWriteTask(taskDto)) {
                            ServerUtils.writeThenFlush(clientSocket, taskDto.getCommandStr(), taskDto.getCommand());
                        }
                        incrementMasterStatus(taskDto);
                    } else {
                        taskQueue.add(taskDto);
                    }
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
                || ((taskDto.getCommandStr() == null || taskDto.getCommandStr().isEmpty())
                    && (taskDto.getCommand() == null || taskDto.getCommand().length == 0))
                || !Objects.equals(taskDto.getJobType(), JobType.PROPAGATE)) {
            return false;
        }
        String jobId = ServerUtils.formatIdFromSocket(taskDto.getSocket());
        if (isMasterCompletePropagate(jobId)) {
            return true;
        }
        return PropagateType.canProcessTask(taskDto.getCommandStr(), PropagateHandler.MASTER_STATUS_MAP.get(jobId));
    }

    private boolean canWriteTask(TaskDto taskDto) {
        if (taskDto == null
                || ((taskDto.getCommandStr() == null || taskDto.getCommandStr().isEmpty())
                    && (taskDto.getCommand() == null || taskDto.getCommand().length == 0))
                || !Objects.equals(taskDto.getJobType(), JobType.PROPAGATE)) {
            return false;
        }
        String jobId = ServerUtils.formatIdFromSocket(taskDto.getSocket());
        if (isMasterCompletePropagate(jobId)) {
            return true;
        }
        return PropagateType.canWriteTask(taskDto.getCommandStr());
    }

    private void incrementMasterStatus(TaskDto taskDto) {
        String jobId = ServerUtils.formatIdFromSocket(taskDto.getSocket());
        if (isMasterCompletePropagate(jobId)) {
            // once complete, not allow to update master status
            return;
        }
        PropagateHandler.MASTER_STATUS_MAP.put(jobId, PropagateHandler.MASTER_STATUS_MAP.get(jobId)+1);
    }

    private boolean isMasterCompletePropagate(String jobId) {
        Integer status = PropagateHandler.MASTER_STATUS_MAP.get(jobId);
        return Objects.equals(status, PropagateType.EMPTY_RDB_TRANSFER.getStatus());
    }
}
