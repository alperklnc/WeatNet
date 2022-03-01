package Server.DataTransform;

import Server.ServerProcess.Server;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class DataServer extends ServerSocket{
    private Socket clientDataSocket;
    private ObjectOutputStream dataOut;

    public DataServer(int port) throws IOException {
        super(port);
        clientDataSocket=accept();
        System.out.println(clientDataSocket.getInetAddress());
        dataOut=new ObjectOutputStream(clientDataSocket.getOutputStream());
    }

    public void closeClient() throws IOException {
        clientDataSocket.close();
    }

    public void sendData(String obj) throws IOException {
        setSoTimeout(10000);
        dataOut.writeObject(obj);
        dataOut.flush();
    }

    public void sendImage(BufferedImage obj) throws IOException {
        setSoTimeout(10000);
        //put image to file for demonstration purposes
        File outputfile = new File("src/Server/imageOutput.png");
        ImageIO.write(obj, "png", outputfile);
        //send image to client
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(obj, "png", bos);
        bos.flush();
        byte [] data = bos.toByteArray();
        dataOut.write(data);
        dataOut.flush();
    }

}
