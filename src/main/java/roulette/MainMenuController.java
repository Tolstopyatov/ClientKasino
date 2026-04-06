package roulette;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import roulette.net.NetworkClient;

public class MainMenuController {
    private NetworkClient networkClient;
    private String username;

    public void initData(NetworkClient client, String name) {
        this.networkClient = client;
        this.username = name;
    }

    @FXML
    private void openRoulette(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/game_table.fxml"));
        Parent root = loader.load();
        RouletteController controller = loader.getController();
        controller.initAfterLogin(networkClient, username);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 950, 750));
        stage.setTitle("Рулетка - " + username);
    }
    @FXML
    private void openCockroachRace(ActionEvent event) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tarakany.fxml"));
        Parent root = loader.load();
        Tarakan controller = loader.getController();
        controller.initAfterLogin(networkClient, username);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 950, 700));
        stage.setTitle("Тараканьи бега - " + username);
    }
    @FXML
    private void logout(ActionEvent event) {
        networkClient.disconnect();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}