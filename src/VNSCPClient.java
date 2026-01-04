import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

// View in the MVC Model.
public class VNSCPClient extends Frame implements ModelObserver {

    private final ClientModel model;
    private final ClientController chatController;

    private Button sendButton;
    private TextArea messageHistory;
    private TextField messageInput;

    private Thread listenerThread;

    public VNSCPClient(ClientController controller, ClientModel model) {
        super("VNSCP Client");

        this.model = model;
        this.model.addObserver(this);

        this.chatController = controller;

        // set up UI
        setLayout(new BorderLayout(8,5));

        sendButton = new Button("Send Message");
        messageHistory = new TextArea();
        messageHistory.setEditable(false);
        messageInput = new TextField();


        Panel inputPanel = new Panel(new BorderLayout());
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(messageHistory, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(_ -> {
            chatController.sendMessage(messageInput.getText());
            messageInput.setText("");
        });

        setSize(800, 600);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // cleanUpBeforeExit(); TODO
                System.exit(0);
            }
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
        ClientController controller = new ClientController();
        ClientModel model = new ClientModel();
        VNSCPClient client = new VNSCPClient(controller, model);
        controller.setView(client);
        controller.setModel(model);
        try {
            controller.connectCommand("vns.lxd-vs.uni-ulm.de", 8122);
            controller.connectPubSub("vns.lxd-vs.uni-ulm.de", 8123);
            controller.login("test" + String.valueOf(((int)(Math.random()*100)))); //TODO custom username
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}