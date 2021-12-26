package network.misq.grpc;

// TODO Move to lower level module for interfaces used by all
public interface ErrorMessageHandler {
    void handleErrorMessage(String errorMessage);
}
