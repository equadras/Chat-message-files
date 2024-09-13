import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
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
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;

            while ((message = in.readLine()) != null) {
                //System.out.println("Mensagem recebida: " + message);

                if (message.startsWith(":Arquivo recebido:")) {
                    receiveFile(in, message.split(":")[2].trim());
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

    private void receiveFile(BufferedReader in, String fileName) {
        try {
            System.out.println("Recebendo arquivo: " + fileName);
            createReceivedFilesDirectory();

            File file = new File("arquivos_recebidos" + File.separator + fileName);
            try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file))) {
                int bytesRead;
                char[] buffer = new char[8192];
                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(new String(buffer, 0, bytesRead).getBytes());
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
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.print("Digite seu nome: ");
            String name = scanner.nextLine();
            out.println(name);

            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("/sair")) {
                    out.println("saindo...");
                    socket.close();
                    break;
                }

                if (message.startsWith("/send file")) {
                    String[] tokens = message.split(" ", 4);
                    if (tokens.length == 4) {
                        sendFile(out, tokens[2], tokens[3]);
                    } else {
                        for (int i = 0; i < tokens.length; i++) {
                            System.out.println(tokens[i]);
                        }
                        System.out.println("Comando incorreto. Use: /send file <destinatario> <caminho do arquivo>");
                    }
                } else {
                    out.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    private void sendFile(PrintWriter out, String recipient, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Arquivo não encontrado no caminho: " + file.getAbsolutePath());
            return;
        }

        try {
            out.println("/send file " + recipient + " " + file.getName());
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            OutputStream socketOut = socket.getOutputStream();
            socketOut.write(fileBytes);
            socketOut.flush();
            System.out.println("Arquivo enviado com sucesso!");
        } catch (IOException e) {
            System.out.println("Erro ao enviar arquivo: " + e.getMessage());
        }
    }
}
