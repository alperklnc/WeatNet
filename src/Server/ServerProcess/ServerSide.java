package Server.ServerProcess;

import Server.DataTransform.DataServer;
import Server.Protocols.Protocol;
import Server.Protocols.ProtocolFactory;
import Server.Useful.*;
import Server.Phases.Authentication;
import Server.Phases.Querying;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.StringTokenizer;

public class ServerSide implements Runnable, AuthProtocol, QueryProtocol {

    protected DataInputStream input;
    protected DataOutputStream output;

    protected Socket clientSocket;

    private String Username;
    private String Token;
    private String rejectMessage="Connection rejected";

    private Phase programPhase;
    private MessageTypes messageType;
    private RequestTypes requestType;


    private byte currentPhase = 0x00;
    private byte currentMessageType;
    private byte currentRequestType;

    private int currentHashCode;


    private Authentication authentication;

    private final int QUESTION_AMOUNT = 3;
    private boolean check = true;

    private Protocol protocol;

    public ServerSide(Socket s) {
        this.clientSocket = s;
    }


    @Override
    public void run() {
        try {
            input = new DataInputStream(clientSocket.getInputStream());
            output = new DataOutputStream(clientSocket.getOutputStream());
            authentication = new Authentication();
            protocol = ProtocolFactory.getProtocol(currentPhase, this, input, output);

        } catch (IOException e) {
            System.err.println("Server Thread. Run. IO error in server thread");
        }

        try {
            //timeout
            //clientSocket.setSoTimeout(10000);

            System.out.println("Server is Connected to Client");
            protocol.SendMessage("Enter UserName:");

            while (check) {
                checkThePhase();
                protocol = ProtocolFactory.getProtocol(currentPhase, this, input, output);
                String request = protocol.GetByteAndUnwrap();

                if (programPhase == Phase.Authentication) {
                    currentMessageType = AUTH_CHALLENGE_RESPONSE;
                    AuthenticationPhase(request);
                    check = sendSuccessOrFailMessage(rejectMessage);
                } else {
                    QueryingPhase(request);
                }
            }

        } catch (SocketTimeoutException s) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Server Thread. Run. IO Error/ Client terminated abruptly");
        } catch (NullPointerException e) {
            System.err.println("Server Thread. Run.Client Closed");
        } finally {
            try {
                System.out.println("Closing the connection");
                if (input != null) {
                    input.close();
                    System.err.println("Socket Input Stream Closed");
                }

                if (output != null) {
                    output.close();
                    System.err.println("Socket Out Closed");
                }
                if (clientSocket != null) {
                    clientSocket.close();
                    System.err.println("Socket Closed");
                }

            } catch (IOException ie) {
                System.err.println("Socket Close Error");
            }
        }
    }

    private void checkThePhase() throws IOException {

        byte[] phase = new byte[1];
        int err = input.read(phase, 0, phase.length);
        if (err == -1) throw new EOFException();

        currentPhase = phase[0];
        controlPhaseAndType();
    }

    private void QueryingPhase(String query) throws IOException {
        if(currentMessageType == AUTH_FAIL){
            System.out.println(query);
            return;
        } else if(currentMessageType == AUTH_SUCCESS) {
            System.out.println(query);
            return;
        }
        StringTokenizer stn = new StringTokenizer(query, " ");

        String token = stn.nextToken();
        String lon = stn.nextToken();
        String lat = stn.nextToken();
        String id = stn.nextToken();
        String requestTyp = switch (currentRequestType) {
            case CURRENT_WEATHER -> "current";
            case DAILY_FORECAST -> "daily";
            case BASIC_WEATHER_MAP -> "map";
            case MINUTE_FORECAST -> "minutely";
            case HISTORICAL_WEATHER -> "history";
            case HISTORICAL_WEATHER_WRONG ->"wrong";
            default -> "history";
        };
        if (token.equals(Token)) {
            System.out.println("Client asks the data of "+requestTyp);
            boolean wrong = false;
            if(requestTyp.equals("wrong")){
                setCurrentRequestType(HISTORICAL_WEATHER);
                requestTyp="history";
                wrong=true;
            }
            Object obj = Querying.getResponse(id, lon, lat, requestTyp);
            if(obj==null){
                protocol.SendMessage("Fail");
                return;
            }
            if(wrong){
                SendWrongHashCode(requestTyp, obj);
            }else{
                SendHashCode(requestTyp, obj);
            }

            SendOnDataSocket(requestTyp, obj);
        } else {
            protocol.SendMessage("Fail");
        }
    }


    private void AuthenticationPhase(String username) throws IOException {
        String userAnswer;

        boolean correctUserName = authentication.controlUsername(username);
        boolean isCorrect = true;
        String question="";
        if (correctUserName) {
            this.Username = username;
            for (int i = 0; i < QUESTION_AMOUNT && isCorrect; i++) {
                question = authentication.chooseQuestion();
                protocol.SendMessage(question);
                checkThePhase();
                userAnswer = protocol.GetByteAndUnwrap();
                isCorrect = authentication.compare(username, question, userAnswer);
            }
            if (isCorrect) {
                currentMessageType = AUTH_SUCCESS;
            } else {
                rejectMessage="Invalid Response to the question: "+question+"\nConnection Rejected";
                currentMessageType = AUTH_FAIL;
            }
        } else {
            rejectMessage="Invalid Username\nConnection Rejected";
            currentMessageType = AUTH_FAIL;
        }
        controlPhaseAndType();
    }

    private void closeEverything() throws IOException {
        input.close();
        output.close();
        clientSocket.close();
    }

    private boolean sendSuccessOrFailMessage(String message) throws IOException {
        if (programPhase == Phase.Authentication) {
            if (messageType == MessageTypes.Auth_Success) {
                createAndSaveToken();
                protocol.SendMessage(String.valueOf(Token));
                return true;
            } else if (messageType == MessageTypes.Auth_Fail) {
                protocol.SendMessage(message);
                closeEverything();
                return false;
            }
        }
        return false;
    }

    private void createAndSaveToken() {
        Random random = new Random();
        int randNum = random.nextInt(1000000) + 1000000;
        this.Token = String.valueOf(Username.hashCode() + randNum).substring(0, 6);
    }

    private void SendOnDataSocket(String requestTyp, Object obj) throws IOException {
        DataServer dataServer = new DataServer(5000);
        if (requestTyp.equals("map")) {
            dataServer.sendImage((BufferedImage) obj);
        } else {
            dataServer.sendData(obj.toString());
        }
        dataServer.closeClient();
        dataServer.close();
    }

    private void SendHashCode(String requestTyp, Object obj) throws IOException {
        if (obj == null) {
            protocol.SendMessage("Fail");
        } else if (requestTyp.equals("map")) {
            createHashCodeImage((BufferedImage) obj);
            protocol.SendMessage(""+currentHashCode);
        } else {
            setHashCode(obj.toString().hashCode());
            protocol.SendMessage(""+currentHashCode);
        }
    }

    private void SendWrongHashCode(String requestTyp, Object obj) throws IOException {
        int defaultHashcode=1000;
        protocol.SendMessage(""+defaultHashcode);
    }

    private void createHashCodeImage(BufferedImage obj) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(obj, "png", bos);
            bos.flush();
            byte [] data = bos.toByteArray();
            int sum=0;
            for (int i=0;i<data.length;i++){
                sum+=data[i];
            }
        setHashCode(sum);
        }


    private void setHashCode(int hashcode){
        currentHashCode=hashcode;
    }


    public void controlPhaseAndType() {

        if (currentPhase == AUTHENTICATION_PHASE) {
            programPhase = Phase.Authentication;
        } else {
            programPhase = Phase.Querying;
        }

        switch (currentMessageType) {
            case AUTH_REQUEST -> messageType = MessageTypes.Auth_Request;
            case AUTH_CHALLENGE_RESPONSE -> messageType = MessageTypes.Auth_Challenge_Response;
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

    public byte getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(byte currentPhase) {
        this.currentPhase = currentPhase;
    }

    public byte getCurrentMessageType() {
        return currentMessageType;
    }

    public void setCurrentMessageType(byte currentMessageType) {
        this.currentMessageType = currentMessageType;
    }

    public byte getCurrentRequestType() {
        return currentRequestType;
    }

    public void setCurrentRequestType(byte currentRequestType) {
        this.currentRequestType = currentRequestType;
    }
}