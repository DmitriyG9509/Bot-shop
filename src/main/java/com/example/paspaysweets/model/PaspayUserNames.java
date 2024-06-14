package com.example.paspaysweets.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "paspayUserNames")
@NamedQuery(name = "PaspayUserNames.deleteByUserNameOffice", query = "DELETE FROM paspayUserNames u WHERE u.userNameOffice = :userName")
public class PaspayUserNames {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name_office")
    private String userNameOffice;
}
