package dto;

import enums.JobType;

import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JobDto {
    public static class Builder {
        private JobType jobType;
        private Socket socket;
        private int freq;
        private ConcurrentLinkedQueue<TaskDto> taskQueue;

        public Builder(JobType jobType) {
            this.jobType = jobType;
        }

        public Builder addSocket(Socket socket) {
            this.socket = socket;
            return this;
        }

        public Builder addFreq(int freq) {
            this.freq = freq;
            return this;
        }

        public Builder addTaskQueue() {
            this.taskQueue = new ConcurrentLinkedQueue<>();
            return this;
        }

        public JobDto build() {
            JobDto jobDto = new JobDto();
            jobDto.setJobType(this.jobType);
            jobDto.setSocket(this.socket);
            jobDto.setFreq(this.freq);
            jobDto.setTaskQueue(this.taskQueue);
            return jobDto;
        }
    }

    private JobType jobType;
    private Socket socket;
    private int freq;
    private ConcurrentLinkedQueue<TaskDto> taskQueue;

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public ConcurrentLinkedQueue<TaskDto> getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(ConcurrentLinkedQueue<TaskDto> taskQueue) {
        this.taskQueue = taskQueue;
    }
}
