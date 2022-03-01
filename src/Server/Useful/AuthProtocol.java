package Server.Useful;

public interface AuthProtocol {

    byte AUTHENTICATION_PHASE=0x00;

    byte AUTH_REQUEST=0x00;
    byte AUTH_CHALLENGE_RESPONSE=0x01;
    byte AUTH_FAIL=0x02;
    byte AUTH_SUCCESS=0x03;

}
