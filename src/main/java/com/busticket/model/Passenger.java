package com.busticket.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Passenger {

    private String fullName;
    private int age;
    private String gender;
    private String seatNumber;

    public Passenger() {}

    public Passenger(String fullName, int age, String gender, String seatNumber) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.seatNumber = seatNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}
