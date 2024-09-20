import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 12345);
        System.out.println("Cliente conectado ao servidor!");

        // Exibir os comandos
        showCommands();

        new Thread(new ListenServer(socket)).start();
        new Thread(new SendToServer(socket)).start();
    }

    public static void showCommands() {
        System.out.println("\nComandos disponíveis:");
        System.out.println("/send message <destinatario> <mensagem> - Enviar uma mensagem para um destinatário.");
        System.out.println("/send file <destinatario> <caminho do arquivo> - Enviar um arquivo para um destinatário.");
        System.out.println("/users - Listar todos os usuários conectados.");
        System.out.println("/sair - Sair do chat.");
        System.out.println();
    }
}

class ListenServer implements Runnable {
    private Socket socket;

    public ListenServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (true) {
                String message = in.readUTF();

                if (message.equals("Arquivo recebido")) {
                    String sender = in.readUTF();
                    String fileName = in.readUTF();
                    long fileSize = in.readLong();
                    receiveFile(sender, fileName, fileSize, in);
                } else {
                    System.out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao escutar servidor: " + e.getMessage());
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

    private void receiveFile(String sender, String fileName, long fileSize, DataInputStream in) {
        try {
            System.out.println("Recebendo arquivo de " + sender + ": " + fileName);
            createReceivedFilesDirectory();

            File file = new File("arquivos_recebidos" + File.separator + fileName);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                long bytesReadTotal = 0;

                while (bytesReadTotal < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - bytesReadTotal);
                    int bytesRead = in.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) break;
                    fileOut.write(buffer, 0, bytesRead);
                    bytesReadTotal += bytesRead;
                }
                System.out.println("Arquivo " + fileName + " salvo com sucesso em 'arquivos_recebidos'!");
            }
        } catch (IOException e) {
            System.out.println("Erro ao receber arquivo: " + e.getMessage());
        }
    }
}

class SendToServer implements Runnable {
    private Socket socket;

    public SendToServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(System.in);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            System.out.print("Digite seu nome: ");
            String name = scanner.nextLine();
            out.writeUTF(name);
            out.flush();

            while (true) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("/sair")) {
                    out.writeUTF("/sair");
                    out.flush();
                    socket.close();
                    break;
                } else if (message.startsWith("/send message")) {
                    String[] tokens = message.split(" ", 3);
                    if (tokens.length >= 3) {
                        String recipient = tokens[1];
                        String textMessage = tokens[2];

                        out.writeUTF("/send message");
                        out.writeUTF(recipient);
                        out.writeUTF(textMessage);
                        out.flush();
                    } else {
                        System.out.println("Comando incorreto. Use: /send message <destinatario> <mensagem>");
                    }
                } else if (message.startsWith("/send file")) {
                    String[] tokens = message.split(" ", 4);
                    if (tokens.length >= 4) {
                        sendFile(out, tokens[2], tokens[3]);
                    } else {
                        System.out.println("Comando incorreto. Use: /send file <destinatario> <caminho do arquivo>");
                    }
                } else if (message.equals("/users")) {
                    out.writeUTF("/users");
                    out.flush();
                } else {
                    out.writeUTF(message);
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    private void sendFile(DataOutputStream out, String recipient, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Arquivo não encontrado no caminho: " + file.getAbsolutePath());
            return;
        }

        try {
            out.writeUTF("/send file");
            out.writeUTF(recipient);
            out.writeUTF(file.getName());
            out.writeLong(file.length());
            out.flush();

            byte[] buffer = new byte[4096];
            try (FileInputStream fileIn = new FileInputStream(file)) {
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.flush();
            System.out.println("Arquivo enviado com sucesso!");
        } catch (IOException e) {
            System.out.println("Erro ao enviar arquivo: " + e.getMessage());
        }
    }
}
