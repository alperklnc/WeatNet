package Server.Protocols;

import Server.ServerProcess.ServerSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class ProtocolFactory{
    public static Protocol getProtocol(byte protocolType, ServerSide serverSide, DataInputStream input, DataOutputStream output){
        if(protocolType==0x00){
            return new AuthenticationProtocol(serverSide, input, output);

        } else{
            return new QueryingProtocol(serverSide, input, output);
        }
    }
}