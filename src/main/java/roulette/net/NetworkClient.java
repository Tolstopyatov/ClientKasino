package roulette.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import roulette.model.Bet;
import roulette.model.GameState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
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
                e.printStackTrace();
                if (onError != null) onError.accept("Connection lost");
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
        Message msg = new Message("login", creds);
        out.println(gson.toJson(msg));
    }

    public void register(String username, String password) {
        if (!connected) return;
        Map<String, String> creds = new HashMap<>();
        creds.put("username", username);
        creds.put("password", password);
        Message msg = new Message("register", creds);
        out.println(gson.toJson(msg));
    }

    public void placeBet(Bet bet) {
        if (!connected) return;
        Message msg = new Message("place_bet", bet);
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

    // Setters for callbacks
    public void setOnGameStateUpdate(Consumer<GameState> callback) { this.onGameStateUpdate = callback; }
    public void setOnSpinResult(Consumer<Integer> callback) { this.onSpinResult = callback; }
    public void setOnRoundStart(Consumer<String> callback) { this.onRoundStart = callback; }
    public void setOnLoginResult(BiConsumer<Boolean, String> callback) { this.onLoginResult = callback; }
    public void setOnRegisterResult(BiConsumer<Boolean, String> callback) { this.onRegisterResult = callback; }
    public void setOnError(Consumer<String> callback) { this.onError = callback; }

    // Вспомогательный класс для отправки сообщений
    private static class Message {
        String type;
        Object data;

        Message(String type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
}
