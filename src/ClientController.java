import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**************************************************************
 * The Controller of the MVC Architecture.
 * This handles network connections and _writes_ to the Model.
 **************************************************************/

public class ClientController {

    // private final ChatModel model;
    private VNSCPClient view;

    private final ExecutorService senderExec = Executors.newSingleThreadExecutor();

    // Since VNSCP is an ASCII-encoded,
    // text-based protocol we shall use Writers and Readers.
    private PrintWriter cmdWriter;
    private BufferedReader cmdReader;

    // Attempt to set up the command channel TCP connection with the VNSCP Server.
    public void connectCommand(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        OutputStream cmdout = s.getOutputStream();
        PrintWriter cmdpw = new PrintWriter(cmdout);
        InputStream cmdin = s.getInputStream();
        BufferedReader cmdbr = new BufferedReader(new InputStreamReader(cmdin));
        //** DEBUG **
        System.out.println("Connected to host.");
        cmdWriter = cmdpw;
        cmdReader = cmdbr;
    }

    // Given a username attempt to log in with this username.
    // Input validation regarding the username is the responsibility of the View.
    public void login(String username) {
        senderExec.submit(() -> {
                    try {
                        //** DEBUG **
                        System.out.println("Logging in as: " + username);

                        cmdWriter.print("LOGIN VNSCP/1.0");
                        cmdWriter.print("\r\n");
                        cmdWriter.print("Username: ");
                        cmdWriter.print(username);
                        cmdWriter.print("\r\n");
                        cmdWriter.print("\r\n");

                        cmdWriter.flush();

                        //** DEBUG **

                        String responseStatus = cmdReader.readLine();
                        HashMap<String, String> responseHeaders = new HashMap<>();
                        String line;
                        String[] header;

                        // Parse all headers and store them in the HashMap responseHeaders.
                        // HashMap is convenient here because the Client MUST NOT assume that these headers are in any pre-defined order.
                        while (!((line = cmdReader.readLine()).isEmpty()))
                        {
                            header = line.replaceAll("\\s", "").split(":");
                            responseHeaders.put(header[0], header[1]);
                        };

                        if (responseStatus != null && responseStatus.contains("ERROR")) {
                            handleError(responseHeaders.get("Reason"));
                        }

        } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
        });
    }

    // Submits the task to the sender thread pool of sending the given String as a message to the server.
    // Input validation regarding the message content is the responsibility of the View.
    public void sendMessage(String msgContent) {
        senderExec.submit(() -> {
            try {
                //** DEBUG **
                System.out.println("Sending message: " + msgContent);

                cmdWriter.print("SEND VNSCP/1.0");
                cmdWriter.print("\r\n");
                cmdWriter.print("Text: ");
                cmdWriter.print(msgContent);
                cmdWriter.print("\r\n");
                cmdWriter.print("\r\n");

                cmdWriter.flush();

                String responseStatus = cmdReader.readLine();
                HashMap<String, String> responseHeaders = new HashMap<>();
                String line;
                String[] header;

                // Parse all headers and store them in the HashMap responseHeaders.
                // HashMap is convenient here because the Client MUST NOT assume that these headers are in any pre-defined order.
                while (!((line = cmdReader.readLine()).isEmpty()))
                {
                    header = line.replaceAll("\\s", "").split(":");
                    responseHeaders.put(header[0], header[1]);
                };

                if (responseStatus != null && responseStatus.startsWith("EXPIRED")) {
                    // This means the client has timed out but is still connected.
                    handleTimeout();
                }
                else if (responseStatus != null && responseStatus.startsWith("ERROR")) {
                    handleError(responseHeaders.get("Reason"));
                }
                else {

                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                handleError(""); // "": Handle it in a very general way.
            }
        });
    }

    public void setView(VNSCPClient view) {this.view = view;}

    private void handleError(String response) {
        System.out.println("Handle error");
        System.out.println(response);} //TODO

    private void handleTimeout() {System.out.println("Handle timeout");}; //TODO
}