package com.example.moviedb;

import com.scalar.db.exception.transaction.TransactionException;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class UserService {
    private ScalarDBOperations scalarDBOperations;

    public UserService() {
        this.scalarDBOperations = new ScalarDBOperations();
        try {
            scalarDBOperations.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ScalarDB", e);
        }
    }

    public boolean registerUser(String userId, String password, String email) {
        try {
            if (scalarDBOperations.isUserInUsersTable(userId)) {
                return false; // User already exists
            }
            System.out.println("Registering user: " + userId);

            Map<String, String> userDetails = new HashMap<>();
            userDetails.put("userId", userId);
            userDetails.put("password", hashPassword(password)); // Hashing the password
            userDetails.put("email", email);

            scalarDBOperations.addUserToUsersTable(userDetails);
            return true;
        } catch (TransactionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean loginUser(String userId, String password) {
        try {
            Map<String, String> userDetails = scalarDBOperations.getUserDetails(userId);
            if (userDetails != null) {
                String storedPassword = userDetails.get("password");
                return verifyPassword(password, storedPassword); // You should implement password verification
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, String> getUserProfile(String userId) {
        try {
            return scalarDBOperations.getUserDetails(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean verifyPassword(String inputPassword, String storedPassword) {
        String hashedInputPassword = hashPassword(inputPassword);
        return hashedInputPassword.equals(storedPassword);
    }

    //region friends

    public JsonNode searchUsers(String query) throws TransactionException {
        try{
            return scalarDBOperations.searchUsers(query);
        } catch (Exception e) {
            throw new TransactionException("Failed to search users: ", e.getMessage());
        }
    }

    

    public void sendFriendRequest(String requesterId, String requesteeId) throws TransactionException {
        scalarDBOperations.sendFriendRequest(requesterId, requesteeId);
    }

    public void acceptFriendRequest(String requesterId, String requesteeId) throws TransactionException {
        scalarDBOperations.acceptFriendRequest(requesterId, requesteeId);
    }

    public void declineFriendRequest(String requesterId, String requesteeId) throws TransactionException {
        scalarDBOperations.declineFriendRequest(requesterId, requesteeId);
    }

    public void removeFriendRequest(String userId, String friendId) throws TransactionException {
        scalarDBOperations.removeFriendRequest(userId, friendId);
    }

    public void removeFriend(String userId, String friendId) throws TransactionException {
        scalarDBOperations.removeFriend(userId, friendId);
    }

    public JsonNode listAllFriends(String userId) throws TransactionException {
        return scalarDBOperations.listAllFriends(userId);

    }

    public JsonNode listFriendRequests(String userId) throws TransactionException {
        return scalarDBOperations.listFriendRequests(userId);
    }

    //endregion


}
