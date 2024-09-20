import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static Map<String, Socket> clients = new HashMap<>();
    private static String logFile = "server_log.txt";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345);
        System.out.println("Servidor rodando na porta 12345...");

        // Exibir comandos disponíveis no servidor
        showCommands();

        // Tentar abrir o arquivo de log para escrita
        try (PrintWriter logWriter = new PrintWriter(new FileWriter(logFile, true))) {
            while (true) {
                // Aceitar uma nova conexão de cliente
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();

                // Iniciar nova thread para lidar com o cliente
                new Thread(new ClientHandler(clientSocket, clientAddress, logWriter)).start();
            }
        }
    }

    public static void showCommands() {
        System.out.println("\nComandos do Servidor:");
        System.out.println("O servidor roteia mensagens entre os clientes.");
        System.out.println("Clientes podem usar os comandos /send message, /send file, /users, /sair.");
        System.out.println();
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientAddress;
        private String clientName;
        private DataInputStream in;
        private DataOutputStream out;
        private PrintWriter logWriter;

        public ClientHandler(Socket socket, String clientAddress, PrintWriter logWriter) throws IOException {
            this.socket = socket;
            this.clientAddress = clientAddress;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.logWriter = logWriter;
        }

        @Override
        public void run() {
            try {
                clientName = in.readUTF(); // Receber o nome do cliente
                logClientConnection(clientName, clientAddress, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                synchronized (clients) {
                    clients.put(clientName, socket);
                }

                System.out.println(clientName + " entrou no chat.");
                broadcast(clientName + " entrou no chat.", null);

                while (true) {
                    String command = in.readUTF();
                    if (command.equals("/send message")) {
                        handleTextMessage();
                    } else if (command.equals("/send file")) {
                        handleFileTransfer();
                    } else if (command.equals("/users")) {
                        listUsers();
                    } else if (command.equals("/sair")) {
                        break;
                    } else {
                        // Broadcast de mensagens normais
                        broadcast(clientName + ": " + command, socket);
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void handleTextMessage() throws IOException {
            String recipient = in.readUTF();
            String textMessage = in.readUTF();

            synchronized (clients) {
                if (clients.containsKey(recipient)) {
                    DataOutputStream recipientOut = new DataOutputStream(clients.get(recipient).getOutputStream());
                    recipientOut.writeUTF(clientName + ": " + textMessage);
                    recipientOut.flush();

                    out.writeUTF("Mensagem enviada para " + recipient + " com sucesso!");
                    out.flush();

                    logMessage(clientName, recipient, textMessage);
                } else {
                    out.writeUTF("Usuário " + recipient + " não está conectado.");
                    out.flush();
                }
            }
        }

        private void handleFileTransfer() throws IOException {
            String recipient = in.readUTF();
            String fileName = in.readUTF();
            long fileSize = in.readLong();

            synchronized (clients) {
                if (clients.containsKey(recipient)) {
                    try {
                        // Ler os dados do arquivo do cliente remetente
                        byte[] buffer = new byte[4096];
                        long bytesReadTotal = 0;

                        // Preparar para enviar ao destinatário
                        Socket recipientSocket = clients.get(recipient);
                        DataOutputStream recipientOut = new DataOutputStream(recipientSocket.getOutputStream());

                        // Informar ao destinatário que um arquivo está chegando
                        recipientOut.writeUTF("Arquivo recebido");
                        recipientOut.writeUTF(clientName);
                        recipientOut.writeUTF(fileName);
                        recipientOut.writeLong(fileSize);

                        // Transferir o arquivo
                        while (bytesReadTotal < fileSize) {
                            int bytesToRead = (int) Math.min(buffer.length, fileSize - bytesReadTotal);
                            int bytesRead = in.read(buffer, 0, bytesToRead);
                            if (bytesRead == -1) break;
                            recipientOut.write(buffer, 0, bytesRead);
                            bytesReadTotal += bytesRead;
                        }
                        recipientOut.flush();

                        out.writeUTF("S: Arquivo enviado com sucesso.");
                        out.flush();

                        logMessage(clientName, recipient, "S: Arquivo enviado: " + fileName);
                    } catch (IOException e) {
                        out.writeUTF("S: Erro ao enviar o arquivo para " + recipient);
                    }
                } else {
                    out.writeUTF("Usuário " + recipient + " não está conectado.");
                }
            }
        }

        private void listUsers() throws IOException {
            out.writeUTF("Usuários conectados:");
            synchronized (clients) {
                for (String user : clients.keySet()) {
                    out.writeUTF(user);
                }
            }
            out.flush();
        }

        private void broadcast(String message, Socket excludeSocket) throws IOException {
            synchronized (clients) {
                for (Socket clientSocket : clients.values()) {
                    if (clientSocket != excludeSocket) {
                        DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
                        clientOut.writeUTF(message);
                        clientOut.flush();
                    }
                }
            }
        }

        private void closeConnection() {
            try {
                if (clientName != null) {
                    System.out.println(clientName + " saiu.");
                    synchronized (clients) {
                        clients.remove(clientName);
                    }
                    broadcast(clientName + " saiu do chat.", socket);
                }
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }

        private void logClientConnection(String clientName, String clientAddress, String connectionTime) {
            logWriter.println("Cliente conectado: Nome " + clientName + ", IP " + clientAddress + " em " + connectionTime);
            logWriter.flush();
        }

        private void logMessage(String sender, String recipient, String messageContent) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logWriter.println("Mensagem enviada: " + timestamp + " De " + sender + " para " + recipient + ": " + messageContent);
            logWriter.flush();
        }
    }
}
