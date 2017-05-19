package com.client.profile;

import com.client.login.MainLauncher;
import com.messages.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by rask on 10.05.2017.
 */
public class ProfileController implements Initializable{
    @FXML Label userMainLabel;
    @FXML Label emailMainLabel;
    @FXML ImageView Defaultview;
    public User user;

    public void closeSystem(ActionEvent actionEvent) {
        ((Node)(actionEvent.getSource())).getScene().getWindow().hide();
    }

    public void minimizeWindow(ActionEvent actionEvent) {
        MainLauncher.getPrimaryStage().setIconified(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void initData(){
        userMainLabel.setText(this.user.getName());
        emailMainLabel.setText(this.user.getEmail());
        System.out.println(this.user.getPicture());
        this.Defaultview.setImage(new Image(getClass().getClassLoader().getResource("images/" + this.user.getPicture() + ".png").toString()));
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
