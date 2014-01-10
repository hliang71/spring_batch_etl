package com.hliang.batch.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
	public static String hash(String key) throws NoSuchAlgorithmException {
	    MessageDigest md5 = MessageDigest.getInstance("MD5");        
	    byte[] passBytes = key.getBytes();
	    byte[] passHash = md5.digest(passBytes);  
	    StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<passHash.length;i++) {
    		String hex=Integer.toHexString(0xff & passHash[i]);
   	     	if(hex.length()==1) hexString.append('0');
   	     	hexString.append(hex);
    	}
	    return hexString.toString();
	}
}
