package com.example.paspaysweets.service;

import com.example.paspaysweets.config.BotConfig;
import com.example.paspaysweets.model.*;
import com.example.paspaysweets.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
public class TelegramBot extends TelegramLongPollingBot {

    private final String NOT_REGISTERED = "Для взаимодействия с ботом пожалуйста зарегистрируйтесь(введите команду /register или в меню нажмите на эту же кнопку). Напоминаю, регистрацию могут пройти только сотрудники Paspay работающие из офиса";
    private final String INFO_TEXT = "Я бот для облегчения процесса взаимодействия с магазином вкусняшек paspay. После регистрации вам доступна кнопка СПИСОК ПРОДУКТОВ, по нажатии которой вам будет выведен соответствующий актуальный список продуктов. Вы можете выбрать нужный вам продукт нажатием кнопки ПРИОБРЕСТИ" + " Стоимость продукта будет зачислена в ваш долг либо списана с вашего кошелька. Для пополнения баланса обратитесь Светлане. Каждую пятницу вам будет приходить сообщение с суммой, которую нужно уплатить в бугалтерию. ";
    private final String DAILY_LINK_MEET = "\n" + "https://meet.google.com/xmf-axvh-sgb?hs=224";

    private final String MONDAY_PLANNING_MEET_LINK = "\n" + "https://meet.google.com/nwj-gbwm-jiv";

    private final String INFO_TEXT_ADMIN = "Я бот для облегчения процесса взаимодействия с магазином вкусняшек paspay. После регистрации вам доступна кнопка СПИСОК ПРОДУКТОВ." + " По нажатии которой вам будет выведен соответствующий актуальный список продуктов. Вы можете выбрать нужный вам продукт нажатием кнопки ПРИОБРЕСТИ\"" + "Стоимость продукта будет зачислена в ваш долг либо списана с вашего кошелька. Для пополнения баланса обратитесь Светлане. Каждую пятницу вам будет приходить сообщение с суммой, которую нужно уплатить в бугалтерию. " + "\n➡\uFE0F Описание кнопок, которые доступны только админам: \n\uD83D\uDFE2 список продуктов  --  выводит список продуктов для покупки" + "\n\uD83D\uDFE2 добавить продукт  --  после нажатия кнопки боту нужно отправить файл в формате excel. Пожалуйста заполняйте только в определенном формате. За инструкцией обратитесь к Дмитрию" + "\n\uD83D\uDFE2 отправить сообщение всем  --  после нажатия кнопки бот попросит отправить ему нужное сообщение для рассылки." + "\n\uD83D\uDFE2 удалить пользователя  --  после нажатия кнопки нужно ввести имя пользователя, userName из телеграм. Получить имя можно например кнопкой ПРОВЕРКА БАЛАНСА ПОЛЬЗОВАТЕЛЕЙ." + "\n\uD83D\uDFE2  сброс всех продуктов  --  Производится сброс всех продуктов из базы данных. ВНИМАНИЕ! продукты и данные о них будут удалены безвозвратно! Для подтверждения действия нужно будет ввести на клавиатуре слово \"да\"" + "\n\uD83D\uDFE2 проверка долгов пользователей  --  в ответ отдает таблицу всех пользователей с их задолженностями" + "\n\uD83D\uDFE2 проверка баланса пользователей  --  отдает таблицу с балансом всех пользователей(кошелек, если кто-то например положил деньги заранее)" + "\n\uD83D\uDFE2 пополнение баланса пользователя  --  в ответ боту нужно ввести сообщение в виде chatId&сумма пополнения(84857584&400). Таблицу с chatId будет у вас в распечатанном виде, так же можно посмотреть его с помощью кнопки БАЛАНС ПОЛЬЗОВАТЕЛЕЙ" + "\n\uD83D\uDFE2 список продуктов  --  выдает список продуктов, которые есть в магазине в формате название-цена-количество";

    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final BotConfig config;
    private final UserNamesRepo userNamesRepo;
    private final CategoryRepo categoryRepo;
    private final TransactionsRepo transactionsRepo;
    private final DailyMeetRepo dailyMeetRepo;

