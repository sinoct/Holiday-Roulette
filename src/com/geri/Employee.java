package com.geri;

public class Employee {
    private String name;
    private String lastVacation;
    private String secondLasVacation;

    public Employee(String name, String lastVacation, String secondLasVacation) {
        this.name = name;
        this.lastVacation = lastVacation;
        this.secondLasVacation = secondLasVacation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastVacation() {
        return lastVacation;
    }

    public void setLastVacation(String lastVacation) {
        this.lastVacation = lastVacation;
    }

    public String getSecondLasVacation() {
        return secondLasVacation;
    }

    public void setSecondLasVacation(String secondLasVacation) {
        this.secondLasVacation = secondLasVacation;
    }
}
