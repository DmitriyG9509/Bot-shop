package com.example.paspaysweets.service;

import com.example.paspaysweets.config.BotConfig;
import com.example.paspaysweets.model.Product;
import com.example.paspaysweets.model.ShopUser;
import com.example.paspaysweets.repository.ProductRepo;
import com.example.paspaysweets.repository.UserRepo;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final String NOT_REGISTERED = "Для взаимодействия с ботом пожалуйста зарегистрируйтесь. Напиминаю, регистрацию могут пройти только сотрудники Paspay работающие из офиса";

    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final BotConfig config;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            Optional<ShopUser> existingUser = userRepo.findByChatId(chatId);
            String messageText = update.getMessage().getText();
            if (existingUser.isEmpty()) {
                sendMessage(chatId, NOT_REGISTERED);
                return;
            } if (messageText.contains("/send") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                var textToSend =messageText.substring(messageText.indexOf(" "));
                var users = userRepo.findAll();
                for (ShopUser user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
                log.info(chatId + "sended message to all by ADMIN");
            } else if (messageText.contains("/addproduct") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                addProduct(update);
            }
//            else if (messageText.contains("/dropdata") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
//                dropData(update);
//            } else if (messageText.startsWith("/deleteuser") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
//                removeUser(update.getMessage());
//            }
//            else if (messageText.startsWith("/removebl") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
//                processRemoveFromBlackListCommand(update.getMessage());
//            }
//        else {
//                switch (messageText) {
//                    case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
//                    case "/info" -> {
//                        sendMessage(chatId, INFO_TEXT);
//                        log.info(chatId + "requested info");
//                    }
//                    case "/register" -> {
//                        if (userRepository.findById(chatId).isEmpty()) {
//                            register(chatId);
//                        } else {
//                            sendMessage(chatId, "Вы уже зарегистрированы.");
//                        }
//                    }
//                    case "/forgetme" -> deleteUser(update.getMessage());
//                    case "Доступные вакансии" -> handleVacanciesButton(chatId);
//                    case "Обо мне" -> sendMessage(chatId, ABOUT_ME);
//                    default -> sendMessage(chatId, "Данной команды не существует");
//                }
//            }
        } else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (update.getCallbackQuery().getMessage().hasText()) {
                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                if (callBackData.equals("YES_BUTTON")) {
                    registerUser(update.getCallbackQuery().getMessage());
                    String text = "Вы успешно зарегистрировались. Теперь вы будете получать все обновления по доступным вакансиям ✉";
                    EditMessageText message = new EditMessageText();
                    message.setChatId(String.valueOf(chatId));
                    message.setText(text);
                    message.setMessageId((int) messageId);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        log.error("Error occured: Exception thrown in YES button" + e);
                    }
                } else if (callBackData.equals("NO_BUTTON")) {
                    String text = "Вы не будете получать обновления по вакансиям ✘";
                    EditMessageText message = new EditMessageText();
                    message.setChatId(String.valueOf(chatId));
                    message.setText(text);
                    message.setMessageId((int) messageId);
                    try {
                        execute(message);
                    } catch (TelegramApiException e) {
                        log.error("Error occured: Exception thrown in NO button " + e);
                    }
                }
                else {
                    log.error("Invalid callbackData: " + callBackData);
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

    public void addProduct(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        String[] parts = messageText.split("\\|");
        if (parts.length == 4) {
            String  productName = parts[0].trim();
            String command = "/addproduct";
            if (productName.startsWith(command)) {
                productName = productName.substring(command.length()).trim();
            }
            Long price = Long.valueOf(parts[1].trim());
            Long quantity = Long.valueOf(parts[2].trim());
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
        } else {
            switch (messageText) {
                case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case "/info" -> {
                    sendMessage(chatId, INFO_TEXT);
                    log.info(chatId + "requested info");
                }
                case "/register" -> {
                    if (userRepo.findById(chatId).isEmpty()) {
                        registerUser(messageText);
                    } else {
                        sendMessage(chatId, "Вы уже зарегистрированы.");
                    }
                }
                case "/forgetme" -> deleteUser(update.getMessage());
                case "Доступные вакансии" -> handleVacanciesButton(chatId);
                case "Обо мне" -> sendMessage(chatId, ABOUT_ME);
                default -> sendMessage(chatId, "Данной команды не существует");
            }
        }

    }

    private void registerUser(Message msg) {
        long chatId = msg.getChatId();
        Chat chat = msg.getChat();

        User user = new User();
        user.setChatId(chatId);
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setUserName(chat.getUserName());
        user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

        userRepository.save(user);

        String successMessage = "Вы успешно зарегистрированы. Теперь вы будете получать все обновления по доступным вакансиям ✉";
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
