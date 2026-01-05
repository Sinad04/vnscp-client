import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**************************************************************
 * The Controller of the MVC Architecture.
 * This handles network connections and _writes_ to the Model.
 **************************************************************/

public class ClientController {

    private VNSCPClient view;
    private ClientModel model;

    private final ExecutorService senderExec = Executors.newSingleThreadExecutor();
    private Thread listenerThread;
    private Thread pingThread;

    private Socket cmdSocket;
    private Socket pubSubSocket;

    // Since VNSCP is an ASCII-encoded,
    // text-based protocol we shall use Writers and Readers.
    private PrintWriter cmdWriter;
    private BufferedReader cmdReader;
    private BufferedReader pubSubReader;

    // Attempt to set up the publish/subscribe channel TCP connection with the VNSCP Server.
    public void connectPubSub(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        InputStream pubsubin = s.getInputStream();

        pubSubSocket = s;
        pubSubReader = new BufferedReader(new InputStreamReader(pubsubin));

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

        cmdSocket = s;
        cmdWriter = cmdpw;
        cmdReader = cmdbr;

        pingThread = new Thread(this::pingServerPeriodically);
        pingThread.start();
    }

    // Given a username attempt to log in with this username.
    // Input validation regarding the username is the responsibility of the View.
    public Future<Boolean> login(String username) {
        return senderExec.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    cmdWriter.print("LOGIN VNSCP/1.0");
                    cmdWriter.print("\r\n");
                    cmdWriter.print("Username: ");
                    cmdWriter.print(username);
                    cmdWriter.print("\r\n");
                    cmdWriter.print("\r\n");

                    cmdWriter.flush();

                    HashMap<String, String> response = parseServerResponse(cmdReader);

                    String status = response.get("STATUS");

                    switch (status) {
                        case "LOGGEDIN":
                            connectPubSub("vns.lxd-vs.uni-ulm.de", 8123);
                            return true;
                        case "ERROR":
                            System.out.println("Handle error case: " + response.get("Reason"));
                            handleError(response.get("Reason"));
                            break;
                        default:
                            handleError("An unknown error occurred.");
                            break;

                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                return false;
            }
        });
    }

    // Sends a BYE command to the server and then cleanly closes BOTH connections and sockets (cmd and pub/sub).
    public void byeAndShutDown() {
        senderExec.submit(() -> {
            try {
                cmdWriter.print("BYE VNSCP/1.0");
                cmdWriter.print("\r\n");
                cmdWriter.print("\r\n");

                cmdWriter.flush();


                // Cmd Channel
                cmdSocket.shutdownInput();
                pingThread.interrupt();
                pingThread.join(2000);
                cmdSocket.close();
                // Pub/Sub channel
                pubSubSocket.shutdownInput();
                listenerThread.interrupt();
                listenerThread.join(2000);
                pubSubSocket.close();

            } catch (InterruptedException interrupt) {
                interrupt.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
    }

    public void getOnlineUsers() {
        senderExec.submit(() -> {
            try {

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
                cmdWriter.print("SEND VNSCP/1.0");
                cmdWriter.print("\r\n");
                cmdWriter.print("Text: ");
                cmdWriter.print(msgContent);
                cmdWriter.print("\r\n");
                cmdWriter.print("\r\n");

                cmdWriter.flush();

                HashMap<String, String> response = parseServerResponse(cmdReader);

                String status = response.get("STATUS");
                switch (status) {
                    case "SENT":
                        break;
                    case "EXPIRED":
                        handleTimeout();
                        break;
                    case "ERROR":
                        handleError(response.get("Reason"));
                        break;
                    default:
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
                handleError(""); // "": Handle it in a very general way.
            }
        });
    }

    private HashMap<String, String> parseServerResponse(BufferedReader reader) throws IOException {
        String responseStatus = reader.readLine();
        if (responseStatus == null) return null;
        responseStatus = responseStatus.split(" ")[1];
        HashMap<String, String> responseHeaders = new HashMap<>();
        String line;
        String[] header;

        responseHeaders.put("STATUS", responseStatus);

        // Parse all headers and store them in the HashMap responseHeaders.
        // HashMap is convenient here because the Client MUST NOT assume that these headers are in any pre-defined order.
        while (!((line = reader.readLine()).isEmpty())) {
            header = line.split(":");
            responseHeaders.put(header[0].trim(), header[1].trim());
        }
        ;
        return responseHeaders;
    }

    // Intended for the ping thread which shall submit a task
    // to ping the server periodically to the send Executor every 15 seconds.
    public void pingServerPeriodically() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                senderExec.submit(this::getOnlineUsers);
                Thread.sleep(60000);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }

    // Let's take a listen 👂
    public void listenForMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                HashMap <String, String> response = null;
                if (!pubSubSocket.isClosed()) {
                    response = parseServerResponse(pubSubReader);
                }
                String status;

                if (response != null && (status = response.get("STATUS")) != null) {
                    switch (status) {
                        case "MESSAGE":

                            Message message = new Message(response.get("Username"), response.get("Text"),
                                    Integer.parseInt(response.get("Id")), response.get("Date"));
                            model.addMessage(message);

                            break;
                        case "EVENT":

                            String desc = response.get("Description");
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
        view.setStatus("An error occurred: " + response, true);
    }

    private void handleTimeout() {System.out.println("Handle timeout");}; //TODO
}