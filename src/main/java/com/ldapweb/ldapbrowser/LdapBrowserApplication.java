package com.ldapweb.ldapbrowser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for LDAP Browser.
 * This class serves as the entry point for the Spring Boot application.
 */
@SpringBootApplication
public class LdapBrowserApplication {

  /**
   * The main method to start the Spring Boot application.
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(LdapBrowserApplication.class, args);
  }
}