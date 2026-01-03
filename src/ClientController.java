import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**************************************************************
 * The Controller of the MVC Architecture.
 * This handles network connections and _writes_ to the Model.
 **************************************************************/

public class ClientController {

    // private final ChatModel model;
    private final VNSCPClient client;

    private final ExecutorService senderExec = Executors.newSingleThreadExecutor();

    // Since VNSCP is an ASCII-encoded,
    // text-based protocol we shall use Writers and Readers.
    private PrintWriter cmdWriter;
    private BufferedReader cmdReader;

    public ClientController(VNSCPClient client) {
        this.client = client;
    }

    // Attempt to set up the command channel TCP connection with the VNSCP Server.
    public void connectCommand(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        OutputStream cmdout = s.getOutputStream();
        PrintWriter cmdpw = new PrintWriter(cmdout);
        InputStream cmdin = s.getInputStream();
        BufferedReader cmdbr = new BufferedReader(new InputStreamReader(cmdin));
        cmdWriter = cmdpw;
        cmdReader = cmdbr;
    }

    // Submits the task to the sender thread pool of sending the given String as a message to the server.
    // Intended to be called by the View.
    public void sendMessage(String msgContent) {
        senderExec.submit(() -> {
            try {
                System.out.println("Sending message: " + msgContent);

                cmdWriter.println("SEND VNSCP/1.0");
                cmdWriter.print("Text: ");
                cmdWriter.println(msgContent);

                String response = cmdReader.readLine();

                //** DEBUG **
                System.out.println(response);

                if (response != null && response.startsWith("SENT")) {
                    // awesome
                }
                else if (response != null && response.startsWith("EXPIRED")) {
                    handleTimeout();
                }
                else if (response != null && response.startsWith("ERROR")) {
                    handleError(response);
                }
                else {

                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                handleError(""); // "": Handle it in a very general way.
            }
        });
    }

    private void handleError(String response) {}; //TODO

    private void handleTimeout() {}; //TODO
}