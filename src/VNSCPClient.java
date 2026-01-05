
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// View in the MVC Model.
public class VNSCPClient extends Frame implements ModelObserver {

    private final ClientModel model;
    private final ClientController controller;

    private final Label statusLabel;
    private final Button sendButton;
    private final TextArea messageHistory;
    private final List userList;
    private final TextField messageInput;

    public VNSCPClient(ClientController controller, ClientModel model) {
        super("VNSCP Client");

        this.model = model;
        this.model.addObserver(this);
        this.controller = controller;

        // set up UI
        setLayout(new BorderLayout(8,5));

        sendButton = new Button("Send Message");
        messageHistory = new TextArea(null, 20, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
        messageHistory.setEditable(false);
        messageInput = new TextField();
        statusLabel = new Label("Welcome to VNSCP Chat.");
        userList = new List(20);


        setSize(800, 600);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanUpBeforeExit();
                System.exit(0);
            }
        });

        controller.setView(this);

        try {
            controller.connectCommand("vns.lxd-vs.uni-ulm.de", 8122);
        } catch (IOException ioe) {

        }

        showLoginDialog();
    }

    private void showLoginDialog() {

        Dialog loginDialog = new Dialog(this, "Login", true);
        loginDialog.setLayout(new BorderLayout());
        TextField usernameField = new TextField(30);
        Button confirmButton = new Button("Confirm");
        Label infoLabel = new Label("Enter a username and press Confirm to log in.");
        Panel textPanel = new Panel(new BorderLayout());

        textPanel.add(infoLabel,BorderLayout.CENTER);
        textPanel.add(statusLabel, BorderLayout.NORTH);

        loginDialog.add(usernameField, BorderLayout.CENTER);
        loginDialog.add(confirmButton, BorderLayout.EAST);
        loginDialog.add(textPanel, BorderLayout.NORTH);

        confirmButton.addActionListener(_ -> {
            setStatus("Logging in..", false);
            String username = usernameField.getText();
            if (username.matches("^[a-zA-Z0-9]{3,15}$")) {
                try {
                    if (controller.login(username).get(6, TimeUnit.SECONDS)) {
                        loginDialog.dispose();

                        showChatUI();
                    }
                } catch (TimeoutException timeoute) {
                    this.setStatus("The login attempt has timed out. Please try again.", true);
                } catch (Exception e) {
                    this.setStatus("An unknown error has occurred.", true);
                }
            } else {
                setStatus("Username must be 3-15 characters long and only contain a-z, A-Z, 0-9.", false);
            }
        });

        loginDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        loginDialog.pack();
        loginDialog.setVisible(true);
    }

    private void cleanUpBeforeExit() {
        controller.byeAndShutDown();
    }

    private void showChatUI() {
        setStatus("Welcome to VNSCP Chat.", false);
        Panel inputPanel = new Panel(new BorderLayout());
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(messageHistory, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
        add(userList, BorderLayout.EAST);

        sendButton.addActionListener(_ -> {
            String message = messageInput.getText();

            controller.sendMessage(message);
            // I'm honestly unsure if the client is required to validate messages, since the server will discard invalid messages with the appropriate reason anyway.
            // But this is how it would be done if so.
            // if (message.isEmpty()) {
            //     setStatus("An Error has occurred: Can't send empty messages!", true);
            // } else if (message.getBytes().length > 512) {
            //     setStatus("An Error has occurred: Message too long to send!", true);
            // }
            messageInput.setText("");
        });

        setVisible(true);
    }

    public void setStatus(String text, boolean urgent) {
        EventQueue.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setBackground(urgent ? Color.PINK : null);
        });
    }

    @Override
    public void onMessageAdded(Message newMsg) {
        EventQueue.invokeLater(() -> {
           String formatted = String.format("%s: %s\n", newMsg.sender(), newMsg.content());
           messageHistory.append(formatted);
        });
    }

    @Override
    public void onUserEvent(String[] users) {
        EventQueue.invokeLater(() -> {
            userList.removeAll();
            for (String user : users) {
                userList.add(user);
            }
        });
    }

    public static void main() {

        ClientController controller = new ClientController();
        ClientModel model = new ClientModel();
        controller.setModel(model);
        new VNSCPClient(controller, model);

    }
}