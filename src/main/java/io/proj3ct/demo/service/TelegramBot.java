package io.proj3ct.demo.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.proj3ct.demo.config.BotConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    static final String HELP_TEXT = "Type /start to start your work with me\n\n" +
            "Type /help to see help text messages\n\n" +
            "Type /info to get information about this bot and why it is useful";
    static final String INFOTEXT = "asd";

    private enum BotState {
        DEFAULT,
        AWAITING_TASK_NAME,
        AWAITING_TASK_PETITION
    }

    private BotState botState;
    private Map<Long, String> taskMap;

    public TelegramBot(BotConfig config) {
        this.config = config;
        this.botState = BotState.DEFAULT;
        this.taskMap = new HashMap<>();
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Розпочати"));
        listOfCommands.add(new BotCommand("/info", "Інформація про бота"));
        listOfCommands.add(new BotCommand("/help", "Як працювати з ботом"));
        listOfCommands.add(new BotCommand("/newpetition", "Створити нову петицію"));
        listOfCommands.add(new BotCommand("/showpetitions", "Показати всі петиції"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    public String getBotToken() {
        return config.getToken();
    }

    private String currentTaskName;
    private String currentTaskText;
    private int id;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
                switch (messageText) {
                    case "/newpetition":
                    case "Створити петицію":
                        botState = BotState.AWAITING_TASK_NAME;
                        sendMessage(chatId, "Введіть прізвище та ім'я:");
                        break;
                    default:
                        if (botState == BotState.AWAITING_TASK_NAME) {
                            currentTaskName = messageText;
                            botState = BotState.AWAITING_TASK_PETITION;
                            sendMessage(chatId, "Напишіть петицію:");
                        } else if (botState == BotState.AWAITING_TASK_PETITION) {
                            currentTaskText = messageText;
                            botState = BotState.DEFAULT;

                            // Создайте объект Petition или выполните другие действия
                            Petition petition = new Petition(currentTaskName, currentTaskText);

                            // Очистите переменные после создания петиции
//                            currentTaskName = null;
//                            currentTaskText = null;

                            // Отправьте подтверждение или выполните другие действия
                            sendMessage(chatId, "Петиція створена, ви можете побачити її на сайті:\nhttps://fendyx.github.io/bigtournaments/#/notes");
                            Gson gson = new Gson();
                            String json = gson.toJson(petition);

                            System.out.println(json);

                            // Создать HTTP клиент
                            HttpClient httpClient = HttpClientBuilder.create().build();

                            // Создать запрос GET для получения текущего JSON
                            HttpGet httpGet = new HttpGet("https://api.jsonstorage.net/v1/json/be8e5401-c3c6-4f0e-80e3-b2c5c298b404/36cdb5ce-1350-429b-b30f-25054dfa2ead?apiKey=140804b5-11a9-4b48-b42c-fb96ca00f76c");

                            try {
                                // Выполнить запрос GET
                                HttpResponse response = httpClient.execute(httpGet);
                                HttpEntity entity = response.getEntity();

                                if (entity != null) {
                                    // Получить ответ в виде строки JSON
                                    String jsonResponse = EntityUtils.toString(entity);

                                    // Преобразовать строку JSON в объект JSON
                                    JSONObject jsonObject = new JSONObject(jsonResponse);

                                    // Получить массив петиций
                                    JSONArray petitions = jsonObject.getJSONArray("petitions");
                                    int id = petitions.length() + 1;

                                    // Добавить новую петицию в массив
                                    JSONObject newPetition = new JSONObject();
                                    newPetition.put("id", id);
                                    newPetition.put("petition", currentTaskText);
                                    newPetition.put("name", currentTaskName);
                                    petitions.put(newPetition);

                                    // Обновить JSON объект
                                    jsonObject.put("petitions", petitions);

                                    // Преобразовать JSON объект обратно в строку
                                    String updatedJson = jsonObject.toString();

                                    // Создать запрос PATCH
                                    HttpPut httpPatch = new HttpPut("https://api.jsonstorage.net/v1/json/be8e5401-c3c6-4f0e-80e3-b2c5c298b404/36cdb5ce-1350-429b-b30f-25054dfa2ead?apiKey=140804b5-11a9-4b48-b42c-fb96ca00f76c");

                                    // Установить содержимое запроса PATCH
                                    StringEntity patchEntity = new StringEntity(updatedJson, ContentType.APPLICATION_JSON);
                                    httpPatch.setEntity(patchEntity);

                                    // Установить заголовки запроса PATCH
                                    httpPatch.setHeader("Accept", "application/json");
                                    httpPatch.setHeader("Content-type", "application/json");

                                    // Выполнить запрос PATCH
                                    HttpResponse patchResponse = httpClient.execute(httpPatch);

                                    // Вывести результат запроса PATCH
                                    System.out.println("Response code: " + patchResponse.getStatusLine().getStatusCode());
                                }
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }

                        }
                         else {
                            switch (messageText){
                                case "/start":
                                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                                    sendMessageWithKeyboard(chatId, "Select what interests you");
                                    break;
                                case "/help":
                                    sendMessage(chatId, HELP_TEXT);
                                    break;
                                case "/info":
                                    sendMessage(chatId, INFOTEXT);
                                    break;
                                case "Write a petition":
                                    break;
                                case "/showpetitions":
                                    sendAllPetitions(chatId);
                                    break;
                            }

                        }
                        break;

                }
            }
        }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привіт, " + name + ", майбутнє в твоїх руках!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithKeyboard(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        // Create keyboard
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // First row of buttons
        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton("Створити петицію"));

        // Add rows to the keyboard
        keyboardRows.add(keyboardRow1);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void sendAllPetitions(long chatId) {
        HttpClient httpClient = HttpClientBuilder.create().build();

        // Создать запрос GET для получения текущего JSON
        HttpGet httpGet = new HttpGet("https://api.jsonstorage.net/v1/json/be8e5401-c3c6-4f0e-80e3-b2c5c298b404/36cdb5ce-1350-429b-b30f-25054dfa2ead?apiKey=140804b5-11a9-4b48-b42c-fb96ca00f76c");

        try {
            // Выполнить запрос GET
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                // Получить ответ в виде строки JSON
                String jsonResponse = EntityUtils.toString(entity);

                // Преобразовать строку JSON в объект JSON
                JSONObject jsonObject = new JSONObject(jsonResponse);

                // Получить массив петиций
                JSONArray petitions = jsonObject.getJSONArray("petitions");

                // Отправить каждую петицию в отдельном сообщении
                for (int i = 0; i < petitions.length(); i++) {
                    JSONObject petition = petitions.getJSONObject(i);
                    String name = petition.getString("name");
                    String petitionText = petition.getString("petition");

                    // Формирование текста сообщения
                    StringBuilder messageText = new StringBuilder();
                    messageText.append("Name: ").append(name).append("\n");
                    messageText.append("Petition: ").append(petitionText);


                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }







}
