package Server.Protocols;


import Server.ServerProcess.ServerSide;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public abstract class Protocol {

    protected ServerSide serverSide;
    protected DataInputStream input;
    protected DataOutputStream output;

    public Protocol(ServerSide serverSide, DataInputStream input, DataOutputStream output) {
        this.serverSide = serverSide;
        this.input = input;
        this.output = output;
    }



    public final void SendMessage(String message) throws IOException {
        // Gönderilecek mesajı sarılmış halde gönderir
        byte[] payload = Wrap(message);
        output.write(payload, 0, payload.length);
    }

    public abstract byte[] Wrap(String message);
    public abstract String GetByteAndUnwrap() throws IOException;

}
