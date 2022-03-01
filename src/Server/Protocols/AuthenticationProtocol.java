package Server.Protocols;

import Server.ServerProcess.ServerSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AuthenticationProtocol extends Protocol{

    public AuthenticationProtocol(ServerSide serverSide, DataInputStream input, DataOutputStream output) {
        super(serverSide, input, output);
    }

    public String GetByteAndUnwrap() throws IOException {
        byte[] header = new byte[5];
        int err = input.read(header, 0, header.length);
        if (err == -1) throw new EOFException();
        byte type = header[1];
        byte[] size = new byte[4];
        for (int i = 0, j = 1; i < size.length; i++, j++) {
            size[i] = header[j];
        }
        int messageLength = ByteBuffer.wrap(size).getInt();

        byte[] requestMsg = new byte[messageLength];
        err = input.read(requestMsg, 0, messageLength);
        if (err == -1) throw new EOFException();
        return new String(requestMsg, StandardCharsets.UTF_8);
    }


    public byte[] Wrap(String message) {
        int messageLength = message.length();
        byte[] applicationHeader = new byte[6];
        byte[] size = ByteBuffer.allocate(4).putInt(messageLength).array();
        byte[] msg = message.getBytes();

        applicationHeader[0] = serverSide.getCurrentPhase();
        applicationHeader[1] = serverSide.getCurrentMessageType();

        for (int i = 2, j = 0; i < applicationHeader.length; i++, j++) {
            applicationHeader[i] = size[j];
        }

        byte[] payload = new byte[applicationHeader.length + messageLength];

        System.arraycopy(applicationHeader, 0, payload, 0, applicationHeader.length);
        for (int i = 6, j = 0; i < payload.length; i++, j++) {
            payload[i] = msg[j];
        }

        return payload;
    }
}
