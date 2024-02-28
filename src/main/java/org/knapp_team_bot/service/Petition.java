package org.knapp_team_bot.service;

public class Petition {
    private int id;
    private String name;
    private String petition;

    public Petition(String name, String petition){
        this.name = name;
        this.petition = petition;
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
