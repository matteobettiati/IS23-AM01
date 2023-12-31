package Client.View.Cli;

import Client.ClientApp;
import Client.Network.Network;
import Client.Network.NetworkFactory;
import Client.View.View;
import Enumeration.ClientCommand;
import Enumeration.GameWarning;
import Enumeration.TurnPhase;
import Utils.Cell;
import Utils.ChatMessage;
import Utils.MockObjects.MockBoard;
import Utils.MockObjects.MockCommonGoal;
import Utils.MockObjects.MockModel;
import Utils.MockObjects.MockPlayer;
import Utils.Rank;
import Enumeration.CliColor;
import Utils.Tile;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static Client.ClientApp.*;

/**
 * This class represents a command-line interface (CLI) for the application.
 * It extends the View class and provides methods for interacting with the user through the command line.
 * The CLI communicates with the LightController and displays the game board, chat messages, and player updates.
 */
@SuppressWarnings("BlockingMethodInNonBlockingContext")
public class Cli extends View {
    private LightController controller;
    private List<Tile> selectedTiles;
    private final Scanner scanner = new Scanner(System.in);
    private static final String TAB = "   ";

    /**
     * Constructs a new Cli object.
     * Initializes the mockModel, displays the title, and starts the CLI.
     *
     * @throws RemoteException if a remote exception occurs
     */
    public Cli() throws RemoteException {
        super();
        mockModel = new MockModel();
        showTitle();
        start();
    }

    /**
     * Starts the CLI by initializing the network connection and asking for the server address and port.
     * It runs the network initialization in a separate thread.
     */
    private void start() {
        try {
            network = askConnection();
        } catch (RemoteException e) {
            printError("ERROR: " + e.getMessage());
            System.exit(-1);
        }
        askServerAddress();
        askServerPort();
        Thread connection = new Thread(() -> network.init());
        connection.start();
    }

    /**
     * Asks the user to select a connection protocol (SOCKET or RMI).
     *
     * @return the Network object corresponding to the selected connection protocol
     * @throws RemoteException if a remote exception occurs
     */
    public Network askConnection() throws RemoteException {
        String input;
        System.out.print(CliColor.BOLD + "To start select a connection protocol between 'SOCKET' or 'RMI': " + CliColor.RESET);

        input = scanner.nextLine();

        while (!input.equalsIgnoreCase("SOCKET") && !input.equalsIgnoreCase("RMI")) {
            printError("ERROR: you type something wrong, please enter 'SOCKET' or 'RMI'");
            input = scanner.nextLine();
        }

        printMessage("Good! You are going to create a " + input.toLowerCase() + " connection.");

        return NetworkFactory.instanceNetwork(input);
    }

    /**
     * Asks the user to enter the server address and validates it.
     */
    private void askServerAddress() {
        System.out.print(CliColor.BOLD + "Please enter the server address. " + CliColor.RESET);
        do {
            System.out.print(CliColor.BOLD + "\nInsert 'default' for the default value (" + IP_SERVER + ") or 'localhost': " + CliColor.RESET);
            String address = scanner.nextLine();

            switch (address) {
                case "l", "localhost" -> {
                    IP_SERVER = "localhost";
                    return;
                }
                case "d", "default" -> {
                    return;
                }
                default -> {
                    if (RenderHelper.validateIP(address)) {
                        IP_SERVER = address;
                        return;
                    } else {
                        printError("ERROR: Invalid address! (remember the syntax xxx.xxx.xxx.xxx)");
                        System.out.println(" Try again.");
                    }
                }
            }
        } while (true);
    }

    /**
     * Asks the user to enter the server address and validates it.
     */
    public void askServerPort() {
        final int MIN_PORT = 1024;
        final int MAX_PORT = 65535;

        while (true) {
            System.out.print(CliColor.BOLD + "Select a valid port between [" + MIN_PORT + ", " + MAX_PORT + "]. ");
            System.out.print("\nInsert 'default' for the default value [for SOCKET (" + SOCKET_PORT + "); for RMI (" + RMI_PORT + ")]: " + CliColor.RESET);

            String input = scanner.nextLine();

            switch(input){
                case "d", "default" -> {
                    return;
                }
                default -> {
                    try {
                        int port = Integer.parseInt(input);
                        if (MIN_PORT <= port && port <= MAX_PORT) {
                            SOCKET_PORT = port;
                            RMI_PORT = port;
                            return;
                        } else {
                            printError("ERROR: MIN PORT = " + MIN_PORT + ", MAX PORT = " + MAX_PORT + ".");
                            System.out.println(" Try again.");
                        }
                    } catch (NumberFormatException e) {
                        printError("ERROR: Please insert only numbers or 'default'.");
                        System.out.println("Try again.");
                    }
                }
            }
        }
    }