    public TelegramBot(ProductRepo productRepo, UserRepo userRepo, BotConfig config, UserNamesRepo userNamesRepo, CategoryRepo categoryRepo, TransactionsRepo transactionsRepo, DailyMeetRepo dailyMeetRepo) {
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.config = config;
        this.userNamesRepo = userNamesRepo;
        this.categoryRepo = categoryRepo;
        this.transactionsRepo = transactionsRepo;
        this.dailyMeetRepo = dailyMeetRepo;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "начало"));
        listofCommands.add(new BotCommand("/info", "информация о боте"));
        listofCommands.add(new BotCommand("/register", "РЕГИСТРАЦИЯ"));
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
            } else {
                switch (botState) {
                    case WAITING_FOR_USER_NAME:
                        // Если бот ожидает имени пользователя для удаления, передаем полученное имя для обработки
                        removeUser(chatId, messageText);
                        break;
                    case WAITING_FOR_CONFIRMATION:
                        // Бот ожидает подтверждения перед удалением всех продуктов
                        dropData(update);
                        break;
                    case WAITING_FOR_USER_INFO:
                        putCash(update);
                        botState = BotState.IDLE; // Добавлено явное переключение состояния в IDLE
                        break;
                    case WAITING_FOR_MESSAGE:
                        // Бот ожидает подтверждения перед удалением всех продуктов
                        sendMessageToAll(update);
                        break;
                    case WAITING_FOR_NAME_FOR_ADD:
                        //Бот ожидает имя пользователя
                        addUserToPaspayUserTable(chatId, messageText);
                    case WAITING_FOR_CHAT_ID_FOR_ADD_TO_DAILY_MEET:
                        //Бот ожидает chatID пользователя
                        addUserToDailyMeetExecute(chatId, messageText);
                    case WAITING_FOR_CHAT_ID_FOR_DELETE_FROM_DAILY_MEET:
                        //Бот ожидает chatID пользователя
                        deleteUserToDailyMeetExecute(chatId, messageText);
                    case IDLE:
                        // Если бот находится в простое, обрабатываем команды и запросы пользователя
                        switch (messageText) {
                            case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                            case "/info" -> {
                                String responseText = isChatIdBotOwner(config.getBotOwners(), chatId) ? INFO_TEXT_ADMIN : INFO_TEXT;
                                sendMessage(chatId, responseText);
                                log.info(chatId + " requested info");
                            }
                            case "Список продуктов \uD83D\uDCDD" -> sendProductCategories(chatId);
                            case "Мой баланс и задолженность \uD83D\uDEE1\uFE0F" -> sendUserDutyAndBalance(chatId);
                            case "История моих покупок \uD83D\uDCDC" -> sendUserTransactionHistory(chatId);
                            case "Отправить сообщение всем \uD83D\uDCE9" -> sendMessageToAll(update);
                            case "Добавить продукт ➕" -> requestProductInfo(chatId);
                            case "Список оставшихся продуктов \uD83E\uDDFE" -> sendProductList(chatId);
                            case "Сброс всех продуктов ❌" -> dropData(update);
                            case "Проверка долгов пользователей \uD83D\uDC6E" -> userDuty(update);
                            case "Проверка баланса пользователей \uD83D\uDCB2" -> userBalance(update);
                            case "Пополнение баланса пользователя \uD83D\uDCB0" -> putCash(update);
                            case "Получить отчет по покупкам \uD83D\uDCC8" -> getReport(chatId);
                            case "Добавить нового пользователя \uD83D\uDFE2" -> addNewUser(chatId);
                            case "Удалить пользователя ⛔" -> requestUserNameToDelete(chatId);
                            case "Добавить юзера на daily meet \uD83D\uDCC5" -> addUserToDailyMeet(chatId);
                            case "Удалить юзера из daily meet \uD83D\uDDD1\uFE0F" -> deleteUserFromDailyMeet(chatId);
                            case "список имя/chatId \uD83E\uDDFE" -> getAllUsersChatId(chatId);
                            default ->
                                    sendMessage(chatId, messageText.equals("/register") ? "Вы уже зарегистрированы" : "");
                        }
                        break;
                }
            }
        }
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            processExcelFile(update);
        }
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();
            String callbackData = callbackQuery.getData();
            var messageId = callbackQuery.getMessage().getMessageId();
            var callbackQueryId = callbackQuery.getId();

            if (callbackData.startsWith("purchase")) {
                handlePurchaseCallback(chatId, callbackData);
                removeInlineKeyboard(chatId, messageId); // Удаляем клавиатуру после обработки
                answerCallbackQuery(callbackQueryId); // Отправляем ответ на колбэк-запрос
            } else if (callbackData.startsWith("view_category")) {
                handleCategoryCallback(chatId, callbackData);
                //removeInlineKeyboard(chatId, messageId); // Удаляем клавиатуру после обработки
                answerCallbackQuery(callbackQueryId); // Отправляем ответ на колбэк-запрос
            } else if (callbackData.startsWith("confirm_purchase") || callbackData.startsWith("cancel_purchase")) {
                handleConfirmationCallback(chatId, callbackData, messageId);
                removeInlineKeyboard(chatId, messageId); // Удаляем клавиатуру после обработки
                answerCallbackQuery(callbackQueryId); // Отправляем ответ на колбэк-запрос
            }
        }

    }

    private void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error occurred while answering callback query: " + e);
        }
    }

    private void getAllUsersChatId(Long chatId) {
        var userEntity = userRepo.findAll();
        var finalList = new HashMap<Long, String>();
        for (ShopUser shopUser : userEntity) {
            var userName = shopUser.getFio();
            var userChatId = shopUser.getChatId();
            finalList.put(userChatId, userName);
        }
        StringBuilder messageBuilder = new StringBuilder();
        for (Map.Entry<Long, String> entry : finalList.entrySet()) {
            messageBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        sendMessage(chatId, messageBuilder.toString());
    }

    private void addUserToDailyMeet(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите chatId пользователя, которого вы хотите добавить:");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message: " + e);
        }
        botState = BotState.WAITING_FOR_CHAT_ID_FOR_ADD_TO_DAILY_MEET;
    }

    private void deleteUserFromDailyMeet(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите chatId пользователя, которого вы хотите удалить:");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message: " + e);
        }
        botState = BotState.WAITING_FOR_CHAT_ID_FOR_DELETE_FROM_DAILY_MEET;
    }

    private void addUserToDailyMeetExecute(Long chatId, String chatIdForAdd) {
        if (!chatIdForAdd.matches("\\d+")) {
            sendMessage(chatId, "Ошибка: идентификатор чата должен содержать только цифры.");
            botState = BotState.IDLE;
            return;
        }
        var entiry = dailyMeetRepo.findByChatId(Long.parseLong(chatIdForAdd));
        if (entiry.isPresent()) {
            sendMessage(chatId, "Пользователь уже получает ссылку на мит");
        } else {
            DailyMeet dailyMeet = new DailyMeet();
            dailyMeet.setChatId(Long.parseLong(chatIdForAdd));
            dailyMeetRepo.save(dailyMeet);
            sendMessage(chatId, "Пользователь успешно добавлен на рассылку на daily meet");
            sendMessage(Long.parseLong(chatIdForAdd), "Вы добавлены на рассылку на daily meet");
        }
        botState = BotState.IDLE;
    }

    private void deleteUserToDailyMeetExecute(Long chatId, String chatIdForDelete) {
        if (!chatIdForDelete.matches("\\d+")) {
            sendMessage(chatId, "Ошибка: идентификатор чата должен содержать только цифры.");
            botState = BotState.IDLE;
            return;
        }
        var entiry = dailyMeetRepo.findByChatId(Long.parseLong(chatIdForDelete));
        if (entiry.isEmpty()) {
            sendMessage(chatId, "Пользователь не получает рассылку");
        } else {
            dailyMeetRepo.deleteByChatId(Long.parseLong(chatIdForDelete));
            sendMessage(chatId, "Пользователь успешно удален из рассылки на daily meet");
            sendMessage(Long.parseLong(chatIdForDelete), "Вы удалены из рассылки на daily meet");
        }
        botState = BotState.IDLE;
    }

    private void sendUserTransactionHistory(Long chatId) {
        var userEntity = userRepo.findByChatId(chatId);
        String userFio = null;
        if (userEntity.isPresent()) {
            userFio = userEntity.get().getFio();
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var transactionsEntity = transactionsRepo.findAllByName(userFio);
        var firstTransactionEntity = transactionsRepo.findFirstByOrderByIdAsc();
        if (firstTransactionEntity.isEmpty()) {
            sendMessage(chatId, "У вас пока нет покупок");
            return;
        }
        if (transactionsEntity.isEmpty()) {
            sendMessage(chatId, "у вас нет покупок за период с " + firstTransactionEntity.get().getRegisteredAt().toLocalDate().format(dateFormatter));
            return;
        }

        StringBuilder transactionsList = new StringBuilder("Список продуктов, которые вы приобрели за период с  " + firstTransactionEntity.get().getRegisteredAt().toLocalDate().format(dateFormatter) + " до " + LocalDate.now() + "\n\n");
        for (Transactions transaction : transactionsEntity) {
            String transactionInfo = "Название: " + transaction.getProductName() + "\n" + "Цена: " + transaction.getPrice() + "\n" + "Дата покупки: " + transaction.getRegisteredAt() + "\n\n";
            transactionsList.append(transactionInfo);
        }
        sendMessage(chatId, transactionsList.toString());
        log.info("Отправлен список транзакций пользователю: " + chatId);
    }

    private void removeInlineKeyboard(Long chatId, Integer messageId) {
        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(chatId.toString());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(null); // Устанавливаем клавиатуру в null, чтобы удалить её

        try {
            execute(editMarkup);
        } catch (TelegramApiException e) {
            log.error("Error occurred while removing inline keyboard: " + e);
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

    public boolean isChatHr(List<Long> botHr, long chatId) {
        for (Long hr : botHr) {
            if (hr == chatId) {
                return true;
            }
        }
        return false;
    }

    public boolean isChatAdmin(List<Long> botAdmin, long chatId) {
        for (Long admin : botAdmin) {
            if (admin == chatId) {
                return true;
            }
        }
        return false;
    }

    public void sendUserDutyAndBalance(Long chatId) {
        var userEntity = userRepo.findByChatId(chatId).get();
        sendMessage(chatId, "Ваш баланс составляет - " + userEntity.getCash() + ".\n" + "Ваш долг составляет - " + userEntity.getDuty());
    }

    public void getReport(long chatId) {
        List<Transactions> transactions = transactionsRepo.findAll();
        StringBuilder content = new StringBuilder();
        for (Transactions transaction : transactions) {
            var stringTrans = transaction.getName() + " " + transaction.getProductName() + " " + transaction.getPrice() + " " + transaction.getRegisteredAt();
            content.append(stringTrans).append("\n");
        }
        java.io.File file = createFile(content.toString());
        sendDocumentToTelegram(chatId, file);

    }

    private java.io.File createFile(String content) {
        java.io.File file = new java.io.File("report.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException e) {
            sendMessage(config.getBotOwners().get(0), "не удалось записать транзакции в файл");
        }
        return file;
    }

    private void sendDocumentToTelegram(Long chatId, java.io.File file) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(file));

        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            sendMessage(config.getBotOwners().get(0), "не отправить файл с репортом в телеграм" + chatId);
        }
    }

    @Transactional(rollbackFor = IllegalArgumentException.class)
    protected void processExcelFile(Update update) {
        List<Product> products = new ArrayList<>();
        Message message = update.getMessage();
        Document document = message.getDocument(); // Получаем информацию о документе
        if (document != null) {
            GetFile getFile = new GetFile();
            getFile.setFileId(document.getFileId());
            try {
                File file = execute(getFile);
                InputStream inputStream = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath()).openStream();
                Workbook workbook = WorkbookFactory.create(inputStream);
                Sheet sheet = workbook.getSheetAt(0); // Предполагается, что информация о продуктах находится в первом листе
                for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                    Row row = sheet.getRow(i);

                    // Обработка каждой строки в файле Excel
                    Cell productNameCell = row.getCell(1); // Столбец с именем продукта
                    if (productNameCell == null || productNameCell.getCellType() == CellType.BLANK) {
                        break;
                    }
                    Cell priceCell = row.getCell(0); // Столбец с ценой
                    Cell quantityCell = row.getCell(2); // Столбец с количеством
                    Cell productTypeCell = row.getCell(3); // Столбец с категорией товара

                    try {
                        String productName = productNameCell.getStringCellValue();
                        long price = (long) priceCell.getNumericCellValue();
                        long quantity = (long) quantityCell.getNumericCellValue();
                        long productType = (long) productTypeCell.getNumericCellValue();
                        Product product = new Product();
                        product.setProductName(productName);
                        product.setPrice((long) Math.ceil(price + price * 8 / 100.0));  //Автоматическая надбавка 8% к стоимости и округление к большему
                        product.setQuantity(quantity);
                        ProductCategory category = categoryRepo.findById(productType).orElse(null);
                        product.setCategory(category);
                        products.add(product);
                    } catch (RuntimeException e) {
                        for (int j = 0; j < config.getBotOwners().size(); j++) {
                            sendMessage(config.getBotOwners()
                                    .get(j), "при добавлении продуктов в excel файле введены не коррректные данные. Пожалуйста, произведите сброс базы данных и повторите попытку.");
                        }
                        log.error("Некорректные данные в excel файле при добавлении продуктов от пользователя - " + update.getMessage()
                                .getChatId());
                        throw new IllegalArgumentException("Некорректные данные в excel файле при добавлении продуктов");
                    }

                }
                productRepo.saveAll(products);
            } catch (IOException | TelegramApiException e) {
                log.error("Error occurred while processing Excel file: " + e);
            }
            sendMessage(update.getMessage().getChatId(), "Продукты, находящиеся в файле успешно добавлены");
        }
    }

    public void sendMessageToAll(Update update) {
        long chatId = update.getMessage().getChatId();

        switch (botState) {
            case IDLE:
                sendMessage(chatId, "Пожалуйста, напишите сообщение, которое вы хотите отправить всем пользователям.");
                botState = BotState.WAITING_FOR_MESSAGE;
                break;
            case WAITING_FOR_MESSAGE:
                String messageText = update.getMessage().getText();
                if (!messageText.isEmpty()) {
                    List<ShopUser> allUsers = userRepo.findAll();
                    for (ShopUser user : allUsers) {
                        long userChatId = user.getChatId();
                        sendMessage(userChatId, messageText);
                    }
                    sendMessage(chatId, "Сообщение успешно отправлено всем пользователям.");
                    botState = BotState.IDLE;
                } else {
                    sendMessage(chatId, "Вы не ввели текст сообщения. Пожалуйста, введите текст для отправки всем пользователям.");
                }
                break;
            default:
                sendMessage(chatId, "Пожалуйста, дождитесь завершения предыдущей операции.");
                break;
        }
    }

    @Transactional
    public void putCash(Update update) {
        long chatId = update.getMessage().getChatId();

        switch (botState) {
            case IDLE:
                sendMessage(chatId, "Пожалуйста, введите информацию о пользователе в формате chatId&сумма пополнения");
                botState = BotState.WAITING_FOR_USER_INFO;
                break;
            case WAITING_FOR_USER_INFO:
                String[] commandParts = update.getMessage().getText().split("&");
                if (commandParts.length == 2) {
                    try {
                        String targetUserChatId = commandParts[0].trim();
                        String targetSum = commandParts[1].trim();
                        Optional<ShopUser> userOptional = userRepo.findByChatId(Long.valueOf(targetUserChatId));
                        if (userOptional.isEmpty()) {
                            sendMessage(chatId, "Пользователь с указанным chatId не найден. Пожалуйста, убедитесь, что введен корректный chatId.");
                            botState = BotState.IDLE;
                            return;
                        }
                        ShopUser user = userOptional.get();
                        long sum = user.getCash() + Long.parseLong(targetSum) - user.getDuty();
                        if (user.getDuty() == Long.parseLong(targetSum)) {
                            user.setDuty(0L);
                            userRepo.save(user);
                            sendMessage(chatId, "Задолженность пользователя с chatId " + targetUserChatId + " успешно погашена.");
                            sendMessage(Long.parseLong(targetUserChatId), "Ваша задолженность успешно погашена.");
                            sendMessage(config.getBotOwners().get(0), "Баланс юзера " + targetUserChatId + "пополнен на " + targetSum);
                            botState = BotState.IDLE;
                            return;
                        }
                        if (sum >= 0 && user.getDuty() != 0) {
                            user.setCash(sum);
                            user.setDuty(0L);
                            userRepo.save(user);
                            sendMessage(chatId, "Баланс пользователя с chatId " + targetUserChatId + " пополнен, а также успешно погашен его долг.");
                            sendMessage(Long.parseLong(targetUserChatId), "Ваш баланс успешно пополнен, а также погашен долг.");
                            sendMessage(config.getBotOwners().get(0), "Баланс юзера " + targetUserChatId + "пополнен на " + targetSum);
                            botState = BotState.IDLE;
                            return;
                        } else if (sum >= 0 && user.getDuty() == 0) {
                            user.setCash(sum);
                            userRepo.save(user);
                            sendMessage(chatId, "Баланс пользователя с chatId " + targetUserChatId + " успешно пополнен.");
                            sendMessage(Long.parseLong(targetUserChatId), "Ваш баланс успешно пополнен и составляет " + sum + ".");
                            sendMessage(config.getBotOwners().get(0), "Баланс юзера " + targetUserChatId + "пополнен на " + targetSum);
                            botState = BotState.IDLE;
                            return;
                        } else {
                            user.setCash(0L);
                            user.setDuty(-(sum));
                            userRepo.save(user);
                            sendMessage(chatId, "Баланс пользователя с chatId " + targetUserChatId + " пополнен, но только частично из-за его долга.");
                            sendMessage(Long.parseLong(targetUserChatId), "Ваш баланс успешно пополнен, но только частично из-за вашего долга. Ваш текущий долг: " + (-(sum)));
                            sendMessage(config.getBotOwners().get(0), "Баланс юзера " + targetUserChatId + "пополнен на " + targetSum);
                            botState = BotState.IDLE;
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Ошибка при обработке суммы пополнения. Пожалуйста, убедитесь, что введена корректная сумма.");
                    } catch (Exception e) {
                        sendMessage(chatId, "Произошла ошибка при пополнении баланса пользователя. Пожалуйста, попробуйте еще раз.");
                        log.error("Error occurred while processing cash refill by user " + chatId, e);
                    }
                } else {
                    sendMessage(chatId, "Неверный формат ввода. Пожалуйста, введите информацию о пользователе в формате chatId|сумма пополнения");
                }
                break;
            default:
                sendMessage(chatId, "Пожалуйста, дождитесь завершения предыдущей операции.");
                break;
        }
    }

    private void userDuty(Update update) {
        List<ShopUser> users = userRepo.findAll();

        if (!users.isEmpty()) {
            StringBuilder dutyInfo = new StringBuilder("Долги пользователей:\n");

            for (ShopUser user : users) {
                if (user.getDuty() != 0) {
                    dutyInfo.append(user.getFio())
                            .append("   ")
                            .append(user.getDuty())
                            .append("ТГ")
                            .append("   ")
                            .append(user.getChatId())
                            .append("\n");
                }
            }

            sendMessage(update.getMessage().getChatId(), dutyInfo.toString());
        } else {
            sendMessage(update.getMessage().getChatId(), "Нет данных о пользователях.");
        }
    }

    private void userBalance(Update update) {
        List<ShopUser> users = userRepo.findAll();

        if (!users.isEmpty()) {
            StringBuilder dutyInfo = new StringBuilder("Баланс пользователей:\n");

            for (ShopUser user : users) {
                dutyInfo.append(user.getFio())
                        .append("   ")
                        .append(user.getCash())
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

    private void requestUserNameToDelete(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите имя пользователя, которого вы хотите удалить:");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message: " + e);
        }
        botState = BotState.WAITING_FOR_USER_NAME;
    }

    private void addNewUser(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите имя пользователя, которого вы хотите добавить:");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message: " + e);
        }
        botState = BotState.WAITING_FOR_NAME_FOR_ADD;
    }
    @Transactional
    protected void removeUser(long chatId, String userNameToDelete) {
        var userExists = userRepo.findByUserName(userNameToDelete);
        var paspayUserName = userNamesRepo.findByUserNameOffice(userNameToDelete);
        if (userExists.isPresent()) { //TODO тут доделать
            try {
                userRepo.deleteByUserName(userNameToDelete);
                sendMessage(chatId, "Баланс пользователя = "  + userExists.get().getCash() + ".  Задолженность пользователя = " + userExists.get().getDuty());
                sendMessage(chatId, "Пользователь " + userNameToDelete + " успешно удален из базы данных по вкусняшкам.");
                sendMessage(config.getBotOwners().get(0), "Пользователь " + userNameToDelete + " успешно удален из базы данных по вкусняшкам");
                sendMessage(config.getBotOwners().get(2), "Пользователь " + userNameToDelete + " успешно удален из базы данных по вкусняшкам");
                log.info("User removed from the table by user " + chatId);
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка при удалении пользователя. Пожалуйста, попробуйте ещё раз.");
                log.error("Error occurred while removing user by user " + chatId, e);
                return; // Выйдем из метода, чтобы не переключить состояние бота на IDLE
            }
        }
        if (paspayUserName.isPresent()) {
            userNamesRepo.deleteByUserNameOffice(userNameToDelete);
            sendMessage(chatId, "Пользователь " + userNameToDelete + " успешно удален из базы данных для регистрации в боте.");
            sendMessage(config.getBotOwners().get(0), "Пользователь " + userNameToDelete + " успешно удален из базы данных для регистрации в боте");
            sendMessage(config.getBotOwners().get(2), "Пользователь " + userNameToDelete + " успешно удален из базы данных для регистрации в боте");
        } else {
            sendMessage(chatId, "Пользователь с именем " + userNameToDelete + " не найден в базе данных.");
        }

        // Возвращаем состояние бота в IDLE после успешного удаления пользователя или если пользователя не существует
        botState = BotState.IDLE;
    }

    private void addUserToPaspayUserTable(long chatId, String userNameToAdd) {
        var paspayUserName = userNamesRepo.findByUserNameOffice(userNameToAdd);
        if (paspayUserName.isEmpty()) {
            try {
                PaspayUserNames paspayUserNames = new PaspayUserNames();
                paspayUserNames.setUserNameOffice(userNameToAdd);
                userNamesRepo.save(paspayUserNames);
                sendMessage(chatId, "Пользователь " + userNameToAdd + " успешно добавлен в базу данных.");
                sendMessage(config.getBotOwners().get(0), "Пользователь " + userNameToAdd + " успешно добавлен в БД и может регистрироваться");
                sendMessage(config.getBotOwners().get(2), "Пользователь " + userNameToAdd + " успешно добавлен в БД и может регистрироваться");
                log.info("User added to the table by user " + chatId);
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка при добавлении пользователя. Пожалуйста, попробуйте ещё раз.");
                log.error("Error occurred while adding user by user " + chatId, e);
                return; // Выйдем из метода, чтобы не переключить состояние бота на IDLE
            }
        } else {
            sendMessage(chatId, "Пользователь с именем " + userNameToAdd + " уже есть в базе данных!");
        }

        // Возвращаем состояние бота в IDLE после успешного удаления пользователя или если пользователя не существует
        botState = BotState.IDLE;
    }

    public void dropData(Update update) {
        // Проверяем текущее состояние бота
        switch (botState) {
            case WAITING_FOR_CONFIRMATION:
                // Пользователь отправил подтверждение
                String messageText = update.getMessage().getText();
                if (messageText.equalsIgnoreCase("да") || messageText.equalsIgnoreCase("Да")) {
                    try {
                        // Удаление всех продуктов из базы данных
                        productRepo.deleteAll();
                        transactionsRepo.deleteAll();
                        long chatId = update.getMessage().getChatId();
                        sendMessage(chatId, "Все продукты а так же старые записи о покупках успешно удалены из таблицы ✅");
                        log.info("All data dropped from the table by user " + chatId);
                    } catch (Exception e) {
                        long chatId = update.getMessage().getChatId();
                        sendMessage(chatId, "Ошибка при удалении данных. Пожалуйста, попробуйте ещё раз.");
                        log.error("Error occurred while dropping data by user " + chatId, e);
                    }
                } else {
                    // Если пользователь ввел что-то отличное от "да", возвращаем бота в прежнее состояние
                    botState = BotState.IDLE;
                }
                break;
            case IDLE:
                // Бот находится в простое, отправляем запрос на подтверждение
                sendMessage(update.getMessage()
                        .getChatId(), "Если действительно хотите удалить все продукты из базы, пожалуйста, напишите слово \"да\" в ответ. \nЕсли нажатие было случайным то просто проигнорируйте");
                // Переключаем состояние бота
                botState = BotState.WAITING_FOR_CONFIRMATION;
                break;
            default:
                // В случае других состояний просто игнорируем запрос на сброс данных
                sendMessage(update.getMessage()
                        .getChatId(), "Пожалуйста, завершите текущую операцию, прежде чем выполнять сброс данных.");
                break;
        }
    }

    public void sendProductList(Long chatId) {
        List<Product> allProducts = productRepo.findAll();
        StringBuilder productListMessage = new StringBuilder("Список продуктов, которые есть в магазине:\n");

        for (Product product : allProducts) {
            String productInfo = "Название: " + product.getProductName() + "\n" + "Цена: " + product.getPrice() + "\n" + "Количество: " + product.getQuantity() + "\n\n";
            productListMessage.append(productInfo);
        }

        sendMessage(chatId, productListMessage.toString());
        log.info("Отправлен список продуктов пользователю: " + chatId);
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

    @Scheduled(cron = "0 0 16 * * FRI,TUE", zone = "GMT+05:00")
    public void sendFridayDutyMessage() {
        var users = userRepo.findAll();
        for (ShopUser user : users) {
            var userDuty = user.getDuty();
            if (userDuty == 0) {
                continue;
            }
            if (LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY) {
                sendMessage(user.getChatId(), "Привет! Ваш долг за вкусняшки на сегодня составляет: " + userDuty + " Тг \uD83D\uDCB2" + ". Пожалуйста, занесите денюжку в бухгалтерию. Хороших выходных! \uD83D\uDE09");
            } else {
                sendMessage(user.getChatId(), "Привет! Ваш долг за вкусняшки на сегодня составляет: " + userDuty + " Тг \uD83D\uDCB2" + ". Пожалуйста, занесите денюжку в бухгалтерию \uD83D\uDE09");
            }
        }
    }

    @Scheduled(cron = "0 25 9 * * TUE-FRI", zone = "GMT+05:00")
    public void sendMeetingLink() {
        var dailyMeetEntity = dailyMeetRepo.findAll();
        for (DailyMeet meet : dailyMeetEntity) {
            sendMessage(meet.getChatId(), DAILY_LINK_MEET + "\n" + "Доброе утро! Подключаемся к ежедневному миту в 09:30.");
        }
    }

    @Scheduled(cron = "0 55 9 * * MON", zone = "GMT+05:00")
    public void sendPlanningMeetingLink() {
        var dailyMeetEntity = dailyMeetRepo.findAll();
        for (DailyMeet meet : dailyMeetEntity) {
            sendMessage(meet.getChatId(), MONDAY_PLANNING_MEET_LINK + "\n" + "Доброе утро! Подключаемся к миту по планированию в 10:00.");
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

    private Map<Long, Boolean> buttonStateMap = new ConcurrentHashMap<>();

    public void handleConfirmationCallback(Long chatId, String callbackData, int messageId) {
        String[] parts = callbackData.split("_");
        Boolean isPressed = buttonStateMap.getOrDefault(chatId, false);
        if (!isPressed) {
            if (parts.length == 3 && parts[0].equals("confirm") && StringUtils.isNumeric(parts[2])) {
                buttonStateMap.put(chatId, true);
                handleConfirmPurchase(chatId, callbackData, messageId);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Задержка в 1 секунду
                        buttonStateMap.remove(chatId);
                        tryDeleteMessage(chatId, messageId); // Удаление сообщения
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                return;
            } else if (parts.length == 3 && parts[0].equals("cancel") && StringUtils.isNumeric(parts[2])) {
                buttonStateMap.put(chatId, true);
                handleCancelPurchase(chatId, callbackData, messageId);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Задержка в 1 секунду
                        buttonStateMap.remove(chatId);
                        tryDeleteMessage(chatId, messageId); // Удаление сообщения
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                return;
            }
        }

        // Если кнопка уже была нажата или не выполнено ни одно из действий, отправляем сообщение об ошибке
        sendMessage(chatId, "Произошла непредвиденная ошибка либо вы случайно нажали на подтверждение покупки дважды");
        sendMessage(config.getBotOwners().get(0), "Не удалось обработать подтверждение покупки, ошибка." + chatId + buttonStateMap.toString());
    }

    private void tryDeleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            sendMessage(config.getBotOwners()
                    .get(0), "Ошибка удаления сообщения подтверждения покупки, юзер " + chatId);
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
                    user.setTotalSum(user.getTotalSum() + product.getPrice());
                    userRepo.save(user);
                    sendResponseAndDocument(user, product, chatId, messageId);
                } else {
                    user.setDuty((-(count)) + user.getDuty());
                    user.setCash(0L);
                    user.setTotalSum(user.getTotalSum() + product.getPrice());
                    userRepo.save(user);
                    sendResponseAndDocument(user, product, chatId, messageId);
                }
            } else {
                sendMessage(chatId, "К сожалению, товар закончился.");
                return;
            }
        } else {
            sendMessage(chatId, "Товар с указанным идентификатором не найден.");
        }
    }

    private void sendResponseAndDocument(ShopUser user, Product product, Long chatId, int messageId) {
        ZonedDateTime todayDateWithZone = ZonedDateTime.now(ZoneId.of("GMT+05:00"));
        LocalDateTime todayDate = todayDateWithZone.toLocalDateTime();
        sendMessage(chatId, "Вы успешно приобрели товар " + product.getProductName() + ". Спасибо за покупку!");
//        tryDeleteMessage(chatId, messageId);
        Transactions boughtProduct = new Transactions();
        boughtProduct.setName(user.getFio());
        boughtProduct.setPrice(product.getPrice());
        boughtProduct.setProductName(product.getProductName());
        boughtProduct.setRegisteredAt(todayDate);
        transactionsRepo.save(boughtProduct);
        String report = user.getFio() + " - " + product.getProductName() + " - " + product.getPrice() + " - " + todayDate.toString() + "\n";
        sendMessage(config.getBotOwners().get(0), report);
        sendMessage(config.getBotOwners().get(2), report);
    }

    private void handleCancelPurchase(Long chatId, String callbackData, int messageId) {
        Long productId = Long.parseLong(callbackData.split("_")[2]);
        sendMessage(chatId, "Вы отменили покупку товара ❌");
//        tryDeleteMessage(chatId, messageId);
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

    enum BotState {
        WAITING_FOR_USER_NAME, WAITING_FOR_CONFIRMATION, WAITING_FOR_USER_INFO, WAITING_FOR_MESSAGE, WAITING_FOR_NAME_FOR_ADD, WAITING_FOR_CHAT_ID_FOR_ADD_TO_DAILY_MEET, WAITING_FOR_CHAT_ID_FOR_DELETE_FROM_DAILY_MEET, IDLE
    }

    BotState botState = BotState.IDLE;

    // Метод для запроса информации о продукте
    private void requestProductInfo(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Пожалуйста отправьте мне файл со списком продуктов, которые вы желаете добавить в формате excel\n" + "Пожалуйста, используйте строгий шаблон для этой цели, который вам был предоставлен");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message: " + e);
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
            user.setTotalSum(0L);
            userRepo.save(user);

            String successMessage = "Вы успешно зарегистрированы. Теперь вы можете пользоваться функциями бота и кушоть вкусняшки \uD83C\uDF6B";
            sendMessage(chatId, successMessage);

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

    public void sendMessage(long chatId, String textToSend) {
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

        KeyboardRow row2 = new KeyboardRow();
        row2.add("  Мой баланс и задолженность \uD83D\uDEE1\uFE0F  ");
        keyboardRows.add(row2);

        KeyboardRow row14 = new KeyboardRow();
        row14.add("  История моих покупок \uD83D\uDCDC  ");
        keyboardRows.add(row14);

        if (isChatIdBotOwner(config.getBotOwners(), chatId)) {
            KeyboardRow row3 = new KeyboardRow();
            row3.add("  Добавить продукт ➕  ");
            keyboardRows.add(row3);

            KeyboardRow row4 = new KeyboardRow();
            row4.add("  Отправить сообщение всем \uD83D\uDCE9  ");
            keyboardRows.add(row4);

            KeyboardRow row6 = new KeyboardRow();
            row6.add("  Сброс всех продуктов ❌  ");
            keyboardRows.add(row6);

            KeyboardRow row7 = new KeyboardRow();
            row7.add("  Проверка долгов пользователей \uD83D\uDC6E  ");
            keyboardRows.add(row7);

            KeyboardRow row8 = new KeyboardRow();
            row8.add("  Проверка баланса пользователей \uD83D\uDCB2  ");
            keyboardRows.add(row8);

            KeyboardRow row9 = new KeyboardRow();
            row9.add("  Пополнение баланса пользователя \uD83D\uDCB0  ");
            keyboardRows.add(row9);

            KeyboardRow row10 = new KeyboardRow();
            row10.add("  Список оставшихся продуктов \uD83E\uDDFE  ");
            keyboardRows.add(row10);

            KeyboardRow row11 = new KeyboardRow();
            row11.add("  Получить отчет по покупкам \uD83D\uDCC8  ");
            keyboardRows.add(row11);
        }
        if (isChatHr(config.getBotHr(), chatId)) {
            KeyboardRow row12 = new KeyboardRow();
            row12.add("  Добавить нового пользователя \uD83D\uDFE2  ");
            keyboardRows.add(row12);

            KeyboardRow row13 = new KeyboardRow();
            row13.add("  Удалить пользователя ⛔  ");
            keyboardRows.add(row13);
        }
        if (isChatAdmin(config.getBotAdmin(), chatId)) {
            KeyboardRow row15 = new KeyboardRow();
            row15.add("  Добавить юзера на daily meet \uD83D\uDCC5  ");
            keyboardRows.add(row15);

            KeyboardRow row16 = new KeyboardRow();
            row16.add("  Удалить юзера из daily meet \uD83D\uDDD1\uFE0F  ");
            keyboardRows.add(row16);

            KeyboardRow row17 = new KeyboardRow();
            row17.add("  список имя/chatId \uD83E\uDDFE  ");
            keyboardRows.add(row17);
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
