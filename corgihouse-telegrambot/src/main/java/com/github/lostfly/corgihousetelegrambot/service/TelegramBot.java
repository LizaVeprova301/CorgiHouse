package com.github.lostfly.corgihousetelegrambot.service;

import com.github.lostfly.corgihousetelegrambot.config.BotConfig;
import com.github.lostfly.corgihousetelegrambot.keyboardMenus.KeyboardMenus;
import com.github.lostfly.corgihousetelegrambot.listMenus.ListMenus;
import com.github.lostfly.corgihousetelegrambot.model.User;
import com.github.lostfly.corgihousetelegrambot.repository.PetRepository;
import com.github.lostfly.corgihousetelegrambot.repository.SessionRepository;
import com.github.lostfly.corgihousetelegrambot.repository.UserRepository;
import com.github.lostfly.corgihousetelegrambot.service.generalFuncs.FileService;
import com.github.lostfly.corgihousetelegrambot.service.generalFuncs.GeneralFuncs;
import com.github.lostfly.corgihousetelegrambot.service.sessionService.SessionService;
import com.github.lostfly.corgihousetelegrambot.service.modelsConnectedFuncs.*;
import com.github.lostfly.corgihousetelegrambot.service.regFuncs.MeetingRegistration;
import com.github.lostfly.corgihousetelegrambot.service.regFuncs.PetRegistration;
import com.github.lostfly.corgihousetelegrambot.service.regFuncs.UserRegistration;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.lostfly.corgihousetelegrambot.constants.funcsConstants.MeetingFuncsConstants.SELECT_NAME_FIELD_MEETING_EDIT_TEXT;
import static com.github.lostfly.corgihousetelegrambot.constants.keyboardsConstants.CommandListConstants.*;
import static com.github.lostfly.corgihousetelegrambot.constants.GlobalConstants.*;
import static com.github.lostfly.corgihousetelegrambot.constants.keyboardsConstants.KeyboardMenusConstants.*;
import static com.github.lostfly.corgihousetelegrambot.constants.keyboardsConstants.ListMenusConstants.*;
import static com.github.lostfly.corgihousetelegrambot.constants.regConstants.PetRegConstants.CANCEL_OPERATION;
import static com.github.lostfly.corgihousetelegrambot.constants.funcsConstants.UserFuncsConstants.EDIT_CHOISE;
import static com.github.lostfly.corgihousetelegrambot.constants.regConstants.PetRegConstants.REGISTER_PET_PHOTO;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRegistration userRegistration;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PetRegistration petRegistration;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private GeneralFuncs generalFuncs;

    @Autowired
    private UserFuncs userFuncs;
    @Autowired
    private PetsFuncs petsFuncs;

    @Autowired
    private FileService fileService;

    @Autowired
    private KeyboardMenus keyboardMenus;
    @Autowired
    private ListMenus listMenus;

    @Autowired
    private MeetingFuncs meetingFuncs;

    @Autowired
    private MeetingRegistration meetingRegistration;
    @Autowired
    private SearchMeetings searchMeetings;

    final BotConfig config;

    public TelegramBot(BotConfig config) {

        super(config.getBotDefaultOptions());

        this.config = config;

        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand(START_MENU_COMMAND, START_MENU_COMMAND_TEXT));
        listofCommands.add(new BotCommand(HELP_MENU_COMMAND, HELP_MENU_COMMAND_TEXT));
        listofCommands.add(new BotCommand(REGISTER_MENU_COMMAND, REGISTER_MENU_COMMAND_TEXT));

        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot command list " + e.getMessage());
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

    @Override
    public void onUpdateReceived(Update update) {
        long chatId = 0;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        }

        sessionService.CreateSession(chatId);


        if (update.hasCallbackQuery()) {

            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callBackData = update.getCallbackQuery().getData();

            switch (callBackData) {
                case DELETE_PROFILE_QUESTION:
                    sendEditMessage(chatId, messageId, userFuncs.deleteProfileQuestion(), listMenus.profileDeleteKeyboard());
                    break;
                case DELETE_PROFILE_CONFIRM:
                    sendEditMessage(chatId, messageId, userFuncs.deleteProfile(chatId));
                    break;
                case DELETE_PROFILE_DENY:
                    sendEditMessage(chatId, messageId, userFuncs.showProfile(chatId), listMenus.profileButtonKeyboard());
                    break;
                case EDIT_PROFILE:
                    sendEditMessage(chatId, messageId, EDIT_CHOISE, listMenus.profileEditKeyboard());
                    break;
                case EDIT_PROFILE_NAME:
                    sendEditMessage(chatId, messageId, userFuncs.editProfile(chatId, EDIT_PROFILE_NAME));
                    break;
                case EDIT_PROFILE_LASTNAME:
                    sendEditMessage(chatId, messageId, userFuncs.editProfile(chatId, EDIT_PROFILE_LASTNAME));
                    break;
                case EDIT_PROFILE_PHONE_NUMBER:
                    sendEditMessage(chatId, messageId, userFuncs.editProfile(chatId, EDIT_PROFILE_PHONE_NUMBER));
                    break;
                case PET_QUESTION_CONFIRM, PET_ADD:
                    sendEditMessage(chatId, messageId, petRegistration.initializeRegistration(update));
                    break;
                case PET_DELETE_SELECTION:
                    sendMessage(chatId, petsFuncs.deleteAnimalSelection(chatId));
                    break;
                case PET_QUESTION_DENY:
                    sendEditMessage(chatId, messageId, CANCEL_OPERATION);
                    break;
                case MEETING_ADD:
                    sendEditMessage(chatId, messageId, meetingRegistration.initializeRegistration(update));
                    break;
                case CREATED_MEETINGS_FULL_INFO_SELECT:
                    sendEditMessage(chatId, messageId, meetingFuncs.fullInfoMeetingSelection(chatId));
                    break;
                case APPLIED_MEETINGS_FULL_INFO_SELECT:
                    sendEditMessage(chatId, messageId, meetingFuncs.fullInfoMeetingSelection(chatId));
                    break;
                case MEETING_DELETE_BUTTON_SELECT:
                    sendEditMessage(chatId, messageId, meetingFuncs.deleteMeetingSelection(chatId));
                    break;
                case MEETING_EDIT_BUTTON_SELECT:
                    sendMessage(chatId, meetingFuncs.editMeetingSelectionNumber(chatId));
                    break;
                case EDIT_MEETING_BUTTON_TITLE:
                    sendEditMessage(chatId, messageId, meetingFuncs.editMeeting(chatId, EDIT_MEETING_BUTTON_TITLE));
                    break;
                case EDIT_MEETING_BUTTON_PLACE:
                    sendEditMessage(chatId, messageId, meetingFuncs.editMeeting(chatId, EDIT_MEETING_BUTTON_PLACE));
                    break;
                case EDIT_MEETING_BUTTON_ANIMALTYPE:
                    sendEditMessage(chatId, messageId, meetingFuncs.editMeeting(chatId, EDIT_MEETING_BUTTON_ANIMALTYPE));
                    break;
                case EDIT_MEETING_BUTTON_BREED:
                    sendEditMessage(chatId, messageId, meetingFuncs.editMeeting(chatId, EDIT_MEETING_BUTTON_BREED));
                    break;
                case EDIT_MEETING_BUTTON_DATE:
                    sendEditMessage(chatId, messageId, meetingFuncs.editMeeting(chatId, EDIT_MEETING_BUTTON_DATE));
                    break;
                case EDIT_MEETING_BUTTON_DESCRIPTION:
                    sendEditMessage(chatId, messageId, meetingFuncs.editMeeting(chatId, EDIT_MEETING_BUTTON_DESCRIPTION));
                    break;
                case REGISTRATION:
                    sendMessage(chatId, userRegistration.initializeRegistration(update));
                    break;
                default:
                    sendMessage(chatId, INDEV_TEXT, keyboardMenus.mainKeyboard());
                    break;

            }
        }


        if (update.hasMessage() && Objects.equals(sessionRepository.findByChatId(chatId).getGlobalFunctionContext(), GLOBAL_CONTEXT_DEFAULT)) {
            String messageText = update.getMessage().getText();

            switch (messageText) {
                case ADMIN_SEND_BROADCAST:
                    if( adminsIdList.contains(chatId) ) {
                        sessionRepository.setGlobalContextByChatId(GLOBAL_ADMIN_BROADCAST, chatId);
                        sendMessage(chatId, "Введите текст объявления: ");
                    }else {
                        sendMessage(chatId, "ВЫ ЛОХ ПОЗОРНЫЙ");
                    }
                    break;
                case START_MENU_COMMAND:
                    sendMessage(chatId, generalFuncs.startCommandReceived(chatId, update.getMessage().getChat().getFirstName()), keyboardMenus.mainKeyboard());
                    break;
                case HELP_MENU_COMMAND:
                    sendMessage(chatId, HELP_TEXT, keyboardMenus.mainKeyboard());
                    break;
                case REGISTER_MENU_COMMAND:
                    sendMessage(chatId, userRegistration.initializeRegistration(update));
                    break;
                case PETS:
                    sendMessage(chatId, petsFuncs.showPets(chatId));
                    break;
                case MY_MEETINGS:
                    sendMessage(chatId, meetingFuncs.changeToMyMeetings(chatId));
                    sendMessage(chatId, meetingFuncs.showMyMeetings(chatId));
                    break;
                case MY_MEETINGS_CREATED:
                    sendMessage(chatId, meetingFuncs.showCreatedMeetings(chatId));
                    break;
                case MY_MEETINGS_APPLIED:
                    sendMessage(chatId, meetingFuncs.showAppliedMeetings(chatId));
                    break;
                case SEARCH_MEETINGS:
                    sendMessage(chatId, searchMeetings.searchMeetings(chatId));
                    try {
                        sendPhoto(chatId, searchMeetings.showRandomPet());
                    } catch (TelegramApiException | IOException e) {
                        log.error(ERROR_OCCURRED + e.getMessage());
                    }
                    break;
                case BACK:
                    sendMessage(chatId, meetingFuncs.changeToMainMenu(chatId));
                    break;
                case PROFILE:
                    sendMessage(chatId, userFuncs.checkExistingProfile(chatId));
                    sendMessage(chatId, userFuncs.showProfile(chatId), listMenus.profileButtonKeyboard());
                    break;
                default:
                    sendMessage(chatId, INDEV_TEXT, keyboardMenus.mainKeyboard());
                    break;
            }
        } else {
            if (update.hasMessage()) {
                if (update.getMessage().hasPhoto() && Objects.equals(sessionRepository.findByChatId(chatId).getPetRegisterFunctionContext(), REGISTER_PET_PHOTO)) {
                    downloadImage(update, petRepository.findTopByOrderByOwnerIdDesc(chatId));
                }
            }

            String messageText = update.getMessage().getText();
            switch (sessionRepository.findByChatId(chatId).getGlobalFunctionContext()) {
                case GLOBAL_ADMIN_BROADCAST -> adminBroadcastNews(messageText, chatId);
                case GLOBAL_CONTEXT_USER_REGISTRATION ->
                        sendMessage(chatId, userRegistration.continueRegistration(update));
                case GLOBAL_CONTEXT_PET_REGISTRATION ->
                        sendMessage(chatId, petRegistration.continueRegistration(update));
                case GLOBAL_CONTEXT_USER_EDIT -> sendMessage(chatId, userFuncs.editProfileAction(chatId, messageText));
                case GLOBAL_CONTEXT_PET_DELETE -> sendMessage(chatId, petsFuncs.deleteAnimal(chatId, messageText));
                case GLOBAL_CONTEXT_FULL_MEETING_INFO -> sendMessage(chatId, meetingFuncs.fullInfoMeetingByNumber(chatId, messageText));
                case GLOBAL_CONTEXT_MEETING_DELETE -> sendMessage(chatId,meetingFuncs.deleteMeeting(chatId,messageText));
                case GLOBAL_CONTEXT_MEETING_REGISTRATION -> sendMessage(chatId, meetingRegistration.continueRegistration(update));
                case GLOBAL_CONTEXT_MEETING_SELECT_NUMBER_MEETING -> sendMessage(chatId, meetingFuncs.editMeetingNameField(chatId,messageText), listMenus.meetingEditKeyboard());
                case GLOBAL_CONTEXT_SET_NAME_FIELD_MEETING -> sendMessage(chatId,meetingFuncs.editMeetingAction(chatId,messageText));
                default -> {
                    sessionRepository.setGlobalContextByChatId(GLOBAL_CONTEXT_DEFAULT, chatId);
                    log.error("No global context found -  " + update.getMessage().getText());
                    sendMessage(chatId, INDEV_TEXT, keyboardMenus.mainKeyboard());
                }
            }
        }

    }

    public void adminBroadcastNews(String msg, Long chatId){
        ArrayList<User> users = userRepository.findAll();
        for (User user : users){
            sendMessage(user.getChatId(), EmojiParser.parseToUnicode(":star:" + "Это сообщение является массовой рассылкой пользователям бота от его администраторов. Если вы получили его, значит произошли важные изменения, которые позволяют нам стать лучше." + ":relaxed:" +"Расскажем подробнее: " + msg + " Если у вас есть пожелания или замечания по работе бота - обращайтесь к администраторам" + ":woman_technologist:" + ":man_technologist:" + ": @eveprova & @lostfly"));
        }
        sessionRepository.setGlobalContextByChatId(GLOBAL_CONTEXT_DEFAULT, chatId);
    }

    public void downloadImage(Update update, Long pet_id) {

        long chatId = update.getMessage().getChatId();

        PhotoSize photo = update.getMessage().getPhoto().stream().reduce((first, second) -> second).orElse(null);

        if (photo != null) {
            try {

                GetFile getFileRequest = new GetFile();
                getFileRequest.setFileId(photo.getFileId());
                org.telegram.telegrambots.meta.api.objects.File file = execute(getFileRequest);
                fileService.downloadPhotoByFilePath(file.getFilePath(), PHOTO_STORAGE_DIR, chatId, pet_id, getBotToken());

            } catch (TelegramApiException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPhoto(Long chatId, File imageFile) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(imageFile));
        sendPhoto.setCaption("КОРЖИКА ВАМ В ЛЕНТУ!");
        execute(sendPhoto);
    }

    void sendMessage(long chatId, String textToSend, ReplyKeyboardMarkup msgKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setReplyMarkup(msgKeyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }

    void sendMessage(long chatId, String textToSend, InlineKeyboardMarkup msgKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setReplyMarkup(msgKeyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }

    void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }

    void sendMessage(long chatId, SendMessage message) {
        if (message == null) {
            return;
        }

        message.setChatId(String.valueOf(chatId));

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }


    void sendEditMessage(long chatId, long messageId, String textToSend, InlineKeyboardMarkup msgKeyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId((int) messageId);
        message.setText(textToSend);
        message.setReplyMarkup(msgKeyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }

    void sendEditMessage(long chatId, long messageId, String textToSend) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId((int) messageId);
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_OCCURRED + e.getMessage());
        }
    }


}
