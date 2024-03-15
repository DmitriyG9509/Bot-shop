package com.example.paspaysweets.service;

import com.example.paspaysweets.config.BotConfig;
import com.example.paspaysweets.model.Product;
import com.example.paspaysweets.model.ProductCategory;
import com.example.paspaysweets.model.ShopUser;
import com.example.paspaysweets.repository.CategoryRepo;
import com.example.paspaysweets.repository.ProductRepo;
import com.example.paspaysweets.repository.UserNamesRepo;
import com.example.paspaysweets.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
@Transactional
public class TelegramBot extends TelegramLongPollingBot {

    private final String NOT_REGISTERED = "Для взаимодействия с ботом пожалуйста зарегистрируйтесь. Напиминаю, регистрацию могут пройти только сотрудники Paspay работающие из офиса";
    private final String INFO_TEXT = "Я бот для облегчения процесса взаимодействия с магазином вкусняшек paspay. После регистрации вам доступна кнопка СПИСОК ПРОДУКТОВ." + ", по нажатии которой вам будет выведен соответствующий актуальный список продуктов. Вы можете выбрать нужный вам продукт нажатием кнопки ПРИОБРЕСТИ" + " Стоимость продукта будет зачислена в ваш долг либо списана с вашего кошелька. Для пополнения баланса обратитесь Светлане. Каждую пятницу вам будет приходить сообщение с суммой, которую нужно уплатить в бугалтерию. ";

    private final String INFO_TEXT_ADMIN = "Я бот для облегчения процесса взаимодействия с магазином вкусняшек paspay. После регистрации вам доступна кнопка СПИСОК ПРОДУКТОВ.\" +\n" + ", по нажатии которой вам будет выведен соответствующий актуальный список продуктов. Вы можете выбрать нужный вам продукт нажатием кнопки ПРИОБРЕСТИ\"" + "Стоимость продукта будет зачислена в ваш долг либо списана с вашего кошелька. Для пополнения баланса обратитесь Светлане. Каждую пятницу вам будет приходить сообщение с суммой, которую нужно уплатить в бугалтерию. " + "\nАдмин команды: \n• /send ваше сообщение  --  рассылка сообщения всем зарегистрированным пользователям" + "\n• /deleteuser DmitriyGerassimenko  --  удаление пользователя и всех его данных из базы(при увольнении)" + "\n• /dropdata  --  сброс базы данных с продуктами(полезно например когда пришла новая партия продуктов и нужно сбросить старые)" + "\n• /addproduct|Nuts|200|10|1  --  добавление продукта в базу данных. Писать строго со всеми знаками |. /addproduct|НАЗВАНИЕ ТОВАРА|ЦЕНА ТОВАРА|КоЛИЧество товара|категория товара" + "\n• /userduty 876545656  --  проверка суммы долга пользователя по chatId" + "\n• /cash|764756473|3000  --  зачисление денег на кошелек пользователя. /cash|ChatId|сумма" + "\n• /products  --  список продуктов, которые есть в магазине";
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final BotConfig config;
    private final UserNamesRepo userNamesRepo;
    private final CategoryRepo categoryRepo;

