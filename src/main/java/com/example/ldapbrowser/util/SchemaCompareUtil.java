package com.example.ldapbrowser.util;

import com.unboundid.ldap.sdk.schema.*;

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
 * Utilities to canonicalize LDAP schema elements for stable comparison across servers.
 * This aims to emulate UnboundID CompareLDAPSchemas normalization without copying code.
 */
public final class SchemaCompareUtil {
  private SchemaCompareUtil() {}

  public static String canonical(ObjectClassDefinition d) {
    if (d == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("OC{");
    appendOIDAndNames(sb, d.getOID(), d.getNames());
    sb.append(";type=").append(d.getObjectClassType() != null ? d.getObjectClassType().getName().toLowerCase(Locale.ROOT) : "");
    sb.append(";obsolete=").append(d.isObsolete());
    appendList(sb, "sup", toLowerSorted(d.getSuperiorClasses()));
    appendList(sb, "must", toLowerSorted(d.getRequiredAttributes()));
    appendList(sb, "may", toLowerSorted(d.getOptionalAttributes()));
    appendExtensions(sb, d.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  public static String canonical(AttributeTypeDefinition d) {
    if (d == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("AT{");
    appendOIDAndNames(sb, d.getOID(), d.getNames());
    sb.append(";obsolete=").append(d.isObsolete());
    if (d.getSuperiorType() != null) sb.append(";sup=").append(d.getSuperiorType().toLowerCase(Locale.ROOT));
    if (d.getEqualityMatchingRule() != null) sb.append(";eq=").append(d.getEqualityMatchingRule().toLowerCase(Locale.ROOT));
    if (d.getOrderingMatchingRule() != null) sb.append(";ord=").append(d.getOrderingMatchingRule().toLowerCase(Locale.ROOT));
    if (d.getSubstringMatchingRule() != null) sb.append(";sub=").append(d.getSubstringMatchingRule().toLowerCase(Locale.ROOT));
    if (d.getSyntaxOID() != null) sb.append(";syntax=").append(d.getSyntaxOID());
    sb.append(";single=").append(d.isSingleValued());
    sb.append(";collective=").append(d.isCollective());
    sb.append(";nousermod=").append(d.isNoUserModification());
    if (d.getUsage() != null) sb.append(";usage=").append(d.getUsage().name().toLowerCase(Locale.ROOT));
    appendExtensions(sb, d.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  public static String canonical(MatchingRuleDefinition d) {
    if (d == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("MR{");
    appendOIDAndNames(sb, d.getOID(), d.getNames());
    if (d.getSyntaxOID() != null) sb.append(";syntax=").append(d.getSyntaxOID());
    sb.append(";obsolete=").append(d.isObsolete());
    appendExtensions(sb, d.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  public static String canonical(MatchingRuleUseDefinition d) {
    if (d == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("MRU{");
    appendOIDAndNames(sb, d.getOID(), d.getNames());
    appendList(sb, "applies", toLowerSorted(d.getApplicableAttributeTypes()));
    sb.append(";obsolete=").append(d.isObsolete());
    appendExtensions(sb, d.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  public static String canonical(AttributeSyntaxDefinition d) {
    if (d == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("SYN{");
    sb.append("oid=").append(d.getOID());
    if (d.getDescription() != null) sb.append(";desc=").append(d.getDescription().trim().toLowerCase(Locale.ROOT));
    appendExtensions(sb, d.getExtensions());
    sb.append('}');
    return sb.toString();
  }

  private static void appendOIDAndNames(StringBuilder sb, String oid, String[] names) {
    if (oid != null) sb.append("oid=").append(oid);
    List<String> list = names != null ? Arrays.stream(names).filter(s -> s != null && !s.isBlank()).map(s -> s.toLowerCase(Locale.ROOT)).sorted().collect(Collectors.toList()) : Collections.emptyList();
    appendList(sb, "names", list);
  }

  private static void appendList(StringBuilder sb, String key, List<String> list) {
    sb.append(';').append(key).append('=').append('[');
    boolean first = true;
    for (String s : list) {
      if (!first) sb.append(',');
      sb.append(s);
      first = false;
    }
    sb.append(']');
  }

  private static List<String> toLowerSorted(String[] arr) {
    if (arr == null) return Collections.emptyList();
    List<String> list = new ArrayList<>();
    for (String s : arr) {
      if (s != null && !s.isBlank()) list.add(s.toLowerCase(Locale.ROOT));
    }
    list.sort(Comparator.naturalOrder());
    return list;
  }

  private static void appendExtensions(StringBuilder sb, Map<String, String[]> ext) {
    if (ext == null || ext.isEmpty()) return;
    // Normalize extension keys/values: lowercase keys, sort keys and values
    TreeMap<String, List<String>> norm = new TreeMap<>();
    for (Map.Entry<String, String[]> e : ext.entrySet()) {
      String k = e.getKey() != null ? e.getKey().toLowerCase(Locale.ROOT) : "";
      List<String> vals = e.getValue() != null ? Arrays.stream(e.getValue())
          .filter(v -> v != null && !v.isBlank())
          .map(v -> v.toLowerCase(Locale.ROOT))
          .sorted()
          .collect(Collectors.toList()) : Collections.emptyList();
      norm.put(k, vals);
    }
    for (Map.Entry<String, List<String>> e : norm.entrySet()) {
      appendList(sb, e.getKey(), e.getValue());
    }
  }
}
