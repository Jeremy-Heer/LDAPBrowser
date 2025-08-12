package com.example.ldapbrowser.service;

import com.example.ldapbrowser.model.LdapServerConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Service for managing LDAP server configurations
 */
@Service
public class ConfigurationService {
    
    private static final String CONFIG_FILE = "ldap-servers.json";
    private final ObjectMapper objectMapper;
    private final Map<String, LdapServerConfig> configurations;
    
    public ConfigurationService() {
        this.objectMapper = new ObjectMapper();
        this.configurations = new LinkedHashMap<>();
        loadConfigurations();
    }
    
    /**
     * Get all server configurations
     */
    public List<LdapServerConfig> getAllConfigurations() {
        return new ArrayList<>(configurations.values());
    }
    
    /**
     * Get configuration by ID
     */
    public LdapServerConfig getConfiguration(String id) {
        return configurations.get(id);
    }
    
    /**
     * Save or update a server configuration
     */
    public void saveConfiguration(LdapServerConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        configurations.put(config.getId(), config);
        saveConfigurations();
    }
    
    /**
     * Delete a server configuration
     */
    public void deleteConfiguration(String id) {
        configurations.remove(id);
        saveConfigurations();
    }
    
    /**
     * Check if configuration exists
     */
    public boolean exists(String id) {
        return configurations.containsKey(id);
    }
    
    /**
     * Load configurations from file
     */
    private void loadConfigurations() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                List<LdapServerConfig> configs = objectMapper.readValue(
                    configFile, 
                    new TypeReference<List<LdapServerConfig>>() {}
                );
                
                for (LdapServerConfig config : configs) {
                    configurations.put(config.getId(), config);
                }
            } catch (IOException e) {
                // Initialize with default configurations if loading fails
                initializeDefaultConfigurations();
            }
        } else {
            // Initialize with default configurations
            initializeDefaultConfigurations();
        }
    }
    
    /**
     * Save configurations to file
     */
    private void saveConfigurations() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(CONFIG_FILE), new ArrayList<>(configurations.values()));
        } catch (IOException e) {
            // Log error if needed for debugging
        }
    }
    
    /**
     * Initialize with some default example configurations
     */
    private void initializeDefaultConfigurations() {
        // Example configuration for OpenLDAP
        LdapServerConfig openLdap = new LdapServerConfig(
            UUID.randomUUID().toString(),
            "Local OpenLDAP",
            "localhost",
            389,
            "cn=admin,dc=example,dc=com",
            ""
        );
        openLdap.setBaseDn("dc=example,dc=com");
        configurations.put(openLdap.getId(), openLdap);
        
        // Example configuration for Active Directory
        LdapServerConfig ad = new LdapServerConfig(
            UUID.randomUUID().toString(),
            "Active Directory",
            "domain.company.com",
            389,
            "user@domain.company.com",
            ""
        );
        ad.setBaseDn("dc=domain,dc=company,dc=com");
        configurations.put(ad.getId(), ad);
        
        // Example configuration for Apache Directory Server
        LdapServerConfig apacheDs = new LdapServerConfig(
            UUID.randomUUID().toString(),
            "Apache Directory Server",
            "localhost",
            10389,
            "uid=admin,ou=system",
            "secret"
        );
        apacheDs.setBaseDn("ou=system");
        configurations.put(apacheDs.getId(), apacheDs);
        
        saveConfigurations();
    }
}
