package com.server;

/**
 * Created by rask on 05.05.2017.
 */

import com.exception.DuplicateUsernameException;
import com.messages.*;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Server {

    /* Setting up variables */
    private static final int PORT = 9001;
    private static final HashMap<String, User> names = new HashMap<>();
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();
    private static ArrayList<User> users = new ArrayList<>();
    static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws Exception {
        logger.info("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);

        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listener.close();
        }
    }


    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private Logger logger = LoggerFactory.getLogger(Handler.class);
        private User user;

        public Handler(Socket socket) throws IOException {
            this.socket = socket;
        }

        public void run() {
            logger.info("Attempting to connect a user...");
            try (
                InputStream is = socket.getInputStream();
                ObjectInputStream input = new ObjectInputStream(is);
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(os)
            ){

                Message firstMessage = (Message) input.readObject();
                if(firstMessage.getType().equals(MessageType.REGISTER))
                    registerUser(firstMessage);
                else {
                    checkDuplicateUsername(firstMessage);
                    writers.add(output);
                    sendNotification(firstMessage);
                    addToList();

                    while (socket.isConnected()) {
                        Message inputmsg = (Message) input.readObject();
                        if (inputmsg != null) {
                            logger.info(inputmsg.getName() + " has " + names.size());
                            switch (inputmsg.getType()) {
                                case USER:
                                    write(inputmsg);
                                    break;
                                case VOICE:
                                    write(inputmsg);
                                    break;
                                case CONNECTED:
                                    addToList();
                                    break;
                                case STATUS:
                                    changeStatus(inputmsg);
                                    break;
                                case REGISTER:
                                    registerUser(inputmsg);
                                    break;
                                case ROOM:
                                    changeRoom(inputmsg);
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException | DuplicateUsernameException | ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                closeConnections();
            }
        }

        private Message registerUser(Message inputmsg) throws SQLException, IOException {
            logger.info("register!");
            Message msg = new Message();
            boolean check = true;
            String query;
            String username = inputmsg.getName();
            String password = inputmsg.getPassword();
            String email = inputmsg.getEmail();

            Connection con = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/slack?autoReconnect=true&useSSL=false", "root", "root");
            logger.info("Connection to DB established");
            Statement stmt = (Statement) con.createStatement();
            query = "SELECT username, email FROM user;";
            stmt.executeQuery(query);
            ResultSet rs = stmt.getResultSet();

            while (rs.next()) {
                String DBusername = rs.getString("username");
                String DBemail = rs.getString("email");
                if (username.equals(DBusername) || email.equals(DBemail)) {
                    check = false;
                    msg.setMsg("Username or e-mail already exists!");
                    logger.info("Username exists");
                    msg.setType(MessageType.REGISTER);
                    con.close();
                    break;
                }
            }
            if (check == true) {
                query = "insert into user (username, password, email, room)"
                        + " values (?, ?, ?, ?)";
                PreparedStatement preparedStmt = (PreparedStatement) con.prepareStatement(query);
                preparedStmt.setString(1, username);
                preparedStmt.setString(2, password);
                preparedStmt.setString(3, email);
                preparedStmt.setString(4, "default");
                preparedStmt.execute();

                logger.info("Created new user profile");
                con.close();
                msg.setMsg("Registered successfully");
                logger.info("Registered");
                msg.setType(MessageType.REGISTER);
            }
            /*OutputStream os = socket.getOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(os);
            output.writeObject(msg);
            output.reset();*/
            return msg;
        }

        private Message changeStatus(Message inputmsg) throws IOException {
            logger.debug(inputmsg.getName() + " has changed status to  " + inputmsg.getStatus());
            Message msg = new Message();
            msg.setName(user.getName());
            msg.setType(MessageType.STATUS);
            msg.setMsg("");
            User userObj = names.get(name);
            userObj.setStatus(inputmsg.getStatus());
            write(msg);
            return msg;
        }

        private Message changeRoom(Message inputmsg) throws IOException {
            logger.info(inputmsg.getName() + " has changed room to  " + inputmsg.getRoom());
            Message msg = new Message();
            msg.setName(user.getName());
            msg.setType(MessageType.ROOM);
            msg.setRoom(inputmsg.getRoom());
            msg.setMsg("");
            User userObj = names.get(name);
            userObj.setRoom(inputmsg.getRoom());
            write(msg);
            return msg;
        }

        private synchronized void checkDuplicateUsername(Message firstMessage) throws DuplicateUsernameException, SQLException {
            logger.info(firstMessage.getName() + " is trying to connect");
            Connection con = (Connection) DriverManager.getConnection("jdbc:mysql://localhost:3306/slack?autoReconnect=true&useSSL=false", "root", "root");
            Statement stmt = (Statement) con.createStatement();
            String query = "SELECT username, password, email FROM user;";
            stmt.executeQuery(query);
            ResultSet rs = stmt.getResultSet();
            boolean check = false;
            String DBemail = null;
            while (rs.next()) {
                String DBusername = rs.getString("username");
                String DBpassword = rs.getString("password");
                DBemail = rs.getString("email");
                if (firstMessage.getName().equals(DBusername) && firstMessage.getPassword().equals(DBpassword)) {
                    check = true;
                    con.close();
                    break;
                }
            }
            if (check && !names.containsKey(firstMessage.getName())) {
                this.name = firstMessage.getName();
                user = new User();
                user.setName(firstMessage.getName());
                user.setEmail(DBemail);
                user.setStatus(Status.ONLINE);
                user.setRoom(Rooms.DEFAULT);
                user.setPicture(firstMessage.getPicture());

                users.add(user);
                names.put(name, user);

                logger.info(name + " has been added to the list");
            } else {
                logger.error(firstMessage.getName() + " is already connected");
                throw new DuplicateUsernameException(firstMessage.getName() + " is already connected");
            }
        }

        private Message sendNotification(Message firstMessage) throws IOException {
            Message msg = new Message();
            msg.setMsg("has joined the chat.");
            msg.setType(MessageType.NOTIFICATION);
            msg.setName(firstMessage.getName());
            msg.setRoom(firstMessage.getRoom());
            msg.setPicture(firstMessage.getPicture());
            write(msg);
            return msg;
        }


        private Message removeFromList() throws IOException {
            logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left the chat.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setName("SERVER");
            msg.setUserlist(names);
            write(msg);
            logger.debug("removeFromList() method Exit");
            return msg;
        }

        /*
         * For displaying that a user has joined the server
         */
        private Message addToList() throws IOException {
            Message msg = new Message();
            msg.setMsg("Welcome, You have now joined the server! Enjoy chatting!");
            msg.setRoom(Rooms.DEFAULT);
            msg.setType(MessageType.CONNECTED);
            msg.setName("SERVER");
            write(msg);
            return msg;
        }

        /*
         * Creates and sends a Message type to the listeners.
         */
        private void write(Message msg) throws IOException {
            for (ObjectOutputStream writer : writers) {
                msg.setUserlist(names);
                msg.setUsers(users);
                msg.setOnlineCount(names.size());
                logger.info(writer.toString() + " " + msg.getName() + " " + msg.getUserlist().toString());
                try {
                    writer.writeObject(msg);
                    writer.reset();
                } catch (Exception ex) {
                    closeConnections();
                }
            }
        }

        /*
         * Once a user has been disconnected, we close the open connections and remove the writers
         */
        private synchronized void closeConnections() {
            logger.debug("closeConnections() method Enter");
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            if (name != null) {
                names.remove(name);
                logger.info("User: " + name + " has been removed!");
            }
            if (user != null){
                users.remove(user);
                logger.info("User object: " + user + " has been removed!");
            }
            try {
                removeFromList();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
        }
    }
}
