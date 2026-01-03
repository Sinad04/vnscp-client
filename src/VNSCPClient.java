import java.awt.*;
import java.awt.event.*;

// View in the MVC Model.
public class VNSCPClient extends Frame {

    // private final ChatModel model;

    private Button sendButton;
    private TextArea messageHistory;
    private TextField messageInput;

    public VNSCPClient() {
        super("VNSCP Client");

        // set up UI
        // this.model = model;
        // this.model.addObserver(this);

        setLayout(new BorderLayout(8,5));

        sendButton = new Button("Send Message");
        messageHistory = new TextArea();
        messageHistory.setEditable(false);
        messageInput = new TextField();


        Panel inputPanel = new Panel(new BorderLayout());
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);

        // sendButton.addActionListener(_ -> sendMessage());

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
        VNSCPClient client = new VNSCPClient();
    }
}