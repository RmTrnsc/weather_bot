package weather.bot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
// import java.util.Iterator;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.w3c.dom.Text;

public class WeatherBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "WeatherBot";
    }

    @Override
    public String getBotToken() {
        return "6864653045:AAHMHNXcE6M7V-kSBGTsT2Ud9r0bP9yaIVg";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        User user = msg.getFrom();
        Long id = user.getId();

        if (msg.isCommand()) {
            if (msg.getText().equals("/today"))
                try {
                    sendTodayWeather(id, msg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
        }
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) // Who are we sending a message to
                .parseMode("Markdown")
                .text(what).build(); // Message content
        try {
            execute(sm); // Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e); // Any error will be printed here
        }
    }

    public void copyMessage(Long who, Integer msgId) {
        CopyMessage cm = CopyMessage.builder()
                .fromChatId(who.toString()) // We copy from the user
                .chatId(who.toString()) // And send it back to him
                .messageId(msgId) // Specifying what message
                .build();
        try {
            execute(cm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTodayWeather(Long who, Message msg)
            throws IOException, InterruptedException, JSONException, ParseException {
        if (msg.hasText()) {
            String key = null;
            try (InputStream input = new FileInputStream("local.properties")) {
                Properties prop = new Properties();
                prop.load(input);
                key = prop.getProperty("weather_api_key");
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://api.weatherapi.com/v1/current.json?q=Chartres%2CFR&days=1&lang=fr"))
                    .header("key", key)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JSONObject obj = new JSONObject(response.body());

            // Retreive and display all JsonObject keys
            // Uncomment Iterator import
            // Iterator<String> keys = obj.keys();
            // while (keys.hasNext()) {
            // key = keys.next();
            // if (obj.get(key) instanceof JSONObject) {
            // System.out.println(key);
            // }
            // }

            JSONObject locationObj = obj.getJSONObject("location");
            JSONObject weatherObj = obj.getJSONObject("current");
            JSONObject conditionObj = weatherObj.getJSONObject("condition");

            String city = locationObj.getString("name");

            String pattern = "dd/MM/yyyy HH:mm";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, new Locale("fr", "FR"));
            String localtimeFormat = simpleDateFormat.format(new Date());

            String lastUpdate = weatherObj.getString("last_updated");
            Date lastUpdateDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", new Locale("fr", "FR"))
                    .parse(lastUpdate);
            String lastUpdateFormat = simpleDateFormat.format(lastUpdateDateFormat);

            Integer temperature = weatherObj.getInt("temp_c");
            Integer feelslike = weatherObj.getInt("feelslike_c");
            Integer windSpeed = weatherObj.getInt("wind_kph");
            String windDirection = weatherObj.getString("wind_dir");
            Integer gust = weatherObj.getInt("gust_kph");
            Integer precipitationMM = weatherObj.getInt("precip_mm");
            Integer humidity = weatherObj.getInt("humidity");
            Integer cloudy = weatherObj.getInt("cloud");
            Integer visibility = weatherObj.getInt("vis_km");
            Integer uv = weatherObj.getInt("uv");

            String conditionText = conditionObj.getString("text");

            ImageIcon conditionIcon = getWeatherIcon(
                    conditionObj.getString("icon"),
                    weatherObj.getInt("is_day"));

            String result = localtimeFormat + "\n"
                    + "Yo! \n"
                    + "Voici la météo pour *" + city + "*\n"
                    + "*Condition => *" + conditionText + " " + conditionIcon + "\n"
                    + "*Température => *" + temperature + "C°, ressenti => " + feelslike + "C°\n"
                    + "*Vitesse du vent => *" + windSpeed + "avec des rafales à," + gust
                    + "kp/h direction => " + windDirection + "\n"
                    + "*Précipitaion mesurée => *" + precipitationMM + "mm\n"
                    + "*Humidité => *" + humidity + "%\n"
                    + "*Couverture nuageuse => *" + cloudy + "%\n"
                    + "*Visibilité => *" + visibility + "Km\n"
                    + "*Indice UV => *" + uv + "\n"
                    + "_Dernière mise à jour " + lastUpdateFormat + "_";

            sendText(who, result);
        }
    }

    private ImageIcon getWeatherIcon(String iconText, Integer is_day) throws IOException {

        ImageIcon icon;

        String[] splitStr = iconText.split("[/]");
        String iconString = splitStr[splitStr.length - 1];

        if (is_day == 1) {
            File file = new File("src\\resources\\day\\" + iconString);
            BufferedImage img = ImageIO.read(file);

            icon = new ImageIcon(img);
            
        } else if (is_day == 0) {
            File file = new File("src\\resources\\night\\" + iconString);
            BufferedImage img = ImageIO.read(file);

            icon = new ImageIcon(img);
        } else {
            icon = null;
        }

        return icon;
    }

}
