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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

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
import views.html.sorted;
public class Application extends Controller {

	static double tresh;

	static double min1 = Float.MAX_VALUE;

	static double min2 = Float.MAX_VALUE;

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
			BufferedImage thumbnail = Scalr.resize(image, Scalr.Method.SPEED,
					Scalr.Mode.FIT_TO_WIDTH, 640, 480, Scalr.OP_ANTIALIAS);

			try {

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(thumbnail, "jpg", baos);
				baos.flush();
				byte[] imageInByte = baos.toByteArray();
				baos.close();
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(imageInByte);
				String md5 = new BigInteger(1, md.digest()).toString(16); // Encrypted
				File finalFile = new File("public/images/", md5 + ".jpg");
				FileUtils.writeByteArrayToFile(finalFile, imageInByte);
				String[] args1 = { "./openCVTest", finalFile.getAbsolutePath() };
				Runtime r = Runtime.getRuntime();
				Process p = r.exec(args1);
				// System.out.println("Extractor returns :"+p.exitValue());

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
		String[] splits = image.split("\\.");
		String md5 = splits[0];
		Jedis jedis = new Jedis("localhost");
		tresh = treshold;

		Map<String, Integer> map = new HashMap<String, Integer>();
		// ValueComparator vc = new ValueComparator(map);
		// TreeMap<String, Integer> sortedMap = new TreeMap<String,
		// Integer>(vc);

		// Descriptors of SELECTED image
		Set<String> smembers = jedis.smembers(md5);

		Set<String> keys = jedis.keys("*");
		// For All keys in Redis
		System.out.println("Start search");

		for (String key : keys) {
			// if key is not SELECTED key proceed
			if (!key.equals(md5)) {
				// get image descriptor haha
				Set<String> otherDescriptors = jedis.smembers(key);
				// System.out.println("Mam deskriptory obrazku");

				int imageDistance = imageDistance(smembers, otherDescriptors);
				// System.out.println("Pro "+key+" jsem nasel "+imageDistance);

				map.put(key, imageDistance);
			}
		}

		map = (HashMap<String, Integer>) sortByValue(map);

		for (Entry<String, Integer> entry : map.entrySet()) {
			System.out.println(entry.getKey() + " with value "
					+ entry.getValue());
		}

		return ok(sorted.render(md5,map));

	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static int imageDistance(Set<String> descr1, Set<String> descr2) {

		int counter = 0;

		for (String s1 : descr1) {
			// System.out.println("aaaaa");

			// System.out.println(s1);

			// System.out.println("aaaaa");
			float[] descriptor1 = getDescriptors(s1);

			for (int i = 0; i < descriptor1.length; i++) {
				// System.out.print(descriptor1[i]+" ");
			}

			// System.out.println("dalsi deskriptor");
			min1 = Float.MAX_VALUE;
			min2 = Float.MAX_VALUE;

			for (String s2 : descr2) {
				float[] descriptor2 = getDescriptors(s2);
				double euclidDistance = euclidDistance(descriptor1, descriptor2);
				// double euclidDistance =
				// CosineSimilarity.cosineSimilarity(descriptor1, descriptor2);
				if (euclidDistance < min2) {
					min2 = euclidDistance;
					if (min2 < min1) {
						min2 = min1;
						min1 = euclidDistance;
					}
				}
			}

			double ratio = min1 / min2;
			if (ratio < tresh) {
				counter++;
			}
		}
		return counter;
	}

	static float[] getDescriptors(String descriptorString) {
		String[] split = descriptorString.split(" ");

		// List<Float> descrs = new ArrayList<Float>();
		float descrs[] = new float[split.length];
		int i = 0;
		for (String s : split) {
			descrs[i] = (Float.parseFloat(s));
			i++;
		}

		return descrs;

	}

	static double euclidDistance(float[] descriptor1, float[] descriptor2) {
		double result = 0;
		for (int i = 0; i < descriptor1.length; i++) {
			if (result > min2) {
				 return result;
			}
			result += (descriptor1[i] - descriptor2[i])
					* (descriptor1[i] - descriptor2[i]);
		}
		return result;
	}

	public static Result list() {
		File dir = new File("public/images/");
		ArrayList<File> files = new ArrayList<File>(Arrays.asList(dir
				.listFiles()));

		return ok(views.html.list.render(files));

	}

}