    /**
     * Asks the player to enter the lobby ID and their nickname to join the lobby.
     * If the lobby ID is not found, a new lobby will be instantiated.
     *
     * @param lobbyInfo the list of lobby information containing lobby and game details
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public void askPlayerInfo(List<Map<String, String>> lobbyInfo) throws RemoteException {
        String inputLobby;
        String inputName;

        if (lobbyInfo != null) {
            System.out.println("Here you can find the lobbies or games with the players logged. Write an ID for the lobby/game; if it doesn't match with others, a new lobby will be instantiated.");
            for (String object : lobbyInfo.get(0).keySet())
                System.out.println("LobbyID: " + object + TAB + "Waiting Room: " + lobbyInfo.get(0).get(object));
            for (String object : lobbyInfo.get(1).keySet())
                System.out.println("GameID: " + object + TAB + "Players Online: " + lobbyInfo.get(1).get(object));
        } else System.out.println("There are no lobby or games: create a new one");


        while (true) {
            System.out.print(CliColor.BOLD + "\nInsert a lobby ID: " + CliColor.RESET);
            String input = scanner.nextLine();
            if (!input.isBlank()) {
                inputLobby = input;
                break;
            } else printError("ERROR: you type something wrong, lobby can't be empty");
        }

        while (true) {
            System.out.print(CliColor.BOLD + "Insert your Nickname: " + CliColor.RESET);
            String input = scanner.nextLine();
            if (!input.isBlank()) {
                inputName = input;
                break;
            } else printError("ERROR: you type something wrong, nickname can't be empty");
        }

        network.login(inputName, inputLobby, this, network);
    }

    /**
     * Asks the player to enter the number of players for the lobby size.
     * The player must enter a number between 2 and 4 (inclusive).
     *
     * @throws RemoteException if a remote communication error occurs
     */
    @Override
    public void askLobbySize() throws RemoteException {
        int playerNumber;
        String input;

        while (true) {
            try {
                System.out.print(CliColor.BOLD + "Please insert the numbers of players (insert a number between 2 and 4): " + CliColor.RESET);
                input = scanner.nextLine();
                playerNumber = Integer.parseInt(input);
                if (playerNumber >= 2 && playerNumber <= 4) {
                    break;
                }
                printError("ERROR: the game can start only with 2, 3 or 4 players.");
            } catch (NumberFormatException exception) {
                printError("ERROR: don't insert letter, only number");
            }
        }

        printMessage("You are going to create a new Game, wait for the others players");

        network.setLobbySize(localPlayer, lobbyID, playerNumber);
    }

