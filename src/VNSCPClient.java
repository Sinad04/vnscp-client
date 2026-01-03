import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

// View in the MVC Model.
public class VNSCPClient extends Frame {

    // private final ChatModel model;
    private final ClientController chatController;

    private Button sendButton;
    private TextArea messageHistory;
    private TextField messageInput;

    public VNSCPClient(ClientController controller) {
        super("VNSCP Client");

        // set up UI
        // this.model = model;
        // this.model.addObserver(this);

        this.chatController = controller;

        setLayout(new BorderLayout(8,5));

        sendButton = new Button("Send Message");
        messageHistory = new TextArea();
        messageHistory.setEditable(false);
        messageInput = new TextField();


        Panel inputPanel = new Panel(new BorderLayout());
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

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

    public static void main() {
        ClientController controller = new ClientController();
        VNSCPClient client = new VNSCPClient(controller);
        controller.setView(client);
        try {
            controller.connectCommand("vns.lxd-vs.uni-ulm.de", 8122);
            controller.login("test" + String.valueOf(((int)(Math.random()*100)))); //TODO custom username
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}