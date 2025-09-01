package com.ldapweb.ldapbrowser.util;

import com.unboundid.ldap.sdk.schema.AttributeSyntaxDefinition;
import com.unboundid.ldap.sdk.schema.AttributeTypeDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleDefinition;
import com.unboundid.ldap.sdk.schema.MatchingRuleUseDefinition;
import com.unboundid.ldap.sdk.schema.ObjectClassDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Utilities to canonicalize LDAP schema elements for stable comparison across
 * servers.
 * This aims to emulate UnboundID CompareLDAPSchemas normalization without
 * copying code.
 */
public final class SchemaCompareUtil {

  /**
   * Private constructor to prevent instantiation.
   */
  private SchemaCompareUtil() {
  }

  /**
   * Generates a canonical string representation of an ObjectClassDefinition.
   *
   * @param definition the ObjectClassDefinition to canonicalize
   * @return the canonical string representation
   */
  public static String canonical(ObjectClassDefinition definition) {
    if (definition == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("OC{");
    appendOidAndNames(sb, definition.getOID(), definition.getNames());
    sb.append(";type=")
        .append(definition.getObjectClassType() != null
            ? definition.getObjectClassType().getName().toLowerCase(Locale.ROOT)
            : "");
    sb.append(";obsolete=").append(definition.isObsolete());
    appendList(sb, "sup", toLowerSorted(definition.getSuperiorClasses()));
    appendList(sb, "must", toLowerSorted(definition.getRequiredAttributes()));
    appendList(sb, "may", toLowerSorted(definition.getOptionalAttributes()));
    appendExtensions(sb, definition.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  /**
   * Generates a canonical string representation of an AttributeTypeDefinition.
   *
   * @param definition the AttributeTypeDefinition to canonicalize
   * @return the canonical string representation
   */
  public static String canonical(AttributeTypeDefinition definition) {
    if (definition == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("AT{");
    appendOidAndNames(sb, definition.getOID(), definition.getNames());
    sb.append(";obsolete=").append(definition.isObsolete());
    if (definition.getSuperiorType() != null) {
      sb.append(";sup=").append(definition.getSuperiorType().toLowerCase(Locale.ROOT));
    }
    if (definition.getEqualityMatchingRule() != null) {
      sb.append(";eq=").append(definition.getEqualityMatchingRule().toLowerCase(Locale.ROOT));
    }
    if (definition.getOrderingMatchingRule() != null) {
      sb.append(";ord=").append(definition.getOrderingMatchingRule().toLowerCase(Locale.ROOT));
    }
    if (definition.getSubstringMatchingRule() != null) {
      sb.append(";sub=").append(definition.getSubstringMatchingRule().toLowerCase(Locale.ROOT));
    }
    if (definition.getSyntaxOID() != null) {
      sb.append(";syntax=").append(definition.getSyntaxOID());
    }
    sb.append(";single=").append(definition.isSingleValued());
    sb.append(";collective=").append(definition.isCollective());
    sb.append(";nousermod=").append(definition.isNoUserModification());
    if (definition.getUsage() != null) {
      sb.append(";usage=").append(definition.getUsage().name().toLowerCase(Locale.ROOT));
    }
    appendExtensions(sb, definition.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  /**
   * Generates a canonical string representation of a MatchingRuleDefinition.
   *
   * @param definition the MatchingRuleDefinition to canonicalize
   * @return the canonical string representation
   */
  public static String canonical(MatchingRuleDefinition definition) {
    if (definition == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("MR{");
    appendOidAndNames(sb, definition.getOID(), definition.getNames());
    if (definition.getSyntaxOID() != null) {
      sb.append(";syntax=").append(definition.getSyntaxOID());
    }
    sb.append(";obsolete=").append(definition.isObsolete());
    appendExtensions(sb, definition.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  /**
   * Generates a canonical string representation of a MatchingRuleUseDefinition.
   *
   * @param definition the MatchingRuleUseDefinition to canonicalize
   * @return the canonical string representation
   */
  public static String canonical(MatchingRuleUseDefinition definition) {
    if (definition == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("MRU{");
    appendOidAndNames(sb, definition.getOID(), definition.getNames());
    appendList(sb, "applies", toLowerSorted(definition.getApplicableAttributeTypes()));
    sb.append(";obsolete=").append(definition.isObsolete());
    appendExtensions(sb, definition.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  /**
   * Generates a canonical string representation of an AttributeSyntaxDefinition.
   *
   * @param definition the AttributeSyntaxDefinition to canonicalize
   * @return the canonical string representation
   */
  public static String canonical(AttributeSyntaxDefinition definition) {
    if (definition == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("SYN{");
    sb.append("oid=").append(definition.getOID());
    if (definition.getDescription() != null) {
      sb.append(";desc=")
          .append(definition.getDescription().trim().toLowerCase(Locale.ROOT));
    }
    appendExtensions(sb, definition.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  /**
   * Appends the OID and names to the StringBuilder.
   *
   * @param sb    the StringBuilder to append to
   * @param oid   the OID to append
   * @param names the names to append
   */
  private static void appendOidAndNames(StringBuilder sb, String oid, String[] names) {
    if (oid != null) {
      sb.append("oid=").append(oid);
    }
    List<String> list = names != null
        ? Arrays.stream(names)
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.toLowerCase(Locale.ROOT))
            .sorted()
            .collect(Collectors.toList())
        : Collections.emptyList();
    appendList(sb, "names", list);
  }

  /**
   * Appends a list of strings to the StringBuilder with a given key.
   *
   * @param sb   the StringBuilder to append to
   * @param key  the key for the list
   * @param list the list of strings to append
   */
  private static void appendList(StringBuilder sb, String key, List<String> list) {
    sb.append(';').append(key).append('=').append('[');
    boolean first = true;
    for (String s : list) {
      if (!first) {
        sb.append(',');
      }
      sb.append(s);
      first = false;
    }
    sb.append(']');
  }

  /**
   * Converts an array of strings to a sorted list of lowercase strings.
   *
   * @param array the array of strings to convert
   * @return the sorted list of lowercase strings
   */
  private static List<String> toLowerSorted(String[] array) {
    if (array == null) {
      return Collections.emptyList();
    }
    List<String> list = new ArrayList<>();
    for (String s : array) {
      if (s != null && !s.isBlank()) {
        list.add(s.toLowerCase(Locale.ROOT));
      }
    }
    list.sort(Comparator.naturalOrder());
    return list;
  }

  /**
   * Appends extensions to the StringBuilder.
   *
   * @param sb         the StringBuilder to append to
   * @param extensions the extensions to append
   */
  private static void appendExtensions(StringBuilder sb, Map<String, String[]> extensions) {
    if (extensions == null || extensions.isEmpty()) {
      return;
    }
    TreeMap<String, List<String>> normalizedExtensions = new TreeMap<>();
    for (Map.Entry<String, String[]> entry : extensions.entrySet()) {
      String key = entry.getKey() != null ? entry.getKey().toLowerCase(Locale.ROOT) : "";
      List<String> values = entry.getValue() != null
          ? Arrays.stream(entry.getValue())
              .filter(value -> value != null && !value.isBlank())
              .map(value -> value.toLowerCase(Locale.ROOT))
              .sorted()
              .collect(Collectors.toList())
          : Collections.emptyList();
      normalizedExtensions.put(key, values);
    }
    for (Map.Entry<String, List<String>> entry : normalizedExtensions.entrySet()) {
      appendList(sb, entry.getKey(), entry.getValue());
    }
  }
}