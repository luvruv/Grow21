package com.example.grow21.models;

import java.util.List;

public class QuestionModel {
    private String id;
    private String type;
    private String category;
    private String question;
    private String image;
    private List<String> options;
    private String answer;
    private String audio;

    public QuestionModel() {
    }

    public QuestionModel(String id, String type, String category, String question,
                         String image, List<String> options, String answer, String audio) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.question = question;
        this.image = image;
        this.options = options;
        this.answer = answer;
        this.audio = audio;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    // Motor game fields
    private String subtype;
    private String pathType;
    private String shape;

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    // Puzzle game fields
    private List<String> colors;  // Color sort: bin colors
    private List<String> items;   // Color sort: "emoji:color" entries
    private List<String> pairs;   // Memory flip: image names to pair

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public List<String> getPairs() {
        return pairs;
    }

    public void setPairs(List<String> pairs) {
        this.pairs = pairs;
    }
}
