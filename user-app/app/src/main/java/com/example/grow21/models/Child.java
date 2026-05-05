package com.example.grow21.models;

public class Child {
    private int id;
    private String name;
    private int age;
    private int avatar;

    public Child() {
    }

    public Child(int id, String name, int age, int avatar) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.avatar = avatar;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getAvatar() {
        return avatar;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }
}
