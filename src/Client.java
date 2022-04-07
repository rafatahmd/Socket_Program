import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private String username;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public Client(Socket socket, String username) {
        this.socket = socket;
        this.username = username;
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(username);
            dataOutputStream.flush();
        } catch (IOException e) {
            endConnection(socket, dataInputStream, dataOutputStream);
        }
    }

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Please enter your username: ");
            String userName = scanner.nextLine();
            while (invalid(userName)) {
                userName = scanner.nextLine();
            }
            System.out.println("Welcome " + userName + "! You can now chat with others connected with you!!");
            Socket socket = new Socket("localhost", 5000);
            Client client = new Client(socket, userName);
            client.listen();
            client.send();
        } catch (IOException e) {
            System.out.println("Could not connect to Server");
        }
    }

    private void send() {
        try {
            try (Scanner scanner = new Scanner(System.in)) {
                while (socket.isConnected()) {
                    String msg = scanner.nextLine();
                    String[] parts = msg.split(" ");
                    switch (parts[0]) {
                        case "/exit":
                            endConnection(socket, dataInputStream, dataOutputStream);
                            break;
                        default:
                            dataOutputStream.writeUTF(msg);
                            dataOutputStream.flush();
                            break;
                    }
                }
            }
        }  catch (Exception e) {
            e.printStackTrace();
            endConnection(socket, dataInputStream, dataOutputStream);
        }

    }

    private void listen() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String msg;
                try {
                    while (socket.isConnected()) {
                        msg = dataInputStream.readUTF();
                        System.out.println(msg);
                    }
                    endConnection(socket, dataInputStream, dataOutputStream);
                } catch (IOException e) {
                    endConnection(socket, dataInputStream, dataOutputStream);
                }
            }
        }).start();
    }

    private void endConnection(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        System.out.println(username + " is leaving the chat room!");
        try {
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if(dataOutputStream != null) {
                dataOutputStream.close();
            }
            if(socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean invalid(String userName) {
        for (Character character : userName.toCharArray()) {
            if (!Character.isLetterOrDigit(character) && character != '_') {
                System.out.println("Username should only contain letters, digits or _\nTry another name");
                return true;
            }
        }
        try {
            File file = new File("files/"+userName);
            if(file.exists()){
                System.out.println("Username already exists");
                return true;
            }
            file.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}