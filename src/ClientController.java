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
    private ClientModel model;

    private final ExecutorService senderExec = Executors.newSingleThreadExecutor();
    private Thread listenerThread;
    private Thread pingThread;

    // Since VNSCP is an ASCII-encoded,
    // text-based protocol we shall use Writers and Readers.
    private PrintWriter cmdWriter;
    private BufferedReader cmdReader;
    private BufferedReader pubsubReader;

    // Attempt to set up the publish/subscribe channel TCP connection with the VNSCP Server.
    public void connectPubSub(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        InputStream pubsubin = s.getInputStream();
        pubsubReader = new BufferedReader(new InputStreamReader(pubsubin));

        listenerThread = new Thread(this::listenForMessages);
        listenerThread.start();
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

        pingThread = new Thread(this::pingServerPeriodically);
        pingThread.start();
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

                        HashMap<String, String> response = parseServerResponse(cmdReader);

                        //TODO handle errors

        } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
        });
    }

    public void getOnlineUsers() {
        senderExec.submit(() -> {
                    try {
                        //** DEBUG **
                        System.out.println("fetching users");

                        cmdWriter.print("PING VNSCP/1.0");
                        cmdWriter.print("\r\n");
                        cmdWriter.print("\r\n");

                        cmdWriter.flush();

                        HashMap<String, String> response = parseServerResponse(cmdReader);

                        String[] users = response.get("Usernames").trim().split(",");
                        model.updateUsers(users);

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

                HashMap<String, String> response = parseServerResponse(cmdReader);

                //TODO handle errors

            } catch (IOException ioe) {
                ioe.printStackTrace();
                handleError(""); // "": Handle it in a very general way.
            }
        });
    }

    private HashMap<String, String> parseServerResponse(BufferedReader reader) throws IOException {
        String responseStatus = reader.readLine();
        responseStatus = responseStatus.split(" ")[1];
        HashMap<String, String> responseHeaders = new HashMap<>();
        String line;
        String[] header;

        responseHeaders.put("STATUS", responseStatus);

        // Parse all headers and store them in the HashMap responseHeaders.
        // HashMap is convenient here because the Client MUST NOT assume that these headers are in any pre-defined order.
        while (!((line = reader.readLine()).isEmpty()))
        {
            header = line.split(":");
            responseHeaders.put(header[0].trim(), header[1].trim());
        };
        return responseHeaders;
    }

    // Intended for the ping thread which shall submit a task
    // to ping the server periodically to the send Executor every 15 seconds.
    public void pingServerPeriodically() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(60000);
                senderExec.submit(this::getOnlineUsers);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    // Let's take a listen 👂
    public void listenForMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("Hello listener thread");
                HashMap <String, String> response = parseServerResponse(pubsubReader);
                System.out.println("DEBUG listener: " + response.get("STATUS"));
                String status;
                if ((status = response.get("STATUS")) != null) {
                    switch (status) {
                        case "MESSAGE":
                            System.out.println("DEBUG listener: enter MESSAGE switch case");
                            Message message = new Message(response.get("Username"), response.get("Text"),
                                    Integer.parseInt(response.get("Id")), response.get("Date"));
                            model.addMessage(message);
                            break;
                        case "EVENT":
                            String desc = response.get("Description");
                            System.out.println("DEBUG listener: enter EVENT switch case");
                            Message alert = new Message("SYSTEM", desc,
                                    Integer.parseInt(response.get("Id")), response.get("Date"));
                            model.addMessage(alert);
                            getOnlineUsers();
                            break;
                        default:

                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void setView(VNSCPClient view) {this.view = view;}
    public void setModel(ClientModel model) {this.model = model;}

    private void handleError(String response) {
        System.out.println("Handle error");
        System.out.println(response);} //TODO

    private void handleTimeout() {System.out.println("Handle timeout");}; //TODO
}