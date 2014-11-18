package com.sjsu.techknowgeek.pvault;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
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
    private static final String salt = "k9l2a42t";
    private final Cipher cipher;
    private final Cipher dcipher;

    /**
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

    /**
     *
     * @param input
     * @return
     * @throws IOException
     * @throws IllegalBlockSizeException
     */
    protected <T extends Serializable> SealedObject encrypt(T input) throws IOException, IllegalBlockSizeException
    {
        return new SealedObject(input, cipher);
    }

    /**
     *
     * @param input
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    protected Object decrypt(SealedObject input) throws IOException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException
    {
        return input.getObject(dcipher);
    }
}
