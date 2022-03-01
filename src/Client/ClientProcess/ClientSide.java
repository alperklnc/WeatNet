package Client.ClientProcess;

import Client.DataTransform.DataClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import Client.Protocols.AuthenticationProtocol;
import Client.Protocols.QueryingProtocol;
import Client.Useful.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ClientSide implements AuthProtocol, QueryProtocol {
    private final Socket clientSocket;

    protected DataInputStream input;
    protected DataOutputStream output;

    private Scanner scanner;

    // Initial Phase is Authentication
    private Phase programPhase = Phase.Authentication;

    private MessageTypes messageType = MessageTypes.Auth_Challenge;
    private RequestTypes requestType = RequestTypes.Current_Weather;

    private byte currentPhase;
    private byte currentMessageType;
    private byte currentRequestType;

    private boolean NotClosed=true;

    private int ObjectHashCode;

    private AuthenticationProtocol authenticationProtocol;
    private QueryingProtocol queryingProtocol;

    private String token;

    public ClientSide(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            System.out.println("Client is connected to the Server");
            input = new DataInputStream(clientSocket.getInputStream());
            output = new DataOutputStream(clientSocket.getOutputStream());

            scanner = new Scanner(System.in);

            authenticationProtocol = new AuthenticationProtocol(this, input, output);
            queryingProtocol = new QueryingProtocol(this, input, output);

            System.out.println("Response from server: " + authenticationProtocol.GetByteAndUnwrap());

            while (NotClosed) {

                String message = scanner.nextLine();
                SendMessage(message);
                String response = authenticationProtocol.GetByteAndUnwrap();

                if (programPhase.equals(Phase.Authentication)) {
                    AuthenticationPhase(response);
                    if (messageType.equals(MessageTypes.Auth_Fail))
                        break;
                }
                if ((programPhase.equals(Phase.Querying))) {
                    setCurrentMessageType(AUTH_CHALLENGE);
                    QueryingPhase();
                }
            }
        } catch(SocketException e){
            System.err.println("Connection is timeout");
        } catch (IOException | ClassNotFoundException e ) {
            e.printStackTrace();
        }
    }

    private void AuthenticationPhase(String response) throws IOException {
        while (messageType.equals(MessageTypes.Auth_Challenge)) {
            System.out.println("Response from server: " + response);
            String message = scanner.nextLine();
            SendMessage(message);
            response = authenticationProtocol.GetByteAndUnwrap();
        }
        if (messageType.equals(MessageTypes.Auth_Success)) {
            System.out.println("Authentication Phase is passed Successfully");
            SaveToken(response);
            currentPhase = QUERYING_PHASE;
        } else {
            System.out.println(response);
        }
        controlPhaseAndType();
    }

    private void SaveToken(String response) {
        token = response;
        System.out.println("Token is taken from the Server and saved.");
    }

    private void QueryingPhase() throws IOException, ClassNotFoundException {
        while (currentMessageType == AUTH_CHALLENGE) {
            System.out.println("\nWeather Condition Types\n" +
                    "1 - Current Weather Forecast\n" +
                    "2 - Daily Forecast for 7 Days\n" +
                    "3 - Basic Weather Maps\n" +
                    "4 - Minute Forecast for 1 Hour\n" +
                    "5 - Historical Weather for 5 Days\n"+
                    "6 - Historical Weather for 5 Days with wrong hashValue");
            System.out.println("Please enter your selection according to index " +
                    "number and enter lon-lat values and id number\n" +
                    "ex: 2 29.08333 40.166672 750268\n"+
                    "ex: 1 28.949659 41.01384 745044\n"+
                    "ex: 3 29.08333 40.166672 750268\n"+
                    "ex: 5 32.833328 39.916672 323784\n"+
                    "ex: 6 27.092291 38.462189 311044");

            String message = scanner.nextLine();

            if (!SendMessage(message))
                continue;

            String hashValue = queryingProtocol.GetByteAndUnwrap();
            if (hashValue.equals("Fail")){
                System.out.println(hashValue);
                closeEverything();
                return;
            }
            try {
                System.out.println("\nHashCode of Object over Command socket: " + hashValue);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Object obj = null;
            DataClient dataClient = new DataClient("localhost", 5000);

            if (currentRequestType == BASIC_WEATHER_MAP) {
                obj = dataClient.getImage();
                ObjectHashCode=dataClient.hashcodeImage;
                System.out.println("HashCode of Object over Data socket for Image: " + ObjectHashCode);
            } else {
                obj = dataClient.getData();
                ObjectHashCode=obj.hashCode();
                System.out.println("HashCode of Object over Data socket for Json: " + ObjectHashCode);
            }

            CheckHashValues(hashValue, obj);
        }
        controlPhaseAndType();
    }

    private void CheckHashValues(String hashValue, Object object) throws IOException {
        int hashValueCommandSocket = Integer.parseInt(hashValue);

        if (hashValueCommandSocket == ObjectHashCode) {
            setCurrentMessageType(AUTH_SUCCESS);
            System.out.println("Hash Values are matched correctly.");
            queryingProtocol.SendMessage("Hash Values are matched correctly.");

            if (currentRequestType == BASIC_WEATHER_MAP) {
                //put image to file for demonstration purposes
                BufferedImage img=(BufferedImage) object;
                File outputfile = new File("src/Client/imageOutput.png");
                ImageIO.write(img, "png", outputfile);
                System.out.println("Image file created as imageOutput.png");
            } else {
                //Prettifying
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(object.toString());
                String prettyJsonString = gson.toJson(je);
                System.out.println(prettyJsonString);
            }

            setCurrentMessageType(AUTH_CHALLENGE);
        } else {
            setCurrentMessageType(AUTH_FAIL);
            System.out.println("Mismatched Hash Values!");
            queryingProtocol.SendMessage("Mismatched Hash Values!");
            setCurrentMessageType(AUTH_CHALLENGE);
        }
    }

    private boolean SendMessage(String message) throws IOException {
        if (programPhase.equals(Phase.Authentication)) {
            authenticationProtocol.SendMessage(message);
        } else {
            String[] msgList = message.split(" ");
            if (msgList.length == 4 && Integer.parseInt(msgList[0]) <= 6) {
                char requestType = message.charAt(0);
                message = message.substring(2);
                message = token + " " + message;
                switch (requestType) {
                    case '1' -> setCurrentRequestType(CURRENT_WEATHER);
                    case '2' -> setCurrentRequestType(DAILY_FORECAST);
                    case '3' -> setCurrentRequestType(BASIC_WEATHER_MAP);
                    case '4' -> setCurrentRequestType(MINUTE_FORECAST);
                    case '5' -> setCurrentRequestType(HISTORICAL_WEATHER);
                    case '6' -> setCurrentRequestType(HISTORICAL_WEATHER_WRONG);
                    default -> setCurrentRequestType(HISTORICAL_WEATHER);
                }
                queryingProtocol.SendMessage(message);
            } else {
                System.err.println("\nPlease enter appropriate request according to ex.");
                return false;
            }
        }
        return true;
    }

    public void controlPhaseAndType() {
        if (currentPhase == AUTHENTICATION_PHASE) {
            programPhase = Phase.Authentication;
        } else {
            programPhase = Phase.Querying;
        }

        switch (currentMessageType) {
            case AUTH_REQUEST -> messageType = MessageTypes.Auth_Request;
            case AUTH_CHALLENGE -> messageType = MessageTypes.Auth_Challenge;
            case AUTH_FAIL -> messageType = MessageTypes.Auth_Fail;
            default -> messageType = MessageTypes.Auth_Success;
        }

        switch (currentRequestType) {
            case CURRENT_WEATHER -> requestType = RequestTypes.Current_Weather;
            case DAILY_FORECAST -> requestType = RequestTypes.Daily_Forecast;
            case BASIC_WEATHER_MAP -> requestType = RequestTypes.Basic_Weather_Map;
            case MINUTE_FORECAST -> requestType = RequestTypes.Minute_Forecast;
            case HISTORICAL_WEATHER_WRONG -> requestType = RequestTypes.Historical_Weather_Wrong;
            default -> requestType = RequestTypes.Historical_Weather;
        }
    }

    private void closeEverything() throws IOException {
        input.close();
        output.close();
        clientSocket.close();
        NotClosed=false;
    }

    public void setCurrentPhase(byte phase) {
        currentPhase = phase;
    }

    public byte getCurrentPhase() {
        return currentPhase;
    }

    public byte getCurrentMessageType() {
        return currentMessageType;
    }

    public void setCurrentMessageType(byte type) {
        currentMessageType = type;
    }

    public byte getCurrentRequestType() {
        return currentRequestType;
    }

    public void setCurrentRequestType(byte requestType) {
        currentRequestType = requestType;
    }
}
