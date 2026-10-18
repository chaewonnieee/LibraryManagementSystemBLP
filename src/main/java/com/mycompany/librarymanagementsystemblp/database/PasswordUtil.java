package com.mycompany.librarymanagementsystemblp.database;

import java.security.MessageDigest;                 // Java's built-in hashing tool
import java.nio.charset.StandardCharsets;           // used to turn text into bytes

public class PasswordUtil {

    public static String hash(String password) {    // takes plain text, returns scrambled text
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");               // choose the SHA-256 method
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));   // scramble the password's bytes
            StringBuilder sb = new StringBuilder();                                // holds the final text
            for (byte b : bytes) {                                                 // go through each scrambled byte
                sb.append(String.format("%02x", b));                               // turn each byte into 2 hex characters
            }
            return sb.toString();                                                  // 64-character result
        } catch (Exception e) {                                                    // SHA-256 always exists in Java
            throw new RuntimeException(e);                                         // so this should never happen
        }
    }
}