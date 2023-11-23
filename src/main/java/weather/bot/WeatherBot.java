package weather.bot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

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
                    getTodayWeather(id, msg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) // Who are we sending a message to
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

    private Text getTodayWeather(Long who, Message msg) throws IOException, InterruptedException {
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
                    URI.create("https://api.weatherapi.com/v1/forecast.json?q=Chartres%2CFR&days=1"))
                    .header("key", key)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        }
        return null;
    }

}
