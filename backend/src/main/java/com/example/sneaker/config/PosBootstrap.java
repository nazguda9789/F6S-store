package com.example.sneaker.config;

import com.example.sneaker.model.PosOperator;
import com.example.sneaker.repository.PosOperatorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PosBootstrap implements CommandLineRunner {
  private final PosOperatorRepository operators;
  private final String username;

  public PosBootstrap(
      PosOperatorRepository operators, @Value("${app.admin-username}") String username) {
    this.operators = operators;
    this.username = username;
  }

  public void run(String... args) {
    if (!operators.existsById(username)) {
      var operator = new PosOperator();
      operator.setUsername(username);
      operators.save(operator);
    }
  }
}
