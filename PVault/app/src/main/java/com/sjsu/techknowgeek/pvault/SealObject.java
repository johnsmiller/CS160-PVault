//Encrpytion approach adpted from http://stackoverflow.com/questions/6262374/encryption-with-aes-algorithm-in-java
//Used to encrypt/decrypt model class objects

package com.sjsu.techknowgeek.pvault;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by John on 11/13/2014.
 */
public class SealObject {
    private static final String salt = "zfl2a12c";
    private final Cipher cipher;
    private final Cipher dcipher;

    /**Initiates cipher for encryption/decryption. Not actually used since professor cleared not
     * doing actual encryption
     *
     * @param Password
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public SealObject(String Password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException
    {
        char[] password = Password.toCharArray();

        SecretKeyFactory fact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt.getBytes(), 1024, 128);
        SecretKey tmp = fact.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        cipher = Cipher.getInstance("AES");
        dcipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        dcipher.init(Cipher.DECRYPT_MODE, secret);
    }

    /** The would-be encryption would go here. Professor cleared not doing actual encryption
     *
     * @param input
     * @return
     */
    protected static void encrypt(File input)
    {
        return;
    }

    /**The would-be decryption would go here. Professor cleared not doing actual decryption
     *
     * @param input
     * @return
     */
    protected static void decrypt(File input)
    {
        return;
    }
	
	protected static String encryptPass(String Password)
    {
        String md5 = null;
        
        if(Password == null) 
            return null;
        
        Password = salt + Password;
        
        try {
            //Create MessageDigest object for MD5
            MessageDigest digest = MessageDigest.getInstance("MD5");

            //Update input string in message digest
            digest.update(Password.getBytes(), 0, Password.length());

            //Converts message digest value in base 16 (hex) 
            md5 = new BigInteger(1, digest.digest()).toString(16);

            } catch (NoSuchAlgorithmException e) {}
        
            return md5;
    }
    
    protected static boolean passCheck(String passInput, String storedPass)
    {
        String md5 = encryptPass(passInput);
        
        if(md5 == null || storedPass == null) 
            return false;
        
        return storedPass.equals(md5);
    }
}
