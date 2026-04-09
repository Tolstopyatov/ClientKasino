package roulette.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import roulette.model.Bet;
import roulette.model.GameState;
import roulette.model.TarakanData;

import java.lang.reflect.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();
    private boolean connected = false;

    // Callbacks
    private Consumer<GameState> onGameStateUpdate;
    private Consumer<Integer> onSpinResult;
    private Consumer<String> onRoundStart;
    private BiConsumer<Boolean, String> onLoginResult;
    private BiConsumer<Boolean, String> onRegisterResult;
    private Consumer<String> onError;
    private Consumer<String> onChatMessage;
    private Consumer<List<TarakanData>> onCockroachRoutes;
    private Consumer<List<TarakanData>> onCockroachPositions;
    private Consumer<Integer> onRaceWinner;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;

        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    processMessage(line);
                }
            } catch (IOException e) {
                if (onError != null) onError.accept("Соединение потеряно: " + e.getMessage());
            } finally {
                disconnect();
            }
        }).start();
    }

    private void processMessage(String json) {
        JsonObject root = gson.fromJson(json, JsonObject.class);
        if (root == null) return;
        String type = root.get("type").getAsString();

        switch (type) {
            case "login_result":
                String loginData = root.get("data").getAsString();
                boolean loginSuccess = "success".equals(loginData);
                if (onLoginResult != null) onLoginResult.accept(loginSuccess, loginData);
                break;

            case "register_result":
                String regData = root.get("data").getAsString();
                boolean regSuccess = "success".equals(regData);
                if (onRegisterResult != null) onRegisterResult.accept(regSuccess, regData);
                break;

            case "update_state":
                GameState state = gson.fromJson(root.get("data"), GameState.class);
                if (onGameStateUpdate != null) onGameStateUpdate.accept(state);
                break;

            case "spin_result":
                int number = root.getAsJsonObject("data").get("number").getAsInt();
                if (onSpinResult != null) onSpinResult.accept(number);
                break;

            case "round_start":
                String roundMsg = root.get("data").getAsString();
                if (onRoundStart != null) onRoundStart.accept(roundMsg);
                break;

            case "chat_message":
                String chatMsg = root.get("data").getAsString();
                if (onChatMessage != null) onChatMessage.accept(chatMsg);
                break;

            case "cockroach_routes":
                if (onCockroachRoutes != null) {
                    Type listType = new TypeToken<List<TarakanData>>(){}.getType();
                    List<TarakanData> routes = gson.fromJson(root.get("data"), listType);
                    onCockroachRoutes.accept(routes);
                }
                break;

            case "cockroach_positions":
                if (onCockroachPositions != null) {
                    Type listType = new TypeToken<List<TarakanData>>(){}.getType();
                    List<TarakanData> positions = gson.fromJson(root.get("data"), listType);
                    onCockroachPositions.accept(positions);
                }
                break;

            case "race_result":
                if (onRaceWinner != null) {
                    JsonObject data = root.getAsJsonObject("data");
                    int winnerId = data.get("winnerId").getAsInt();
                    onRaceWinner.accept(winnerId);
                }
                break;

            case "error":
                String errorMsg = root.get("data").getAsString();
                if (onError != null) onError.accept(errorMsg);
                break;
        }
    }

    public void login(String username, String password) {
        if (!connected) return;
        Map<String, String> creds = new HashMap<>();
        creds.put("username", username);
        creds.put("password", password);
        sendMessage("login", creds);
    }

    public void register(String username, String password) {
        if (!connected) return;
        Map<String, String> creds = new HashMap<>();
        creds.put("username", username);
        creds.put("password", password);
        sendMessage("register", creds);
    }

    public void placeBet(Bet bet) {
        if (!connected) return;
        sendMessage("place_bet", bet);
    }

    public void placeTarakanBet(int tarakanId, int amount) {
        if (!connected) return;
        Map<String, Object> data = new HashMap<>();
        data.put("tarakanId", tarakanId);
        data.put("amount", amount);
        sendMessage("place_tarakan_bet", data);
    }

    public void sendChatMessage(String message) {
        if (!connected) return;
        sendMessage("chat_message", message);
    }

    private void sendMessage(String type, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("data", data);
        out.println(gson.toJson(msg));
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Сеттеры колбэков
    public void setOnGameStateUpdate(Consumer<GameState> cb) { this.onGameStateUpdate = cb; }
    public void setOnSpinResult(Consumer<Integer> cb) { this.onSpinResult = cb; }
    public void setOnRoundStart(Consumer<String> cb) { this.onRoundStart = cb; }
    public void setOnLoginResult(BiConsumer<Boolean, String> cb) { this.onLoginResult = cb; }
    public void setOnRegisterResult(BiConsumer<Boolean, String> cb) { this.onRegisterResult = cb; }
    public void setOnError(Consumer<String> cb) { this.onError = cb; }
    public void setOnChatMessage(Consumer<String> cb) { this.onChatMessage = cb; }
    public void setOnCockroachRoutes(Consumer<List<TarakanData>> cb) { this.onCockroachRoutes = cb; }
    public void setOnCockroachPositions(Consumer<List<TarakanData>> cb) { this.onCockroachPositions = cb; }
    public void setOnRaceWinner(Consumer<Integer> cb) { this.onRaceWinner = cb; }
}