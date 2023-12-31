package Client.View.Gui.Scene;

import Client.Network.NetworkFactory;
import Client.View.Gui.GuiApplication;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static Client.ClientApp.*;

/**
 * The `ConnectionScene` class represents the scene for establishing a connection to the game server.
 * It extends the JavaFX `Scene`
 * class and provides a UI for the user to choose the type of connection
 * (RMI or SOCKET) and enter the server IP and port.
 */

public class ConnectionScene extends Scene {

    private static GuiApplication app;
    private final TextField ipField;
    private final TextField portField;

    /**
     * Class constructor.
     */
    public ConnectionScene(GuiApplication app) {

        super(new Pane(), 960, 750);

        setUserAgentStylesheet(STYLEPATH);

        ConnectionScene.app = app;

        Label label = new Label("Choose the type of connection: ");
        label.getStyleClass().add("label-title");

        ipField = new TextField();
        ipField.setPromptText("IP: (default " + IP_SERVER + ") ");
        ipField.getStyleClass().add("text-field");
        ipField.setMaxWidth(400);

        portField = new TextField();
        portField.setPromptText("PORT: default SOCKET(" + SOCKET_PORT + ") RMI(" + RMI_PORT + ")");
        portField.getStyleClass().add("text-field");
        portField.setMaxWidth(400);

        Button rmiButton = new Button("RMI");

        Button socketButton = new Button("SOCKET");

        rmiButton.setOnAction(e -> handleRMIClick());
        socketButton.setOnAction(e -> handleSocketClick());

        VBox backgroundBox = new VBox();
        backgroundBox.setSpacing(40);
        backgroundBox.setAlignment(Pos.CENTER);

        HBox hBoxButtons = new HBox(40, rmiButton, socketButton);
        hBoxButtons.setAlignment(Pos.CENTER);

        backgroundBox.getChildren().addAll(label, ipField, portField, hBoxButtons);

        setRoot(backgroundBox);
    }

    private void handleRMIClick() {
        System.out.println("Enabling RMI connection...");
        if (!checkIp()) return;
        if (!checkPort()) return;
        try {
            network = NetworkFactory.instanceNetwork("RMI");
        } catch (RemoteException e) {
            app.printError("ERROR: " + e.getMessage());
            System.exit(-1);
        }
        //network.init(ipField.getText(), Integer.parseInt(portField.getText()));
        Thread connection = new Thread(() -> network.init());
        connection.start();
    }

    private void handleSocketClick() {
        System.out.println("Enabling SOCKET connection...");
        if (!checkIp()) return;
        if (!checkPort()) return;
        try {
            network = NetworkFactory.instanceNetwork("SOCKET");
        } catch (RemoteException e) {
            app.printError("ERROR: " + e.getMessage());
            System.exit(-1);
        }
        //network.init(ipField.getText(), Integer.parseInt(portField.getText()));
        Thread connection = new Thread(() -> network.init());
        connection.start();
    }

    /**
     * Calls switchScene and set as the current scene the LoginScene.
     */
    public static void toLoginScene(List<Map<String, String>> lobbyInfo) {
        Scene loginScene = new LoginScene(app, lobbyInfo);
        app.switchScene(loginScene);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkPort() {
        final int MIN_PORT = 1024;
        final int MAX_PORT = 65535;

        switch (portField.getText()) {
            case "d", "default" -> {
                return true;
            }
            default -> {
                try {
                    int port = Integer.parseInt(portField.getText());
                    if (MIN_PORT <= port && port <= MAX_PORT) {
                        SOCKET_PORT = port;
                        RMI_PORT = port;
                        return true;
                    } else {
                        app.printError("ERROR: MIN PORT = " + MIN_PORT + ", MAX PORT = " + MAX_PORT + ".");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    app.printError("ERROR: Please insert only numbers or 'default'.");
                    return false;
                }
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkIp() {

        switch (ipField.getText()) {
            case "d", "default" -> {
                return true;
            }
            case "l", "localhost" -> {
                IP_SERVER = "127.0.0.1";
                return true;
            }
            default -> {
                String zeroTo255 = "([01]?\\d{1,2}|2[0-4]\\d|25[0-5])";
                String IP_REGEX = "^(" + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + ")$";
                if (!ipField.getText().matches(IP_REGEX)) app.printError("Invalid IP address");
                else {
                    IP_SERVER = ipField.getText();
                    return true;
                }
            }
        }
        return false;
    }

}
