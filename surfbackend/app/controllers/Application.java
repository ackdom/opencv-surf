package controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;

import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import redis.clients.jedis.Jedis;
import views.html.index;
import views.html.learn;

public class Application extends Controller {

    public static Result index() {
    	Jedis jedis = new Jedis("localhost");
    	Set<String> smembers = jedis.smembers("mylist");
    	jedis.save();
        return ok(index.render("Your new application is ready."));
    }
    
    public static Result learnForm() {
		return ok(learn.render(""));    	
    }
    
    public static Result learnPicture() {
    	 MultipartFormData body = request().body().asMultipartFormData();
    	  FilePart picture = body.getFile("picture");
    	  if (picture != null) {
    	    String fileName = picture.getFilename();
    	    String contentType = picture.getContentType(); 
    	    File file = picture.getFile();
            
    	    BufferedImage image = null;
			try {
				image = ImageIO.read(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	    BufferedImage thumbnail =
    	    		  Scalr.resize(image, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH,
	    		               640, 480, Scalr.OP_ANTIALIAS);
    	    
    	    try {
    	    	
    	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	    	ImageIO.write( thumbnail, "jpg", baos );
    	    	baos.flush();
    	    	byte[] imageInByte = baos.toByteArray();
    	    	baos.close();    	    	
    	    	MessageDigest md = MessageDigest.getInstance("MD5");
    	    	md.update(imageInByte);    	    	
    	    	String md5 = new BigInteger(1, md.digest()).toString(16); // Encrypted 
    	    	File finalFile = new File("public/images/", md5+".jpg");
    	    	FileUtils.writeByteArrayToFile(finalFile, imageInByte);
    	    	String[] args1 = {"./extractor", finalFile.getAbsolutePath() };
    	    	Runtime r = Runtime.getRuntime();
    	    	Process p = r.exec(args1);    	    	
            } catch (IOException ioe) {
            	
                System.out.println("Problem operating on filesystem");
            } catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	    
    	    
    	    
    	    return ok("File uploaded");
    	  } else {
    	    flash("error", "Missing file");
    	    return redirect(routes.Application.index());    
    	  }   	
    }
    
    public static Result similarity(String image, Double treshold) {
    	String md5 = image.split(".")[0];
    	Jedis jedis = new Jedis("localhost");
    	
    	//Descriptors of SELECTED image
    	Set<String> smembers = jedis.smembers(md5);
    	
    	Set<String> keys = jedis.keys("*");
    	//For All keys in Redis
    	for(String key : keys) {
    		//if key is not SELECTED key proceed
    		if(!key.equals(md5)) {
    			//get image descriptor haha
    			Set<String> otherDescriptors = jedis.smembers(key);
    			
    		} 
    	}
    	

    	
    	return TODO;
    	
    	
    }
    
    
    public static Result list() {
    	File dir = new File("public/images/");
    	ArrayList<File> files = new ArrayList<File>(Arrays.asList(dir.listFiles()));

    	
    	return ok(views.html.list.render(files));
    	
    }

    
    

}
