package Server.Phases;

import Api.ApiRequest;
import org.json.JSONObject;

import java.awt.image.BufferedImage;


public class Querying {

    public static Object getResponse(String city_id, String lon, String lat, String extraction){

        ApiRequest request= new ApiRequest();
        if (extraction.equals("map")) {
            BufferedImage imageResponse = request.GetResponseasImg(city_id,
                    lon, lat, "temp_new");
            return imageResponse;
        }else {
            JSONObject jsonResponse = request.GetResponseasJson(city_id,
                    lon, lat, extraction);
            return jsonResponse;
        }

    }
}
