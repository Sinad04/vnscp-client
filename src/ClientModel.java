import java.util.concurrent.CopyOnWriteArrayList;

public class ClientModel {

    private final CopyOnWriteArrayList<Message> messages;
    private final CopyOnWriteArrayList<ModelObserver> observers;

    public ClientModel() {
        this.messages = new CopyOnWriteArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void addMessage(Message msg) {
        messages.add(msg);
        notifyObservers(msg);
    }

    public void addObserver(ModelObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(Message newMessage) {
        for (ModelObserver observer : observers) {
            observer.onMessageAdded(newMessage);
        }
    }
}



