package Client.Protocols;

import Client.ClientProcess.ClientSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class QueryingProtocol extends Protocol{

    public QueryingProtocol(ClientSide clientSide, DataInputStream input, DataOutputStream output){
        super(clientSide, input, output);
    }

    @Override
    public byte[] Wrap(String applicationPayload) {
        int applicationPayloadLength = applicationPayload.length();
        byte[] applicationHeader = new byte[7];
        byte[] payloadSize = ByteBuffer.allocate(4).putInt(applicationPayloadLength).array();

        applicationHeader[0] = clientSide.getCurrentPhase();
        applicationHeader[1] = clientSide.getCurrentMessageType();
        applicationHeader[2] = clientSide.getCurrentRequestType();

        for (int i = 3, j = 0; i < applicationHeader.length; i++, j++) {
            applicationHeader[i] = payloadSize[j];
        }

        byte[] payload = new byte[applicationPayloadLength + applicationHeader.length];
        System.arraycopy(applicationHeader, 0, payload, 0, applicationHeader.length);

        byte[] applicationPayloadByte = applicationPayload.getBytes();
        for (int i = 7, j = 0; i < payload.length; i++, j++) {
            payload[i] = applicationPayloadByte[j];
        }

        return payload;
    }

    @Override
    public String GetByteAndUnwrap() throws IOException {
        // FIRST PART
        byte[] applicationHeader = new byte[7];

        // If an error is occurred while reading the header, an exception is thrown
        int error = input.read(applicationHeader, 0, applicationHeader.length);
        if (error == -1) throw new EOFException();

        byte phase = applicationHeader[0];
        clientSide.setCurrentPhase(phase);

        byte type = applicationHeader[1];
        clientSide.setCurrentMessageType(type);

        byte requestType = applicationHeader[2];
        clientSide.setCurrentRequestType(requestType);

        // Size is transferred to its allocated space in the array
        byte[] size = new byte[4];
        for (int i = 0, j = 3; i < size.length; i++, j++) {
            size[i] = applicationHeader[j];
        }

        // SECOND PART
        int messageLength = ByteBuffer.wrap(size).getInt();
        byte[] message = new byte[messageLength];

        // If an error is occurred while reading the message, an exception is thrown
        error = input.read(message, 0, messageLength);
        if (error == -1) throw new EOFException();

        String messageSTR = new String(message, StandardCharsets.UTF_8);
        clientSide.controlPhaseAndType();

        return messageSTR;
    }
}
