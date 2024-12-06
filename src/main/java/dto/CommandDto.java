package dto;

import java.util.List;

public class CommandDto {
    private List list;

    public CommandDto(List list) {
        this.list = list;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }
}
