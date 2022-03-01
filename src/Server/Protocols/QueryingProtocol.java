package Server.Protocols;

import Server.ServerProcess.ServerSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class QueryingProtocol extends Protocol{

    public QueryingProtocol(ServerSide serverSide, DataInputStream input, DataOutputStream output){
        super(serverSide, input, output);
    }

    @Override
    public byte[] Wrap(String message) {
        int messageLength = message.length();
        byte[] applicationHeader = new byte[7];
        byte[] size = ByteBuffer.allocate(4).putInt(messageLength).array();

        applicationHeader[0] = serverSide.getCurrentPhase();
        applicationHeader[1] = 0x01;
        applicationHeader[2] = serverSide.getCurrentRequestType();

        for (int i = 3, j = 0; i < applicationHeader.length; i++, j++) {
            applicationHeader[i] = size[j];
        }

        byte[] payload = new byte[applicationHeader.length + messageLength];
        System.arraycopy(applicationHeader, 0, payload, 0, applicationHeader.length);

        byte[] hshValue = message.getBytes();
        for (int i = 7, j = 0; i < payload.length; i++, j++) {
            payload[i] = hshValue[j];
        }

        return payload;
    }

    @Override
    public String GetByteAndUnwrap() throws IOException {
        // FIRST PART
        byte[] applicationHeader = new byte[6];

        // If an error is occurred while reading the header, an exception is thrown
        int error = input.read(applicationHeader, 0, applicationHeader.length);
        if (error == -1) throw new EOFException();

        byte type = applicationHeader[0];
        serverSide.setCurrentMessageType(type);

        byte requestType = applicationHeader[1];
        serverSide.setCurrentRequestType(requestType);

        byte[] messageSize = new byte[4];
        for (int i = 0, j = 2; i < messageSize.length; i++, j++) {
            messageSize[i] = applicationHeader[j];
        }

        // SECOND PART
        int messageLength = ByteBuffer.wrap(messageSize).getInt();
        byte[] message = new byte[messageLength];

        // If an error is occurred while reading the message, an exception is thrown
        error = input.read(message, 0, messageLength);
        if (error == -1) throw new EOFException();

        String messageSTR = new String(message, StandardCharsets.UTF_8);
        serverSide.controlPhaseAndType();

        return messageSTR;
    }
}
