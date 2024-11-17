package dto;

import enums.RESPResultType;

import java.util.List;

public class RESPResult {

    private RESPResultType type;
    private List<String> list;

    public RESPResultType getType() {
        return type;
    }

    public void setType(RESPResultType type) {
        this.type = type;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }
}
