package Server.Phases;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.*;

public class Authentication {
    private List<String> questions; // Security Questions
    private List<String> answers; // Clients' original answers from text file

    private HashMap<String, HashMap<String, String>> clientsInfo; // All the information of clients

    private final SecureRandom random = new SecureRandom();

    public Authentication() {
        getFileInfo();
    }

    private void getFileInfo() {
        questions = new ArrayList<String>();
        answers = new ArrayList<String>();

        clientsInfo = new HashMap<String, HashMap<String, String>>();

        try {
            Scanner questionScanner = new Scanner(new File("questions.txt"));

            while (questionScanner.hasNextLine()) {
                String question = questionScanner.nextLine();
                questions.add(question);
            }

            String client;
            Scanner answerScanner = new Scanner(new File("client_answers.txt"));

            while (answerScanner.hasNextLine()) {
                HashMap<String, String> clientAnswers = new HashMap<String, String>();
                answers.clear();

                StringTokenizer line = new StringTokenizer(answerScanner.nextLine(), ",");

                while (line.hasMoreTokens()) {
                    answers.add(line.nextToken().trim());
                }
                client = answers.get(0);

                answers.remove(client);
                for (int i = 0; i < questions.size(); i++) {
                    clientAnswers.put(questions.get(i), answers.get(i));
                }
                clientsInfo.put(client, clientAnswers);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String chooseQuestion() {
        int randNum = random.nextInt(questions.size());
        String question=questions.get(randNum);
        questions.remove(randNum);
        return question;
    }

    public boolean controlUsername(String username) {
        return clientsInfo.containsKey(username);
    }

    public boolean compare(String username, String question, String userAnswer) {
        String answer = clientsInfo.get(username).get(question);
        return answer.equalsIgnoreCase(userAnswer);
    }
}
