
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

// View in the MVC Model.
public class VNSCPClient extends Frame implements ModelObserver {

    private final ClientModel model;
    private final ClientController controller;

    private Label statusLabel;
    private Button sendButton;
    private TextArea messageHistory;
    private TextField messageInput;

    public VNSCPClient(ClientController controller, ClientModel model) {
        super("VNSCP Client");

        this.model = model;
        this.model.addObserver(this);
        this.controller = controller;

        // set up UI
        setLayout(new BorderLayout(8,5));

        sendButton = new Button("Send Message");
        messageHistory = new TextArea();
        messageHistory.setEditable(false);
        messageInput = new TextField();
        statusLabel = new Label("Welcome to VNSCP Chat.");

        setSize(800, 600);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // cleanUpBeforeExit(); TODO
                System.exit(0);
            }
        });

        showLoginDialog();
    }

    private void showLoginDialog() {
        Dialog loginDialog = new Dialog(this, "Login", true);

        loginDialog.setLayout(new BorderLayout());

        TextField usernameField = new TextField(30);
        Button confirmButton = new Button("Confirm");

        loginDialog.add(usernameField, BorderLayout.CENTER);
        loginDialog.add(confirmButton, BorderLayout.EAST);
        System.out.println("Login dialog made");
        confirmButton.addActionListener(_ -> {
            loginDialog.dispose();
            String username = usernameField.getText();
            // TODO validate input
            controller.login(username);
            showChatUI();
        });

        System.out.println("before show");
        loginDialog.pack();
        loginDialog.setVisible(true);
        System.out.println("after show");
    }


    private void showChatUI() {

        Panel inputPanel = new Panel(new BorderLayout());
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(messageHistory, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        sendButton.addActionListener(_ -> {
            controller.sendMessage(messageInput.getText());
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
           System.out.println("onMessageAdded: " + formatted);
           messageHistory.append(formatted);
        });
    }

    public static void main() {

        ClientModel model = new ClientModel();
        ClientController controller = new ClientController();
        controller.setModel(model);
        try {
            controller.connectCommand("vns.lxd-vs.uni-ulm.de", 8122);
            controller.connectPubSub("vns.lxd-vs.uni-ulm.de", 8123);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        VNSCPClient client = new VNSCPClient(controller, model);
        controller.setView(client);


    }
}