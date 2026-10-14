package com.org.model;

public class FacePredictionResponse {

    private boolean recognized;
    private Integer userId;
    private String name;
    private Double confidence;
    private String message;

    public FacePredictionResponse() {
    }

    public FacePredictionResponse(
            boolean recognized,
            Integer userId,
            String name,
            Double confidence,
            String message) {
        this.recognized = recognized;
        this.userId = userId;
        this.name = name;
        this.confidence = confidence;
        this.message = message;
    }

    public boolean isRecognized() {
        return recognized;
    }

    public void setRecognized(boolean recognized) {
        this.recognized = recognized;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
