package Client.Protocols;

import Client.ClientProcess.ClientSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Protocol {

    protected ClientSide clientSide;
    protected DataInputStream input;
    protected DataOutputStream output;

    public Protocol(ClientSide clientSide, DataInputStream input, DataOutputStream output) {
        this.clientSide = clientSide;
        this.input = input;
        this.output = output;
    }

    /**
     * Wraps the message according to specified protocol and sends to the Server.
     * @param message that will be wrapped and sent.
     */
    public final void SendMessage(String message) throws IOException {
        byte[] payload = Wrap(message);
        output.write(payload, 0, payload.length);
    }

    /**
     * Wraps the message according to specified protocol and returns it as a byte array
     * @param message that will be wrapped
     * @return message as a byte array
     */
    public abstract byte[] Wrap(String message);

    /**
     * Gets the wrapped message from the Server and unwraps it according to specified protocol.
     * @return unwrapped message as a string
     */
    public abstract String GetByteAndUnwrap() throws IOException;

}