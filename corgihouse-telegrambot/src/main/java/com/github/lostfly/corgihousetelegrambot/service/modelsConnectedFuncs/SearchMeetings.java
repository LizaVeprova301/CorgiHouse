package com.github.lostfly.corgihousetelegrambot.service.modelsConnectedFuncs;

import com.github.lostfly.corgihousetelegrambot.keyboardMenus.KeyboardMenus;
import com.github.lostfly.corgihousetelegrambot.listMenus.ListMenus;
import com.github.lostfly.corgihousetelegrambot.model.Meeting;
import com.github.lostfly.corgihousetelegrambot.model.Pet;
import com.github.lostfly.corgihousetelegrambot.repository.MeetingRepository;
import com.github.lostfly.corgihousetelegrambot.repository.PetRepository;
import com.github.lostfly.corgihousetelegrambot.repository.SessionRepository;
import com.github.lostfly.corgihousetelegrambot.repository.UserToMeetingRepository;
import com.github.lostfly.corgihousetelegrambot.service.generalFuncs.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.text.ParseException;
import java.util.List;
import java.util.Random;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import static com.github.lostfly.corgihousetelegrambot.constants.funcsConstants.ShowMeetingsConstants.NO_AT_ALL_MEETINGS_TEXT;

@Slf4j
@Component
public class SearchMeetings {
    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ListMenus listMenus;

    @Autowired
    private KeyboardMenus keyboardMenus;

    @Autowired
    private UserFuncs userFuncs;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserToMeetingRepository userToMeetingRepository;
    @Autowired
    private MeetingFuncs meetingFuncs;
    @Autowired
    private FileService fileService;
    @Autowired
    private PetRepository petRepository;

    public String showAllMeetingInfo(Meeting meeting){
        String fullFilledStatus = meeting.getFullFilled() ? "Да" : "Нет";
        String completedStatus = meeting.getCompleted() ? "Да" : "Нет";
        String created_meeting_item = "ID события: " + meeting.getMeetingId() + "\n\n" +
                "Название: " + meeting.getTitle() + "\n\n" +
                "Дата: " + meeting.getEventDate() + "\n\n" +
                "Тип животного: " + meeting.getAnimalType() + "\n\n" +
                "Порода: " + meeting.getBreed() + "\n\n" +
                "Описание: " + meeting.getDescription() + "\n\n" +
                "Место проведения: " + meeting.getPlace() + "\n\n" +
                "Максимальное кол-во участников: " + meeting.getUserLimit() + "\n\n" +
                "Заполнено: " + fullFilledStatus + "\n\n" +
                "Прошло: " + completedStatus + "\n\n";
        return created_meeting_item;
    }

    private <T> T getRandomElement(List<T> list) {
        Collections.shuffle(list);
        return list.get(0);
    }

    private boolean shouldReturnNull() {
        Random random = new Random();
        return random.nextInt(10) == 0;
    }

    public File showRandomPet() throws IOException {
        if (!shouldReturnNull()) {
            return null;
        }

        Random random = new Random();

        File petPhoto = fileService.giveCorgiPhotoByFilePath((long) random.nextInt(10));

        return petPhoto;
    }

    public SendMessage searchMeetings(long chatId) {

        if (userFuncs.checkExistingProfile(chatId) != null){
            return userFuncs.checkExistingProfile(chatId);
        }

        ArrayList<Meeting> all_meetings_created = meetingRepository.findAllByOwnerIdNot(chatId);


        SendMessage message = new SendMessage();
        if (all_meetings_created.isEmpty()) {
            message.setText(NO_AT_ALL_MEETINGS_TEXT);
            message.setReplyMarkup(listMenus.meetingKeyboard());
        }else{

            StringBuilder created_meetings_list = new StringBuilder();

            for(Meeting meeting : all_meetings_created){
                created_meetings_list.append(showAllMeetingInfo(meeting));
            }

            message.setText(created_meetings_list.toString());
            message.setChatId(chatId);
            message.setReplyMarkup(listMenus.searchMeetingKeyboard());

        }
        return message;

    }

}
