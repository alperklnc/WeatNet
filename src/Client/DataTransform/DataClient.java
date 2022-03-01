package Client.DataTransform;



import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class DataClient extends Socket{
    private ObjectInputStream dataIn;
    public int hashcodeImage;
    public DataClient(String lcl,int port) throws IOException {
        super(lcl,port);
        dataIn=new ObjectInputStream(getInputStream());
    }

    public Object getData() throws IOException, ClassNotFoundException {
        Object obj = dataIn.readObject();
        return obj;
    }
    public Object getImage() throws IOException, ClassNotFoundException {
        byte[] data = dataIn.readAllBytes();
        createHashCodeImage(data);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        BufferedImage obj = ImageIO.read(bis);
        return obj;
    }

    private void createHashCodeImage(byte[] data) throws IOException {
        int sum=0;
        for (int i=0;i<data.length;i++){
            sum+=data[i];
        }
        hashcodeImage=sum;
    }
}
