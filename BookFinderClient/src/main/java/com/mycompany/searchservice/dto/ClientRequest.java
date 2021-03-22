package com.mycompany.searchservice.dto;

import java.io.Serializable;
import java.util.Map;

public class ClientRequest implements Serializable {

    private final static long serialVersionUID = 1L;
    private Map<String, String> phrases;

    public ClientRequest(Map<String, String> phrases) {
        this.phrases = phrases;
    }

    public Map<String, String> getPhrases() {
        return phrases;
    }

    public void setPhrases(Map<String, String> phrases) {
        this.phrases = phrases;
    }
}
