package handler.command.impl;

import domain.GeoDto;
import enums.CommandType;
import handler.command.CommandHandler;
import service.RESPUtils;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GeoAddHandler implements CommandHandler {
    @Override
    public void register() {
        CommandHandler.HANDLER_MAP.put(CommandType.GEOADD.getAlias(), this);
    }

    @Override
    public String process(Socket clientSocket, List list) {
        if (list == null || list.size() < 4 || (list.size()-1) % 3 != 0) {
            throw new RuntimeException("invalid param");
        }

        String geoKey = (String) list.get(0);
        List<GeoDto> geoDtoList = new ArrayList<>();
        for (int i=1; i<list.size(); i+=3) {
            GeoDto geoDto = new GeoDto();
            geoDto.setLongitude(Double.parseDouble((String) list.get(i)));
            geoDto.setLatitude(Double.parseDouble((String) list.get(i+1)));
            geoDto.setMember((String) list.get(i+2));
            geoDtoList.add(geoDto);
        }

        return RESPUtils.toSimpleInt(geoDtoList.size());
    }
}
