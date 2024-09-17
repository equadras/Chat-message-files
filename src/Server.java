import java.io.*;
import java.net.*;
import java.nio.file.Files;
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
                //aceita connection com cliente
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();

                // inicia nova thread ClientHandler
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
        private BufferedReader in;
        private PrintWriter out;
        private PrintWriter logWriter;

        public ClientHandler(Socket socket, String clientAddress, PrintWriter logWriter) throws IOException {
            this.socket = socket;
            this.clientAddress = clientAddress;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.logWriter = logWriter;
        }

        @Override
        public void run() {
            try {
                clientName = in.readLine(); //  nome do cliente
                logClientConnection(clientName, clientAddress, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                //uma thread espera a outra para alterar o map
                synchronized (clients) {
                    clients.put(clientName, socket);
                }

                System.out.println(clientName + " entrou no chat.");
                broadcast(clientName + " entrou no chat.", null);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/send message")) {
                        handleTextMessage(message);
                    }
                    else if (message.startsWith("/send file")) {
                        handleFileTransfer(message);
                    }
                    else if (message.equals("/users")) {
                        listUsers();
                    }
                    else if (message.equals("/sair")) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void handleTextMessage(String message) {
            String[] tokens = message.split(" ", 4);
            if (tokens.length < 4) {
                out.println("Formato correto: /send message <destinatario> <mensagem>");
                return;
            }

            String recipient = tokens[2];
            String textMessage = tokens[3];

            //uma thread espera a outra para alterar o map
            synchronized (clients) {
                if (clients.containsKey(recipient)) {
                    try {
                        //abre um OutputStream pro socket do destinatario
                        PrintWriter recipientOut = new PrintWriter(clients.get(recipient).getOutputStream(), true);
                        recipientOut.println(clientName + ": " + textMessage);
                        out.println("Mensagem enviada para " + recipient + " com sucesso!");
                        logMessage(clientName, recipient, textMessage);

                    } catch (IOException e) {
                        out.println("Erro ao enviar mensagem para " + recipient);
                    }
                } else {
                    out.println("Usuário " + recipient + " não está conectado.");
                }
            }
        }


        private void handleFileTransfer(String message) throws IOException {
            String[] tokens = message.split(" ", 4);
            if (tokens.length < 4) {
                out.println("Formato incorreto: /send file <destinatario> <caminho do arquivo>");
                return;
            }

            String recipient = tokens[2];
            String fileName = tokens[3];

            synchronized (clients) {
                if (clients.containsKey(recipient)) {
                    try {
                        InputStream socketIn = socket.getInputStream();
                        saveReceivedFile(fileName, socketIn);

                        PrintWriter recipientOut = new PrintWriter(clients.get(recipient).getOutputStream(), true);
                        recipientOut.println("Arquivo recebido: " + fileName);

                        byte[] fileBytes = Files.readAllBytes(new File("arquivos_recebidos" + File.separator + fileName).toPath());
                        OutputStream recipientStream = clients.get(recipient).getOutputStream();
                        recipientStream.write(fileBytes);
                        recipientStream.flush();

                        logMessage(clientName, recipient, "Arquivo enviado: " + fileName);
                    } catch (IOException e) {
                        out.println("" + "Erro ao enviar o arquivo para " + recipient);
                    }
                } else {
                    out.println("Usuário " + recipient + " não está conectado.");
                }
            }
        }

        private void saveReceivedFile(String fileName, InputStream socketIn) {
            createReceivedFilesDirectory();
            File file = new File("arquivos_recebidos" + File.separator + fileName);

            try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while (socketIn.available() > 0 && (bytesRead = socketIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                fileOut.flush();
                System.out.println("Arquivo " + fileName + " salvo com sucesso em 'arquivos_recebidos'!");
            } catch (IOException e) {
                System.out.println("Erro ao salvar arquivo: " + e.getMessage());
            }
        }


        private void createReceivedFilesDirectory() {
            File receivedFilesDir = new File("arquivos_recebidos");
            if (!receivedFilesDir.exists()) {
                if (receivedFilesDir.mkdir()) {
                    System.out.println("Diretório 'arquivos_recebidos' criado.");
                }
            }
        }

        private void listUsers() {
            out.println("Usuários conectados:");
            synchronized (clients) {
                for (String user : clients.keySet()) {
                    out.println(user);
                }
            }
        }

        private void broadcast(String message, Socket excludeSocket) throws IOException {
            synchronized (clients) {
                for (Socket clientSocket : clients.values()) {
                    if (clientSocket != excludeSocket) {
                        PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                        clientOut.println(message);
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