    public TelegramBot(ProductRepo productRepo, UserRepo userRepo, BotConfig config, UserNamesRepo userNamesRepo, CategoryRepo categoryRepo) {
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.config = config;
        this.userNamesRepo = userNamesRepo;
        this.categoryRepo = categoryRepo;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "начало"));
        listofCommands.add(new BotCommand("/info", "информация о боте"));
        listofCommands.add(new BotCommand("/register", "записать мои данные и подписаться на бота"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            Optional<ShopUser> existingUser = userRepo.findByChatId(chatId);
            String messageText = update.getMessage().getText();

            if (existingUser.isEmpty()) {
                if (update.getMessage().getText().equals("/start")) {
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
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
            } else if (messageText.contains("/dropdata") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                dropData(update);
            } else if (messageText.startsWith("/deleteuser") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                removeUser(update);
            } else if (messageText.startsWith("/userduty") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                userDuty(update);
            } else if (messageText.startsWith("/cash") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                putCash(update);
            } else if (messageText.startsWith("/products") && isChatIdBotOwner(config.getBotOwners(), chatId)) {
                getProducts(update);
            } else {
                switch (messageText) {
                    case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    case "/info" -> {
                        String responseText = isChatIdBotOwner(config.getBotOwners(), chatId) ? INFO_TEXT_ADMIN : INFO_TEXT;
                        sendMessage(chatId, responseText);
                        log.info(chatId + " requested info");
                    }
                    case "Список продуктов \uD83D\uDCDD" -> sendProductCategories(chatId);
                    case "Добавить продукт ➕" -> addProduct(update);
                    //case "Отправить сообщение всем \uD83D\uDCE9" -> handleSendMessageToAllCommand(update);
                    case "Удалить пользователя \uD83D\uDDD1\uFE0F" -> sendProductCategories(chatId);
                    case "Сброс всех продуктов ❌" -> sendProductCategories(chatId);
                    case "Проверка задолженности пользователя \uD83D\uDC6E" -> sendProductCategories(chatId);
                    case "Пополнение баланса пользователя \uD83D\uDCB0" -> sendProductCategories(chatId);
                    case "Список оставшихся продуктов \uD83E\uDDFE" -> sendProductCategories(chatId);
                    default ->
                            sendMessage(chatId, messageText.equals("/register") ? "Вы уже зарегистрированы" : "Данной команды не существует");
                }
            }
        }
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();
            String callbackData = callbackQuery.getData();
            var messageId = callbackQuery.getMessage().getMessageId();
            if (callbackData.startsWith("purchase")) {
                handlePurchaseCallback(chatId, callbackData);
            } else if (callbackData.startsWith("view_category")) {
                handleCategoryCallback(chatId, callbackData);
            } else if (callbackData.startsWith("confirm_purchase") || callbackData.startsWith("cancel_purchase")) {
                handleConfirmationCallback(chatId, callbackData, messageId);
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

    public void getProducts(Update update) {
        List<Product> products = productRepo.findAll();
        StringBuilder messageText = new StringBuilder("Список продуктов:\n");

        for (Product product : products) {
            // Формируем строку с названием и количеством продукта
            String productInfo = product.getProductName() + " - " + product.getQuantity() + " шт.\n";
            messageText.append(productInfo);
        }

        // Отправляем сформированный текст
        sendMessage(update.getMessage().getChatId(), messageText.toString());
    }

    //Пример: /cash|chatId|5000
    @Transactional
    public void putCash(Update update) {
        String[] commandParts = update.getMessage().getText().split("\\|");
        var chatId = update.getMessage().getChatId();
        if (commandParts.length == 3) {
            try {
                String targetUserChatId = commandParts[1];
                String targetSum = commandParts[2];
                Optional<ShopUser> userOptional = userRepo.findByChatId(Long.valueOf(targetUserChatId));
                if (userOptional.isEmpty()) {
                    sendMessage(chatId, "Пользователь не найден! Пожалуйста повторите запрос");
                    return;

                }
                ShopUser user = userOptional.get();
                var sum = user.getCash() + Long.parseLong(targetSum) - user.getDuty();
                if (user.getDuty() == Long.parseLong(targetSum)) {
                    user.setDuty(0L);
                    userRepo.save(user);
                    sendMessage(chatId, "Задолженность пользователя " + targetUserChatId + " погашена ✅");
                    sendMessage(Long.parseLong(targetUserChatId), "Ваш долг погашен ✅");
                    return;
                }
                if (sum >= 0 && user.getDuty() != 0) {
                    user.setCash(sum);
                    user.setDuty(0L);
                    userRepo.save(user);
                    sendMessage(chatId, "Баланс пользователя " + targetUserChatId + " пополнен, а так же списан его долг ✅");
                    sendMessage(Long.parseLong(targetUserChatId), "Ваш баланс пополнен, а так же списан долг ✅" + " Текущий баланс составляет " + sum);
                    return;
                } else if (sum >= 0 && user.getDuty() == 0) {
                    user.setCash(sum);
                    user.setDuty(0L);
                    userRepo.save(user);
                    sendMessage(chatId, "Баланс пользователя " + targetUserChatId + " пополнен ✅");
                    sendMessage(Long.parseLong(targetUserChatId), "Ваш баланс пополнен и составляет" + sum + " ✅");
                    return;
                } else {
                    user.setCash(0L);
                    user.setDuty(-(sum));
                    userRepo.save(user);
                    sendMessage(chatId, "Баланс пользователя " + targetUserChatId + " пополнен, частично списан его долг на сумму пополнения. Текущий долг " + (-(sum)));
                    sendMessage(Long.parseLong(targetUserChatId), "Ваш баланс пополнен, а так же частично списан долг ✅  Текущий долг " + (-(sum)));
                }
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка при списании долга. Пожалуйста, попробуйте ещё раз.");
                log.error("Error occurred while paying duty " + chatId, e);
            }
        }
    }


    private void userDuty(Update update) {
        List<ShopUser> users = userRepo.findAll();

        if (!users.isEmpty()) {
            StringBuilder dutyInfo = new StringBuilder("Долги пользователей:\n");

            for (ShopUser user : users) {
                dutyInfo.append(user.getUsername())
                        .append("   ")
                        .append(user.getDuty())
                        .append("ТГ")
                        .append("   ")
                        .append(user.getChatId())
                        .append("\n");
            }

            sendMessage(update.getMessage().getChatId(), dutyInfo.toString());
        } else {
            sendMessage(update.getMessage().getChatId(), "Нет данных о пользователях.");
        }
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

    public void sendProductCategories(Long chatId) {
        List<ProductCategory> categories = categoryRepo.findAll();
        boolean isUserRegistered = isUserRegistered(chatId);

        if (isUserRegistered) {
            if (!categories.isEmpty()) {
                for (ProductCategory category : categories) {
                    String callbackData = "view_category_" + category.getId();
                    InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                    InlineKeyboardButton button = new InlineKeyboardButton(category.getCategoryName());
                    button.setCallbackData(callbackData);
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(button);
                    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                    keyboard.add(row);
                    keyboardMarkup.setKeyboard(keyboard);

                    sendMessageWithInlineKeyboard(chatId, getCategoryEmoji(category), keyboardMarkup);
                }
                log.info("The user requested product categories: " + chatId);
            } else {
                sendMessage(chatId, "На данный момент категорий продуктов нет, пожалуйста, оставайтесь на связи и проверяйте список, они обязательно появятся!");
                log.info("The user requested product categories, no available categories now: " + chatId);
            }
        } else {
            String registrationMessage = "Для доступа к категориям продуктов, пожалуйста, зарегистрируйтесь.";
            sendMessage(chatId, registrationMessage);
            log.info("User is not registered, prompting for registration: " + chatId);
        }
    }

    private String getCategoryEmoji(ProductCategory category) {
        switch (category.getCategoryName()) {
            case "Сладости":
                return "\uD83C\uDF6B";
            case "Еда":
                return "🍔";
            case "Напитки":
                return "\uD83E\uDD64";
            default:
                return "❓";
        }
    }

    @Scheduled(cron = "0 0 17 * * FRI")
    public void sendFridayMessage() {
        var users = userRepo.findAll();

        for (ShopUser user : users) {
            var userDuty = user.getDuty();
            if (userDuty == 0) {
                continue;
            }
            sendMessage(user.getChatId(), "Привет! Ваш долг за вкусняшки на сегодня составляет: " + userDuty + ". Пожалуйста, занесите денюжку в бугалтерию. Хороших выходных!");
            ;
        }
        for (ShopUser user : users) {
            var duty = user.getDuty();
            long chatId = user.getChatId();
            String userName = user.getUsername();

            for (int i = 0; i < config.getBotOwners().toArray().length; i++) {
                sendMessage(config.getBotOwners()
                                  .get(i), "Пользователь " + userName + " (chatId: " + chatId + ") имеет задолженность: " + duty);
            }
        }

    }

    public void handleCategoryCallback(Long chatId, String callbackData) {
        String[] parts = callbackData.split("_");
        if (parts.length == 3 && parts[0].equals("view") && StringUtils.isNumeric(parts[2])) {
            Long categoryId = Long.parseLong(parts[2]);

            List<Product> products = productRepo.findByCategoryId(categoryId);

            if (!products.isEmpty()) {
                InlineKeyboardMarkup keyboardMarkup = createProductTable(products);

                sendMessageWithInlineKeyboard(chatId, "Товары в выбранной категории:", keyboardMarkup);
            } else {
                sendMessage(chatId, "В данной категории пока нет товаров.");
            }
        } else {
            sendMessage(chatId, "Ошибка обработки callback'а.");
        }
    }

    private InlineKeyboardMarkup createProductTable(List<Product> products) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Product product : products) {
            if (product.getQuantity() > 0) { // Проверка наличия товара
                // Создаем кнопку "приобрести" с уникальным callback'ом для каждого продукта
                String callbackData = "purchase_product_" + product.getId();
                String buttonText = product.getProductName() + " - " + product.getPrice() + " KZT"; // Обновленный текст кнопки
                InlineKeyboardButton button = new InlineKeyboardButton(buttonText);
                button.setCallbackData(callbackData);
                List<InlineKeyboardButton> row = Collections.singletonList(button);
                rows.add(row);
            }
        }

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    public void handlePurchaseCallback(Long chatId, String callbackData) {
        String[] parts = callbackData.split("_");

        if (parts.length == 3 && parts[0].equals("purchase") && StringUtils.isNumeric(parts[2])) {
            Long productId = Long.parseLong(parts[2]);
            Optional<Product> optionalProductBase = productRepo.findById(productId);

            if (optionalProductBase.isPresent()) {
                Product product = optionalProductBase.get();

                // Проверяем количество товара
                if (product.getQuantity() == 0) {
                    sendMessage(chatId, "К сожалению, товар закончился.");
                    return;
                }

                // Отправляем сообщение с вопросом о подтверждении покупки
                InlineKeyboardMarkup keyboardMarkup = createConfirmationKeyboard(productId);
                sendMessageWithInlineKeyboard(chatId, "Хотите приобрести товар?", keyboardMarkup);
            } else {
                sendMessage(chatId, "Товар с указанным идентификатором не найден.");
            }
        } else {
            sendMessage(chatId, "Произошла непредвиденная ошибка.");
            sendMessage(config.getBotOwners().get(0), "Не удалось купить товар, ошибка.");
        }
    }

    private InlineKeyboardMarkup createConfirmationKeyboard(Long productId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton("Да");
        yesButton.setCallbackData("confirm_purchase_" + productId);

        InlineKeyboardButton noButton = new InlineKeyboardButton("Нет");
        noButton.setCallbackData("cancel_purchase_" + productId);

        List<InlineKeyboardButton> row = Arrays.asList(yesButton, noButton);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    public void handleConfirmationCallback(Long chatId, String callbackData, int messageId) {
        String[] parts = callbackData.split("_");

        if (parts.length == 3 && parts[0].equals("confirm") && StringUtils.isNumeric(parts[2])) {
            handleConfirmPurchase(chatId, callbackData, messageId);
        } else if (parts.length == 3 && parts[0].equals("cancel") && StringUtils.isNumeric(parts[2])) {
            handleCancelPurchase(chatId, callbackData, messageId);
        } else {
            sendMessage(chatId, "Произошла непредвиденная ошибка.");
            sendMessage(config.getBotOwners().get(0), "Не удалось обработать подтверждение покупки, ошибка.");
        }
    }
    private void tryDeleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            sendMessage(config.getBotOwners().get(0), "Ошибка удаления сообщения подтверждения покупки, юзер " + chatId);
            log.error("Ошибка удаления сообщения подтверждения покупки, юзер" + chatId);
        }
    }

    private void handleConfirmPurchase(Long chatId, String callbackData, int messageId) {
        Long productId = Long.parseLong(callbackData.split("_")[2]);
        Optional<Product> optionalProductBase = productRepo.findById(productId);
        Optional<ShopUser> optionalShopUser = userRepo.findByChatId(chatId);
        if (optionalProductBase.isPresent() && optionalShopUser.isPresent()) {
            Product product = optionalProductBase.get();
            ShopUser user = optionalShopUser.get();

            // Уменьшаем количество товара
            if (product.getQuantity() > 0) {
                product.setQuantity(product.getQuantity() - 1);
                productRepo.save(product);
                var count = user.getCash() - product.getPrice();
                if (count >= 0) {
                    user.setCash(count);
                    userRepo.save(user);
                    sendMessage(chatId, "Вы успешно приобрели товар! Спасибо за покупку.");
                    tryDeleteMessage(chatId, messageId);
                } else {
                    user.setDuty((-(count)) + user.getDuty());
                    user.setCash(0L);
                    userRepo.save(user);
                    sendMessage(chatId, "Вы успешно приобрели товар! Спасибо за покупку.");
                    tryDeleteMessage(chatId, messageId);
                }
            } else {
                sendMessage(chatId, "К сожалению, товар закончился.");
                return;
            }
        } else {
            sendMessage(chatId, "Товар с указанным идентификатором не найден.");
        }
    }

    private void handleCancelPurchase(Long chatId, String callbackData, int messageId) {
        Long productId = Long.parseLong(callbackData.split("_")[2]);
        sendMessage(chatId, "Вы отменили покупку товара ❌");
        tryDeleteMessage(chatId, messageId);
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

    //пример команды: /addproduct|Nuts|200|10|1
    public void addProduct(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        String[] parts = messageText.split("\\|");
        if (parts.length == 5) {
            String productName = parts[1].trim();
            String command = "/addproduct";
            if (productName.startsWith(command)) {
                productName = productName.substring(command.length()).trim();
            }
            Long price = Long.valueOf(parts[2].trim());
            Long quantity = Long.valueOf(parts[3].trim());
            Long productType = Long.valueOf(parts[4].trim());
            Product product = new Product();
            product.setProductName(productName);
            product.setPrice(price);
            product.setQuantity(quantity);
            ProductCategory category = categoryRepo.findById(productType).orElse(null);
            product.setCategory(category);
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
        } else {
            String notExistMessage = "Вы не являетесь сотрудником офиса paspay в городе Караганда. Регистрироваться могут только сотрудники, которые работают в inHouse формате";
            SendMessage notExistResponse = new SendMessage();
            notExistResponse.setChatId(String.valueOf(chatId));
            notExistResponse.setText(notExistMessage);

            try {
                execute(notExistResponse);
            } catch (TelegramApiException e) {
                log.error("Error occurred while sending registration failure message: " + e);
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + "! Я создан для того чтобы облегчить твои покупки вкусняшек в Paspay) Для дальнейшего взаимодействия со мной пожалуйста зарегистрируйся(в \"меню\" выберите register). " + "\nВот краткое описание команд и кнопок, доступных в боте: \n /start - список команд \n /register - подписаться на бота и рассылку" + "\n /info - информация о боте";
        log.info("Replied START command to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        // Создаем клавиатуру с кнопками
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Позволяет клавиатуре подстраиваться под размер экрана
        keyboardMarkup.setOneTimeKeyboard(true); // Клавиатура скрывается после нажатия на кнопку

        // Создаем строки с кнопками
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Добавляем кнопки в строки
        KeyboardRow row1 = new KeyboardRow();
        row1.add("  Список продуктов \uD83D\uDCDD  ");
        keyboardRows.add(row1);

        if (isChatIdBotOwner(config.getBotOwners(), chatId)) {
            KeyboardRow row2 = new KeyboardRow();
            row2.add("  Добавить продукт ➕  ");
            keyboardRows.add(row2);

//            KeyboardRow row3 = new KeyboardRow();
//            row3.add("  Отправить сообщение всем \uD83D\uDCE9  ");
//            keyboardRows.add(row3);

            KeyboardRow row4 = new KeyboardRow();
            row4.add("  Удалить пользователя \uD83D\uDDD1\uFE0F  ");
            keyboardRows.add(row4);

            KeyboardRow row5 = new KeyboardRow();
            row5.add("  Сброс всех продуктов ❌  ");
            keyboardRows.add(row5);

            KeyboardRow row6 = new KeyboardRow();
            row6.add("  Проверка задолженности пользователя \uD83D\uDC6E  ");
            keyboardRows.add(row6);

            KeyboardRow row7 = new KeyboardRow();
            row7.add("  Пополнение баланса пользователя \uD83D\uDCB0  ");
            keyboardRows.add(row7);

            KeyboardRow row8 = new KeyboardRow();
            row8.add("  Список оставшихся продуктов \uD83E\uDDFE  ");
            keyboardRows.add(row8);
        }
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message " + e);
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
