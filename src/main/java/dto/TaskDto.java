package dto;

import enums.JobType;

import java.net.Socket;
import java.util.Arrays;

public class TaskDto {
    public static class Builder {
        private JobType jobType;
        private Socket socket;
        private String commandStr;
        private byte[] command;
        private int freq;

        public Builder addJobType(JobType jobType) {
            this.jobType = jobType;
            return this;
        }

        public Builder addSocket(Socket socket) {
            this.socket = socket;
            return this;
        }

        public Builder addCommandStr(String commandStr) {
            this.commandStr = commandStr;
            return this;
        }

        public Builder addCommand(byte[] command) {
            this.command = command;
            return this;
        }

        public Builder addFreq(int freq) {
            this.freq = freq;
            return this;
        }

        public TaskDto build() {
            TaskDto taskDto = new TaskDto();
            taskDto.setJobType(this.jobType);
            taskDto.setSocket(this.socket);
            taskDto.setCommandStr(this.commandStr);
            taskDto.setCommand(this.command);
            taskDto.setFreq(this.freq);
            return taskDto;
        }
    }

    private JobType jobType;
    private Socket socket;
    private String commandStr;
    private byte[] command;
    private int freq;

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

    public String getCommandStr() {
        return commandStr;
    }

    public void setCommandStr(String commandStr) {
        this.commandStr = commandStr;
    }

    public byte[] getCommand() {
        return command;
    }

    public void setCommand(byte[] command) {
        this.command = command;
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    @Override
    public String toString() {
        return "TaskDto{" +
                "jobType=" + jobType +
                ", socket=" + socket +
                ", commandStr='" + commandStr + '\'' +
                ", command=" + Arrays.toString(command) +
                '}';
    }
}
