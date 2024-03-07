package org.knapp_team_bot.service;

import org.springframework.core.SpringVersion;

public class Petition {
    private int id;
    private String name;
    private String petition;
    private String username;
    private long  chatId;

    public Petition(String name, String petition, String username,long chatId){
        this.name = name;
        this.petition = petition;
        this.username = username;
        this.chatId = chatId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPetition() {
        return petition;
    }

    public void setPetition(String petition) {
        this.petition = petition;
    }
}
