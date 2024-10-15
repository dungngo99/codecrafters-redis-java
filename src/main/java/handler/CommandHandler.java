package handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CommandHandler {
    Map<String, CommandHandler> HANDLER_MAP = new HashMap<>();

    void register();
    String process(List list);
}
