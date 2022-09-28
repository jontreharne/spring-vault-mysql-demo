package com.example.mysqldemo;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "greetings")
@Data
public class Greeting {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    Long id;

    @Column(name = "greeting")
    String greeting;
}