    /**
     * Displays the game board using the MockBoard object.
     */
    public void showBoard() {
        Cell[][] board = mockModel.getMockBoard().getBoard();
        int numberPlayer = mockModel.getMockPlayers().size();
        MockCommonGoal commonGoal1 = mockModel.getMockCommonGoal().get(0);
        MockCommonGoal commonGoal2 = mockModel.getMockCommonGoal().get(1);

        String[] parole1 = commonGoal1.getDescription().split(" ");
        String[] parole2 = commonGoal2.getDescription().split(" ");

        List<String> subString1 = new LinkedList<>(RenderHelper.subString(parole1));
        while (subString1.size() <= 3) {
            subString1.add(null);
        }

        List<String> subString2 = new LinkedList<>();
        while (subString2.size() <= 3) {
            subString2.add(null);
        }
        subString2.addAll(RenderHelper.subString(parole2));
        while (subString2.size() <= 8) {
            subString2.add(null);
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TAB + " ");
        if (numberPlayer == 2) for (int i = 0; i <= 6; i++)
            stringBuilder.append("  ").append(i).append("  ");
        else for (int i = 0; i <= 8; i++)
            stringBuilder.append("  ").append(i).append("  ");
        System.out.print(stringBuilder.append(TAB + "| "));


        System.out.println(CliColor.BOLD + "COMMON GOAL" + CliColor.RESET);

        for (int i = 0; i < board.length; i++) {
            System.out.print(i + "   ");
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j].getStatus() && board[i][j].getTile() != null) {
                    String colorString = board[i][j].getTile().color().getCode();
                    System.out.print(CliColor.BBLACK + "|" + colorString + i + "," + j + CliColor.BBLACK + "|" + CliColor.RESET);
                } else {
                    System.out.print(CliColor.BBLACK + "|" + TAB + "|" + CliColor.RESET); //print empty black space
                }
            }
            System.out.print("   | ");

            //print CommonGoal
            if (i <= 2) {
                if (i == 0) {
                    System.out.print("[" + CliColor.BRED + " " + (!(commonGoal1.getScoringToken().isEmpty()) ? commonGoal1.getScoringToken().get(commonGoal1.getScoringToken().size() - 1) : 0) + " " + CliColor.RESET + "] - ");
                }
                if (subString1.get(i) != null) System.out.print(subString1.get(i));
                else System.out.print("");
            }
            if (i >= 4 && i <= 6) {
                if (i == 4) {
                    System.out.print("[" + CliColor.BRED + " " + (!(commonGoal2.getScoringToken().isEmpty()) ? commonGoal2.getScoringToken().get(commonGoal2.getScoringToken().size() - 1) : 0) + " " + CliColor.RESET + "] - ");
                }

                if (subString2.get(i) != null) System.out.print(subString2.get(i));
                else System.out.print("");
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Displays the shelves on the CLI.
     */
    private void showShelves() {
        int numColumn = 5;
        int numRow = 6;
        int numPlayer = mockModel.getMockPlayers().size();

        System.out.print("    ");
        for (int k = 0; k < numPlayer; k++) {
            System.out.print("  A  " + "  B  " + "  C  " + "  D  " + "  E  " + TAB + TAB);
        }
        System.out.println();

        for (int i = 0; i < numRow; i++) {
            System.out.print(TAB + " ");
            for (int k = 0; k < numPlayer; k++) {
                for (int j = 0; j < numColumn; j++) {
                    Tile[][] shelf = mockModel.getMockPlayers().get(k).getShelf();
                    Tile[][] privateGoal = mockModel.getMockPlayers().get(k).getPersonalGoal();
                    String colorString = (shelf[i][j] != null) ? shelf[i][j].color().getCode() : CliColor.BBLACK.toString();
                    String colorBar;

                    if (localPlayer.equals(mockModel.getMockPlayers().get(k).getPlayerID())) {
                        colorBar = (privateGoal[i][j] != null) ? privateGoal[i][j].color().getCode() : CliColor.BBLACK.toString();
                    } else {
                        colorBar = CliColor.BBLACK.toString();
                    }
                    System.out.print(colorBar + "|" + colorString + TAB + colorBar + "|" + CliColor.RESET);
                }
                System.out.print(TAB + TAB);
            }
            System.out.println();
        }
        System.out.print(TAB + " ");

        for (MockPlayer player : this.mockModel.getMockPlayers()) {
            if (player.isOnline()) {
                System.out.print(CliColor.BOLD + player.getPlayerID() + ": " + player.getScore() + CliColor.RESET);
                for (int i = 0; i < 31 - player.getPlayerID().length() - RenderHelper.countDigit(player.getScore()); i++)
                    System.out.print(" ");
            } else {
                System.out.print(CliColor.BOLD + player.getPlayerID() + ": " + CliColor.RED + "OFFLINE" + CliColor.RESET);
                for (int i = 0; i < 31 - player.getPlayerID().length() - " OFFLINE ".length(); i++)
                    System.out.print(" ");
            }
        }
        System.out.println("\n");
    }

    /**
     * Displays the available commands and their descriptions.
     */
    public void showHelp() {
        System.out.println(CliColor.BOLD + "Commands:" + CliColor.RESET);
        for (ClientCommand command : ClientCommand.values()) {
            System.out.println(command);
        }
    }

    /**
     * Displays the all messages in chat.
     */
    public void showChat() {
        clearCLI();
        for (ChatMessage message : mockModel.getChat()) {
            System.out.println(message);
        }
    }

    /**
     * Displays the status of the game.
     */
    public void showStatus() {
        if (mockModel.getCurrentPlayer().equals(localPlayer)) {
            System.out.println(CliColor.BOLD + "It's your turn. " + mockModel.getTurnPhase() + " For more help type 'help'" + CliColor.RESET);
        } else {
            System.out.println(CliColor.BOLD + "It's NOT your turn. Wait for others player. For help type 'help'" + CliColor.RESET);
        }
    }

    /**
     * Displays the title of the game.
     */
    public void showTitle() {
        System.out.print(CliColor.BOLDYELLOW);
        System.out.println("""
                 ✹ ｡  .  ･ . ∴ * ███╗   ███╗██╗   ██╗    ██████╗██╗  ██╗███████╗██╗     ███████╗██╗███████╗. 　･ ∴　　｡ 　
                ｡    ✦    *      ████╗ ████║╚██╗ ██╔╝   ██╔════╝██║  ██║██╔════╝██║     ██╔════╝██║██╔════╝ ∴⋆  ˚  *   .
                  ∴   *  ｡ .  ✹  ██╔████╔██║ ╚████╔╝    ╚█████╗ ███████║█████╗  ██║     █████╗  ██║█████╗   ｡ ·　 ✦   *
                 .   ･  *   ｡  ∴ ██║╚██╔╝██║  ╚██╔╝      ╚═══██╗██╔══██║██╔══╝  ██║     ██╔══╝  ██║██╔══╝　   ✹  ｡   ·  ✧
                  ･  .   ✦     * ██║ ╚═╝ ██║   ██║      ██████╔╝██║  ██║███████╗███████╗██║     ██║███████╗ ✦ ∴ 　･ ｡· ∴
                  ✹   ｡ ∴.  ･   .╚═╝     ╚═╝   ╚═╝      ╚═════╝ ╚═╝  ╚═╝╚══════╝╚══════╝╚═╝     ╚═╝╚══════╝ ･　 *　　✹　 ˚""" + CliColor.RESET);

        System.out.println("Complete rules are available here: " + CliColor.BOLDPINK + "https://www.craniocreations.it/prodotto/my-shelfie\n" + CliColor.RESET);
    }


    /**
     * Displays the tiles on the CLI.
     *
     * @param tiles The list of tiles selected to display.
     */
    private void showTile(List<Tile> tiles) {
        System.out.print(TAB);
        for (int i = 0; i < tiles.size(); i++) {
            System.out.print(tiles.get(i).color().getCode() + "|" + (i + 1) + "|");
            System.out.print(CliColor.RESET + TAB);
        }
        System.out.println();
    }

    /**
     * Displays the game board, shelves, and status on the CLI.
     */
    public void showAll() {
        clearCLI();
        showBoard();
        showShelves();
        if (this.mockModel.getTurnPhase() == TurnPhase.INSERTING) {
            showTile(this.selectedTiles);
        }
        showStatus();
    }

    @Override
    public synchronized void endGame(List<Rank> classification) {
        System.out.println(CliColor.BOLD + "Final leaderboard:" + CliColor.RESET);
        Rank first = classification.get(0);
        for (Rank rank : classification) {
            if (rank.getTotalScore() == first.getTotalScore()) {
                printMessage(rank.toString());
            } else System.out.println(rank);
        }
        quit(0);
    }

    @Override
    public synchronized void crashedPlayer(String crashedPlayer) throws RemoteException {
        this.mockModel.getPlayer(crashedPlayer).setOnline(false);
    }

    @Override
    public synchronized void reloadPlayer(String reloadPlayer) throws RemoteException {
        this.mockModel.getPlayer(reloadPlayer).setOnline(true);
    }

    @Override
    public synchronized void outcomeMessage(GameWarning message) throws RemoteException {
        printMessage(message.getMs());
        if(message == GameWarning.WON){
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
        }
    }

    @Override
    public synchronized void newTurn(String playerID) throws RemoteException {
        clearCLI();
        mockModel.setCurrentPlayer(playerID);
        mockModel.setTurnPhase(TurnPhase.PICKING);
        showAll();
    }

    @Override
    public synchronized void outcomeSelectTiles(List<Tile> tiles) throws RemoteException {
        this.mockModel.setTurnPhase(TurnPhase.INSERTING);
        this.selectedTiles = tiles;
        showTile(tiles);
        showStatus();
    }

    @Override
    public synchronized void outcomeInsertTiles(boolean success) throws RemoteException {
        if (success) {
            this.mockModel.setTurnPhase(TurnPhase.PICKING);
        } else printError("Insertion failed");
    }


    @Override
    public synchronized void outcomeException(Exception e) throws RemoteException {
        printError(e.getMessage());
    }

    @Override
    public void outcomeLogin(String localPlayer, String lobbyID) throws RemoteException {
        System.out.println("You logged into the lobby");
        ClientApp.localPlayer = localPlayer;
        ClientApp.lobbyID = lobbyID;
        network.startPing(localPlayer, lobbyID);
    }

    @Override
    public void allGame(MockModel mockModel) throws RemoteException {
        this.mockModel = mockModel;
        this.controller = new LightController();
        if (mockModel.getChat() != null) fixChat();
        setLocalFirst();
        newTurn(mockModel.getCurrentPlayer());
        while (true) {
            try {
                if (!(System.in.available() > 0)) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Thread inputThread = new Thread(() -> {
            while (true) {
                String input = this.scanner.nextLine();
                if (input != null && !input.isEmpty()) {
                    controller.elaborate(input);
                }
            }
        });
        inputThread.start();
    }

    @Override
    public synchronized void updateBoard(MockBoard mockBoard) {
        this.mockModel.setMockBoard(mockBoard);
    }

    @Override
    public synchronized void updateCommonGoal(MockCommonGoal mockCommonGoal) {
        this.mockModel.update(mockCommonGoal);
    }

    @Override
    public synchronized void updatePlayer(MockPlayer mockPlayer) {
        this.mockModel.update(mockPlayer);
    }

    @Override
    public synchronized void updateChat(ChatMessage message) {
        if (message.to() == null || message.to().equals(localPlayer)) {
            this.mockModel.addMessage(message);
            System.out.println(CliColor.BOLD + "\rNew Message" + CliColor.RESET);
            return;
        }
        if (message.from().equals(localPlayer)) {
            printMessage("Message sent correctly");
            this.mockModel.addMessage(message);
        }
    }

    /**
     * Prints an error message on the CLI.
     *
     * @param error The error message to print.
     */
    public void printError(String error) {
        System.out.println(CliColor.BOLDRED + error + CliColor.RESET);
    }

    /**
     * Prints a regular message on the CLI.
     *
     * @param message The message to print.
     */
    public void printMessage(String message) {
        System.out.println(CliColor.BOLDGREEN + message + CliColor.RESET);
    }

    /**
     * Moves the local player to the first position in the player order.
     */
    private void setLocalFirst() {
        for (MockPlayer player : mockModel.getMockPlayers()) {
            if (player.getPlayerID().equals(localPlayer)) {
                mockModel.getMockPlayers().remove(player);
                mockModel.getMockPlayers().add(0, player);
                break;
            }
        }
    }

    /**
     * Fixes the chat messages in the MockModel to only include relevant messages.
     */
    private void fixChat() {
        mockModel.getChat().removeIf(message -> message.to() != null && !message.to().equals(localPlayer) && !message.from().equals(localPlayer));
    }

    /**
     * Clears the CLI by removing all displayed content.
     */
    private void clearCLI() {
        System.out.print(CliColor.CLEAR_ALL);
        System.out.flush();
    }

    /**
     * This class provides helper methods for rendering and validation operations.
     */
    static class RenderHelper {

        /**
         * Splits an array of words into substrings of a maximum length.
         *
         * @param words The array of words to split.
         * @return A list of substrings.
         */
        private static List<String> subString(String[] words) {
            List<String> subString = new LinkedList<>();
            StringBuilder sb1 = new StringBuilder();
            int maxLength = 85;

            for (String word : words) {
                if (word.length() > maxLength) {
                    if (sb1.length() > 0) {
                        subString.add(sb1.toString().trim());
                        sb1.setLength(0);
                    }
                    subString.add(word);
                } else if (sb1.length() + word.length() <= maxLength) {
                    sb1.append(word).append(" ");
                    if (sb1.length() > maxLength) {
                        subString.add(sb1.toString().trim());
                        sb1.setLength(0);
                    }
                } else {
                    subString.add(sb1.toString().trim());
                    sb1.setLength(0);
                    sb1.append(word).append(" ");
                }
            }
            if (sb1.length() > 0) {
                subString.add(sb1.toString().trim());
            }
            return subString;
        }

        /**
         * States whether the given address is valid or not.
         *
         * @param address the inserted IP address.
         * @return a boolean whose value is:
         * -{@code true} if the address is valid;
         * -{@code false} otherwise.
         */
        private static boolean validateIP(String address) {
            String zeroTo255 = "([01]?\\d{1,2}|2[0-4]\\d|25[0-5])";
            String IP_REGEX = "^(" + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + ")$";
            return address.matches(IP_REGEX);
        }

        /**
         * Counts the number of digits in a given number.
         *
         * @param number The number to count the digits of.
         * @return The count of digits in the number.
         */
        private static int countDigit(int number) {
            int count = 2;
            if (number == 0) {
                return 3;
            }
            while (number != 0) {
                number = number / 10;
                count++;
            }
            return count;
        }
    }
}
