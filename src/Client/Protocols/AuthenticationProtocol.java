package Client.Protocols;

import Client.ClientProcess.ClientSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AuthenticationProtocol extends Protocol {

    public AuthenticationProtocol(ClientSide clientSide, DataInputStream input, DataOutputStream output) {
        super(clientSide, input, output);
    }

    @Override
    public byte[] Wrap(String message) {
        int messageLength = message.length();
        byte[] applicationHeader = new byte[6];
        byte[] size = ByteBuffer.allocate(4).putInt(messageLength).array();

        applicationHeader[0] = clientSide.getCurrentPhase();
        applicationHeader[1] = 0x00;

        for (int i = 2, j = 0; i < applicationHeader.length; i++, j++) {
            applicationHeader[i] = size[j];
        }

        byte[] payload = new byte[applicationHeader.length + messageLength];
        System.arraycopy(applicationHeader, 0, payload, 0, applicationHeader.length);

        byte[] msg = message.getBytes();
        for (int i = 6, j = 0; i < payload.length; i++, j++) {
            payload[i] = msg[j];
        }

        return payload;
    }

    @Override
    public String GetByteAndUnwrap() throws IOException {
        // FIRST PART
        byte[] applicationHeader = new byte[6];

        // If an error is occurred while reading the message, an exception is thrown
        int error = input.read(applicationHeader, 0, applicationHeader.length);
        if (error == -1) throw new EOFException();

        byte phase = applicationHeader[0];
        clientSide.setCurrentPhase(phase);

        byte messageType = applicationHeader[1];
        clientSide.setCurrentMessageType(messageType);

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
        clientSide.controlPhaseAndType();

        return messageSTR;
    }
}
