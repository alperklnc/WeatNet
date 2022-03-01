package Server.Useful;

public interface QueryProtocol {

    byte QUERYING_PHASE=0x01;

    byte CURRENT_WEATHER=0x00;
    byte DAILY_FORECAST=0x01;
    byte BASIC_WEATHER_MAP=0x02;
    byte MINUTE_FORECAST=0x03;
    byte HISTORICAL_WEATHER=0x04;
    byte HISTORICAL_WEATHER_WRONG=0x05;

}
