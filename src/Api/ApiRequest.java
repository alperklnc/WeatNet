package Api;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;

public class ApiRequest {

	
	
	public JSONObject GetResponseasJson(String city_id, String lon, String lat, String exclude) {
		// Host url
		// Headers for a request
		String query=null;
		String Jsonhost;
		// Params
		String charset = "UTF-8";
		// Format query for preventing encoding problems
		JSONObject jsonObject = null;
		String app_id = "84077bf0e0064a07030ca97d2d717587";// Key for OpenWeatherMap
		long unixTime = Instant.now().getEpochSecond();
		try {
			if (exclude.equals("history")){
				Jsonhost = "https://api.openweathermap.org/data/2.5/onecall/timemachine";
				query = String.format("lat=%s&lon=%s&dt=%d&appid=%s", URLEncoder.encode(lat, charset),
						URLEncoder.encode(lon, charset), unixTime,
						URLEncoder.encode(app_id, charset));
			} else{
				Jsonhost = "https://api.openweathermap.org/data/2.5/onecall";
				query = String.format("lat=%s&lon=%s&exclude=%s&appid=%s", URLEncoder.encode(lat, charset),
						URLEncoder.encode(lon, charset), URLEncoder.encode(exclude, charset),
						URLEncoder.encode(app_id, charset));
			}
			// Get Json Response from openweathermap
			HttpResponse<JsonNode> response = Unirest.get(Jsonhost + "?" + query).asJson();
			jsonObject = response.getBody().getObject();
			//Execution of Code Snippet.
			System.out.println("Status of Response from API: "+response.getStatus());
			System.out.println("Content type: "+response.getHeaders().get("Content-Type"));
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonObject;

	}

	public BufferedImage GetResponseasImg(String city_id, String lon, String lat, String layer) {
		// Host url
		String MapHost = "https://tile.openweathermap.org/map/";
		String charset = "UTF-8";
		// Headers for a request
		String app_id = "84077bf0e0064a07030ca97d2d717587";// Type here your key
		// Params
		int z = 1; // zoom level
		Double x = Double.parseDouble(lat);
		Double y = Double.parseDouble(lon);
		BufferedImage Img=null;
		try {
			String tile =getTileNumber(x,y,z);
			String appIdQuery=String.format("?appid=%s", URLEncoder.encode(app_id, charset));
			// Get Image Response from openweathermap
			HttpResponse<InputStream> response = Unirest.get(MapHost +layer+tile+".png"+ appIdQuery).asBinary();
			//Execution of Code Snippet.
			System.out.println("Status of Response from API: "+response.getStatus());
			System.out.println("Content type: "+response.getHeaders().get("Content-Type"));
			Img= ImageIO.read(response.getBody());
			

		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Img;

	}
	
	public static String getTileNumber(final double lat, final double lon, final int zoom) {
		   int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
		   int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
		    if (xtile < 0)
		     xtile=0;
		    if (xtile >= (1<<zoom))
		     xtile=((1<<zoom)-1);
		    if (ytile < 0)
		     ytile=0;
		    if (ytile >= (1<<zoom))
		     ytile=((1<<zoom)-1);
		    return("/" + zoom + "/" + xtile + "/" + ytile);
		   }

}
