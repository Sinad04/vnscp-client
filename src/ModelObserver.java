
public interface ModelObserver {
    void onMessageAdded(Message newMessage);
    void onUserEvent(String[] userNames);
}
