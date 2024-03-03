package com.example.paspaysweets.service;

import com.example.paspaysweets.config.BotConfig;
import com.example.paspaysweets.model.Product;
import com.example.paspaysweets.model.ShopUser;
import com.example.paspaysweets.repository.ProductRepo;
import com.example.paspaysweets.repository.UserNamesRepo;
import com.example.paspaysweets.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final String NOT_REGISTERED = "Для взаимодействия с ботом пожалуйста зарегистрируйтесь. Напиминаю, регистрацию могут пройти только сотрудники Paspay работающие из офиса";
    private final String INFO_TEXT = "иформация";
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final BotConfig config;
    private final UserNamesRepo userNamesRepo;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            Optional<ShopUser> existingUser = userRepo.findByChatId(chatId);
            String messageText = update.getMessage().getText();
//            if (existingUser.isEmpty()) {
//                registerControl(update);
//                return;
//            }
            if (existingUser.isEmpty()) {
                if (update.getMessage().getText().equals("/register")) {
                    registerControl(update);
                    return;
                }
                sendMessage(chatId, NOT_REGISTERED);
                return;
            }
            if (messageText.contains("/send") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                var textToSend = messageText.substring(messageText.indexOf(" "));
                var users = userRepo.findAll();
                for (ShopUser user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
                log.info(chatId + "sended message to all by ADMIN");
            } else if (messageText.contains("/addproduct") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                addProduct(update);
            }
            else if (messageText.contains("/dropdata") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                dropData(update);
            }
            else if (messageText.startsWith("/deleteuser") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                removeUser(update);
            }

        else {
                switch (messageText) {
                    case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    case "/info" -> {
                        sendMessage(chatId, INFO_TEXT);
                        log.info(chatId + "requested info");
                    }
                    case "Список продуктов" -> sendProductList(chatId);
                    //case "Обо мне" -> sendMessage(chatId, ABOUT_ME);
                    default -> sendMessage(chatId, "Данной команды не существует");
                }
            }
        }

    }

    public boolean isChatIdBotOwner(List<Long> botOwners, long chatId) {
        for (Long owner : botOwners) {
            if (owner == chatId) {
                return true;
            }
        }
        return false;
    }

    //пример команды:  /deleteuser DmitriyGerassimenko
    @Transactional
    public void removeUser(Update update) {
        String[] commandParts = update.getMessage().getText().split(" ");
        if (commandParts.length == 2) {
            try {
                String targetUserName = commandParts[1];
                var chatId = update.getMessage().getChatId();
                userRepo.deleteByUserName(targetUserName);
                sendMessage(chatId, "Вы успешно удалили пользователя из списка ✅");
                log.info("User removed from the table by user " + chatId);
            } catch (Exception e) {
                long chatId = update.getMessage().getChatId();
                sendMessage(chatId, "Ошибка при удалении пользователя. Пожалуйста, попробуйте ещё раз.");
                log.error("Error occurred while removing user by user " + chatId, e);
            }
        }
    }

    //пример команды: /dropdata
    public void dropData(Update update) {
        try {
            productRepo.deleteAll();
            long chatId = update.getMessage().getChatId();
            sendMessage(chatId, "Все продукты успешно удалены из таблицы ✅");
            log.info("All data dropped from the table by user " + chatId);
        } catch (Exception e) {
            long chatId = update.getMessage().getChatId();
            sendMessage(chatId, "Ошибка при удалении данных. Пожалуйста, попробуйте ещё раз.");
            log.error("Error occurred while dropping data by user " + chatId, e);
        }
    }

    public void sendProductList(Long chatId) {
        List<Product> products = productRepo.findAll();
        boolean isUserRegistered = isUserRegistered(chatId);

        if (isUserRegistered) {
            if (!products.isEmpty()) {
                for (Product product : products) {
                    String callbackData = "purchase_product_" + product.getId();
                    InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                    InlineKeyboardButton button = new InlineKeyboardButton("Приобрести");
                    button.setCallbackData(callbackData);
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(button);
                    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                    keyboard.add(row);
                    keyboardMarkup.setKeyboard(keyboard);

                    StringBuilder productText = new StringBuilder();
                    productText
                            .append("Наименование: ").append(product.getProductName()).append("\n")
                            .append("Цена: ").append(product.getPrice()).append("\n")
                            .append("Имеется в наличии: ").append(product.getQuantity()).append("\n\n\n");

                    sendMessageWithInlineKeyboard(chatId, productText.toString(), keyboardMarkup);
                }
                log.info("The user requested product list: " + chatId);
            } else {
                sendMessage(chatId, "На данный момент продуктов нет, пожалуйста, оставайтесь на связи и проверяйте список, они обязательно появятся!");
                log.info("The user requested product list, no available products now: " + chatId);
            }
        } else {
            String registrationMessage = "Для доступа к списку продуктов, пожалуйста, зарегистрируйтесь.";
            sendMessage(chatId, registrationMessage);
            log.info("User is not registered, prompting for registration: " + chatId);
        }
    }

    public void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message with inline keyboard: " + e);
        }
    }

    private boolean isUserRegistered(long chatId) {
        Optional<ShopUser> user = userRepo.findByChatId(chatId);
        return user.isPresent();
    }

    public void registerControl(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        Message message = update.getMessage();
        switch (messageText) {
            case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            case "/info" -> {
                sendMessage(chatId, INFO_TEXT);
                log.info(chatId + "requested info");
            }
            case "/register" -> {
                if (userRepo.findById(chatId).isEmpty()) {
                    registerUser(message);
                } else {
                    sendMessage(chatId, "Вы уже зарегистрированы.");
                }
            }
        }
    }
    //пример команды: /addproduct|Nuts|200|10
    public void addProduct(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        String[] parts = messageText.split("\\|");
        if (parts.length == 4) {
            String productName = parts[1].trim();
            String command = "/addproduct";
            if (productName.startsWith(command)) {
                productName = productName.substring(command.length()).trim();
            }
            Long price = Long.valueOf(parts[2].trim());
            Long quantity = Long.valueOf(parts[3].trim());
            Product product = new Product();
            product.setProductName(productName);
            product.setPrice(price);
            product.setQuantity(quantity);
            productRepo.save(product);
            sendMessage(chatId, "продукт " + productName + " в количестве " + quantity + " с ценой " + price + " успешно добавлен в базу ✅");
            log.info("Product added to db by user " + chatId);
        } else {
            sendMessage(chatId, "Пожалуйста, убедитесь, что ввод содержит название продукта, его цену и количество, разделенные символом '|'. Пример ввода:  Nuts|200|10");
            log.error("Product not added, owner's mistake " + chatId);
        }

    }

    private void registerUser(Message msg) {
        long chatId = msg.getChatId();
        Chat chat = msg.getChat();
        var ifInclude = userNamesRepo.findByUserNameOffice(chat.getUserName());
        if (ifInclude.isPresent()) {
            String notExistMessage = "Вы не являетесь сотрудником офиса paspay в городе Караганда. Регистрироваться могут только сотрудники которые работают в inHouse формате";
            SendMessage notExistResponse = new SendMessage();
            notExistResponse.setChatId(String.valueOf(chatId));
            notExistResponse.setText(notExistMessage);
        }
        ShopUser user = new ShopUser();
        user.setChatId(chatId);
        user.setUserName(chat.getUserName());
        user.setName(chat.getFirstName());
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

        userRepo.save(user);

        String successMessage = "Вы успешно зарегистрированы. Теперь вы можете пользоваться функциями бота и кушоть вкусняшки \uD83C\uDF6B";
        SendMessage successResponse = new SendMessage();
        successResponse.setChatId(String.valueOf(chatId));
        successResponse.setText(successMessage);

        try {
            execute(successResponse);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending registration success message: " + e);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + "! Я создал для того чтобы облегчить твои покупки вкусняшек в Paspay) Для дальнейшего взаимодействия со мной пожалуйста зарегистрируйся(тыкни кнопку регистрация). " +
                "\nВот краткое описание команд и кнопок, доступных в боте: \n /start - список команд \n /register - подписаться на бота и рассылку" +
                "\n /forgetMe - отписаться от бота \n /info - информация о боте";
        log.info("Replied START command to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Список продуктов");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured while sending message " + e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }


}
