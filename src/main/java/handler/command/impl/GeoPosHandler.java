package handler.command.impl;

import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GeoPosHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.GEOPOS.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 2) {
            throw new RuntimeException("invalid param");
        }

        String zSetGeoKey = (String) list.get(0);
        List<String> zSetGeoMembers = new ArrayList<>();
        for (int i=1; i<list.size(); i++) {
            zSetGeoMembers.add((String) list.get(i));
        }

        CommandHandler zScoreCommandHandler = CommandHandler.HANDLER_MAP.get(CommandType.ZSCORE.getAlias());
        if (Objects.isNull(zScoreCommandHandler)) {
            return RESPUtils.toArray(List.of(RESPUtils.getBulkNullArray()));
        }

        List<Object> respList = new ArrayList<>();
        for (String zSetGeoMember: zSetGeoMembers) {
            String resp = zScoreCommandHandler.process(clientSocket, List.of(zSetGeoKey, zSetGeoMember));
            if (resp == null || resp.isEmpty() || resp.equals(RESPUtils.getBulkNullString())) {
                respList.add(null);
            } else {
                respList.add(List.of("0", "0"));
            }
        }

        String r1 = RESPUtils.toBulkStringFromNestedList(respList);
        System.out.println(r1);
        return r1;
    }
}
