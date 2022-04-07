import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ClientHandler implements Runnable{

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String userName;
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<ClientHandler>();


    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            userName = dataInputStream.readUTF();
            clientHandlers.add(this);
            broadcast("[SERVER]: New client " + userName + " has joined the chat!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String msg) {
        for (ClientHandler clientHandler: clientHandlers) {
            if (!clientHandler.equals(this)) {
                try {
                    clientHandler.dataOutputStream.writeUTF(msg);
                    clientHandler.dataOutputStream.flush();
                } catch (IOException e) {
                    endConnection(socket, dataInputStream, dataOutputStream);
                }
            }
        }
    }

    @Override
    public void run() {
        String msg;
        while (socket.isConnected()) {
            try {
                msg = dataInputStream.readUTF();
                if (msg.charAt(0) != '/') {
                    broadcast(userName + ": " + msg);
                } else {
                    command(msg);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
                endConnection(socket, dataInputStream, dataOutputStream);
                break;
            }
        }
    }

    private void command(String msg) {

        String[] parts = msg.split(" ");
        switch (parts[0]) {
            case "/send":
                String message = userName + ": ";
                for ( int i = 2; i < parts.length; i++ ) {
                    message += parts[i] + " ";
                }
                send(parts[1], message);
                break;
            case "/users_list":
                try {
                    showUsers();
                } catch (IOException e) {
                    System.out.println("Error while loading list");
                }
                break;
            case "/show_commands":
                try {
                    dataOutputStream.writeUTF(loadCommandList());
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                invalidCommand();
                break;
        }

    }

    private String loadCommandList() {
        return "/send <username> <message> - send message to <username>\n" +
                "/show_commands - show the command list\n" +
                "/users_list - show list of all users\n" +
                "/exit - exit the chat\n";
    }

    private void invalidCommand() {
        try {
            dataOutputStream.writeUTF("Invalid command. Only following commands are accepted\n"+loadCommandList());
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void showUsers() throws IOException {
        String list = "";
        int cnt = 0;
        for (ClientHandler clientHandler: clientHandlers) {
            list += clientHandler.userName + "\n";
            cnt++;
        }
        list = "Total number of users : " + cnt + "\n" + list;
        dataOutputStream.writeUTF(list);
        dataOutputStream.flush();
    }

    private void send(String username, String message) {
        for (ClientHandler clientHandler: clientHandlers) {
            if (clientHandler.userName.equals(username)) {
                try {
                    clientHandler.dataOutputStream.writeUTF(message);
                    clientHandler.dataOutputStream.flush();
                } catch (IOException e) {
                    endConnection(socket, dataInputStream, dataOutputStream);
                }
            }
        }
    }
    private void endConnection(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {

        if (removeUser()) {
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
    }

    private boolean removeUser() {
        Path path = Paths.get("files/" + userName);
        if (path.toFile().exists()) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("User " + userName + " is leaving the chat");
            broadcast("[SERVER]: " + userName + " left the chat!");
            clientHandlers.remove(this);
            return true;
        }
        System.out.println(userName + " has already left the chat");
        return false;
    }
}