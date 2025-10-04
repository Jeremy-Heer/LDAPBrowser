package com.ldapweb.ldapbrowser.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing Access Control Instructions (ACIs) to extract
 * structured information like description, targets, permissions, and bind rules.
 */
public class AciParser {

  // ACI structure: (target1)(target2)...(version 3.0; acl "description"; allow/deny (permissions) bind_rules;)
  private static final Pattern ACI_MAIN_PATTERN = Pattern.compile(
      "^(.*)\\(version\\s+3\\.0;\\s*acl\\s+\"([^\"]*)\";\\s*(allow|deny)\\s*\\(([^)]+)\\)\\s*([^;]*);\\)$",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );

  // Target patterns
  private static final Pattern TARGETATTR_PATTERN = Pattern.compile(
      "\\(targetattr\\s*=\\s*\"([^\"]+)\"\\)",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern TARGET_PATTERN = Pattern.compile(
      "\\(target\\s*=\\s*\"([^\"]+)\"\\)",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern TARGETSCOPE_PATTERN = Pattern.compile(
      "\\(targetscope\\s*=\\s*\"([^\"]+)\"\\)",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern TARGETFILTER_PATTERN = Pattern.compile(
      "\\(targetfilter\\s*=\\s*\"([^\"]+)\"\\)",
      Pattern.CASE_INSENSITIVE
  );

  // Bind rule patterns
  private static final Pattern USERDN_PATTERN = Pattern.compile(
      "userdn\\s*=\\s*\"([^\"]+)\"",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern GROUPDN_PATTERN = Pattern.compile(
      "groupdn\\s*=\\s*\"([^\"]+)\"",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern ROLEDN_PATTERN = Pattern.compile(
      "roledn\\s*=\\s*\"([^\"]+)\"",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern AUTHMETHOD_PATTERN = Pattern.compile(
      "authmethod\\s*=\\s*\"([^\"]+)\"",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern IP_PATTERN = Pattern.compile(
      "ip\\s*=\\s*\"([^\"]+)\"",
      Pattern.CASE_INSENSITIVE
  );

  private static final Pattern DNS_PATTERN = Pattern.compile(
      "dns\\s*=\\s*\"([^\"]+)\"",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Represents a parsed ACI with its components.
   */
  public static class ParsedAci {
    private final String description;
    private final String allowOrDeny;
    private final List<String> permissions;
    private final List<String> targets;
    private final List<String> bindRules;
    private final String rawAci;

    public ParsedAci(String description, String allowOrDeny, List<String> permissions,
                     List<String> targets, List<String> bindRules, String rawAci) {
      this.description = description != null ? description : "";
      this.allowOrDeny = allowOrDeny != null ? allowOrDeny.toLowerCase() : "unknown";
      this.permissions = new ArrayList<>(permissions != null ? permissions : new ArrayList<>());
      this.targets = new ArrayList<>(targets != null ? targets : new ArrayList<>());
      this.bindRules = new ArrayList<>(bindRules != null ? bindRules : new ArrayList<>());
      this.rawAci = rawAci != null ? rawAci : "";
    }

    public String getDescription() {
      return description;
    }

    public String getAllowOrDeny() {
      return allowOrDeny;
    }

    public List<String> getPermissions() {
      return new ArrayList<>(permissions);
    }

    public List<String> getTargets() {
      return new ArrayList<>(targets);
    }

    public List<String> getBindRules() {
      return new ArrayList<>(bindRules);
    }

    public String getRawAci() {
      return rawAci;
    }

    /**
     * Gets a formatted string of resources (targets).
     */
    public String getResourcesString() {
      if (targets.isEmpty()) {
        return "All";
      }
      return String.join(", ", targets);
    }

    /**
     * Gets a formatted string of rights (allow/deny + permissions).
     */
    public String getRightsString() {
      if (permissions.isEmpty()) {
        return allowOrDeny;
      }
      return allowOrDeny + " (" + String.join(", ", permissions) + ")";
    }

    /**
     * Gets a formatted string of clients (bind rules).
     */
    public String getClientsString() {
      if (bindRules.isEmpty()) {
        return "Any";
      }
      return String.join(", ", bindRules);
    }

    /**
     * Gets the name/title for the ACI (using description or fallback).
     */
    public String getName() {
      if (description != null && !description.trim().isEmpty()) {
        return description.trim();
      }
      return "Unnamed ACI";
    }
  }

  /**
   * Parses an ACI string and extracts its components.
   *
   * @param aciString the raw ACI string to parse
   * @return ParsedAci object containing the extracted components
   */
  public static ParsedAci parseAci(String aciString) {
    if (aciString == null || aciString.trim().isEmpty()) {
      return new ParsedAci("", "unknown", new ArrayList<>(), 
                          new ArrayList<>(), new ArrayList<>(), aciString);
    }

    String trimmedAci = aciString.trim();
    
    try {
      Matcher mainMatcher = ACI_MAIN_PATTERN.matcher(trimmedAci);
      if (!mainMatcher.matches()) {
        // If the ACI doesn't match the expected pattern, return basic info
        return new ParsedAci("Unparseable ACI", "unknown", new ArrayList<>(),
                            new ArrayList<>(), new ArrayList<>(), trimmedAci);
      }

      String targetsSection = mainMatcher.group(1);
      String description = mainMatcher.group(2);
      String allowOrDeny = mainMatcher.group(3);
      String permissionsSection = mainMatcher.group(4);
      String bindRulesSection = mainMatcher.group(5);

      // Parse permissions
      List<String> permissions = parsePermissions(permissionsSection);

      // Parse targets
      List<String> targets = parseTargets(targetsSection);

      // Parse bind rules
      List<String> bindRules = parseBindRules(bindRulesSection);

      return new ParsedAci(description, allowOrDeny, permissions, targets, bindRules, trimmedAci);

    } catch (Exception e) {
      // If parsing fails, return basic info with error indication
      return new ParsedAci("Parse Error: " + e.getMessage(), "unknown", 
                          new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), trimmedAci);
    }
  }

  /**
   * Parses the permissions section of an ACI.
   */
  private static List<String> parsePermissions(String permissionsSection) {
    List<String> permissions = new ArrayList<>();
    if (permissionsSection != null && !permissionsSection.trim().isEmpty()) {
      String[] parts = permissionsSection.split(",");
      for (String part : parts) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
          permissions.add(trimmed);
        }
      }
    }
    return permissions;
  }

  /**
   * Parses the targets section of an ACI.
   */
  private static List<String> parseTargets(String targetsSection) {
    List<String> targets = new ArrayList<>();
    
    if (targetsSection == null) {
      return targets;
    }

    // Parse targetattr
    Matcher targetattrMatcher = TARGETATTR_PATTERN.matcher(targetsSection);
    while (targetattrMatcher.find()) {
      targets.add("Attributes: " + targetattrMatcher.group(1));
    }

    // Parse target
    Matcher targetMatcher = TARGET_PATTERN.matcher(targetsSection);
    while (targetMatcher.find()) {
      targets.add("Target: " + targetMatcher.group(1));
    }

    // Parse targetscope
    Matcher targetscopeMatcher = TARGETSCOPE_PATTERN.matcher(targetsSection);
    while (targetscopeMatcher.find()) {
      targets.add("Scope: " + targetscopeMatcher.group(1));
    }

    // Parse targetfilter
    Matcher targetfilterMatcher = TARGETFILTER_PATTERN.matcher(targetsSection);
    while (targetfilterMatcher.find()) {
      targets.add("Filter: " + targetfilterMatcher.group(1));
    }

    return targets;
  }

  /**
   * Parses the bind rules section of an ACI.
   */
  private static List<String> parseBindRules(String bindRulesSection) {
    List<String> bindRules = new ArrayList<>();
    
    if (bindRulesSection == null) {
      return bindRules;
    }

    // Parse userdn
    Matcher userdnMatcher = USERDN_PATTERN.matcher(bindRulesSection);
    while (userdnMatcher.find()) {
      bindRules.add("User DN: " + userdnMatcher.group(1));
    }

    // Parse groupdn
    Matcher groupdnMatcher = GROUPDN_PATTERN.matcher(bindRulesSection);
    while (groupdnMatcher.find()) {
      bindRules.add("Group DN: " + groupdnMatcher.group(1));
    }

    // Parse roledn
    Matcher rolednMatcher = ROLEDN_PATTERN.matcher(bindRulesSection);
    while (rolednMatcher.find()) {
      bindRules.add("Role DN: " + rolednMatcher.group(1));
    }

    // Parse authmethod
    Matcher authmethodMatcher = AUTHMETHOD_PATTERN.matcher(bindRulesSection);
    while (authmethodMatcher.find()) {
      bindRules.add("Auth Method: " + authmethodMatcher.group(1));
    }

    // Parse ip
    Matcher ipMatcher = IP_PATTERN.matcher(bindRulesSection);
    while (ipMatcher.find()) {
      bindRules.add("IP: " + ipMatcher.group(1));
    }

    // Parse dns
    Matcher dnsMatcher = DNS_PATTERN.matcher(bindRulesSection);
    while (dnsMatcher.find()) {
      bindRules.add("DNS: " + dnsMatcher.group(1));
    }

    return bindRules;
  }
}