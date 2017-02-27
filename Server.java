package chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by я on 13.02.2017.
 */
public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {

        ConsoleHelper.writeMessage("Введите порт сервера: ");
        int serverPort = ConsoleHelper.readInt();

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {

            ConsoleHelper.writeMessage("Сервер запущен");

            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        }
    }
    public static void sendBroadcastMessage(Message message) {

        try {

            for (Connection connection : connectionMap.values()) {
                connection.send(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            ConsoleHelper.writeMessage("Сообщение не отправлено");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;
        public Handler(Socket socket) {
            this.socket = socket;
        }
        private void sendListOfUsers(Connection connection, String userName) throws IOException{
            for(String name:connectionMap.keySet()){
                if(!name.equals(userName)){
                    connection.send(new Message(MessageType.USER_ADDED,name));
                }
            }
        }
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while(true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    String s = userName+": "+message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, s));
                } else ConsoleHelper.writeMessage("Ошибка!");
            }
        }
        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if (message.getType() == MessageType.USER_NAME) {
                    if (message.getData() != null) {
                        if (connectionMap.get(message.getData()) == null) {
                            connectionMap.put(message.getData(), connection);
                            connection.send(new Message(MessageType.NAME_ACCEPTED));
                            return message.getData();
                        }
                    }
                }
            }
        }
        public void run(){
            ConsoleHelper.writeMessage("Установлено соединение с адресом: "+socket.getRemoteSocketAddress());
            String newClient=null;
            try( Connection connection = new Connection(socket)) {
                newClient=serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED,newClient));
                sendListOfUsers(connection,newClient);
                serverMainLoop(connection,newClient);
            }
            catch (IOException e){ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом IO");}
            catch (ClassNotFoundException a){ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом CFE");}
            connectionMap.remove(newClient);
            sendBroadcastMessage(new Message(MessageType.USER_REMOVED, newClient));
            ConsoleHelper.writeMessage("Соединение закрыто");

        }
    }
}
