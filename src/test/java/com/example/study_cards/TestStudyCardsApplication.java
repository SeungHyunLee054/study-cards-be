package com.example.study_cards;

import org.springframework.boot.SpringApplication;

public class TestStudyCardsApplication {

    public static void main(String[] args) {
        SpringApplication.from(StudyCardsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
