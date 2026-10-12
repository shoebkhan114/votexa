package com.votexa.duplicate;

import java.time.LocalDate;

/** Pure-Java similarity scoring (rule-based + Jaro-Winkler). Decision support only. */
public final class Similarity {
    private Similarity() {}

    public static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9 ]", "").trim().replaceAll("\\s+", " ");
    }

    public static double jaro(String s1, String s2) {
        if (s1.equals(s2)) return s1.isEmpty() ? 0 : 1.0;
        int l1 = s1.length(), l2 = s2.length();
        if (l1 == 0 || l2 == 0) return 0;
        int range = Math.max(0, Math.max(l1, l2) / 2 - 1);
        boolean[] m1 = new boolean[l1], m2 = new boolean[l2];
        int matches = 0;
        for (int i = 0; i < l1; i++) {
            int lo = Math.max(0, i - range), hi = Math.min(l2 - 1, i + range);
            for (int j = lo; j <= hi; j++) {
                if (!m2[j] && s1.charAt(i) == s2.charAt(j)) { m1[i] = true; m2[j] = true; matches++; break; }
            }
        }
        if (matches == 0) return 0;
        int t = 0, k = 0;
        for (int i = 0; i < l1; i++) {
            if (m1[i]) {
                while (!m2[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) t++;
                k++;
            }
        }
        double m = matches;
        return (m / l1 + m / l2 + (m - t / 2.0) / m) / 3.0;
    }

    public static double jaroWinkler(String a, String b) {
        double j = jaro(a, b);
        int p = 0, max = Math.min(4, Math.min(a.length(), b.length()));
        while (p < max && a.charAt(p) == b.charAt(p)) p++;
        return j + p * 0.1 * (1 - j);
    }

    public static double nameScore(String a, String b) { return jaroWinkler(norm(a), norm(b)); }

    public static double dobScore(LocalDate a, LocalDate b) {
        if (a == null || b == null) return 0;
        if (a.equals(b)) return 1.0;
        return a.getYear() == b.getYear() ? 0.3 : 0.0;
    }

    public static double addressScore(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return 0;
        return jaroWinkler(norm(a), norm(b));
    }

    /** Weighted score 0..100: name 50%, date of birth 30%, address 20%. */
    public static double score(String n1, LocalDate d1, String a1, String n2, LocalDate d2, String a2) {
        double s = 0.5 * nameScore(n1, n2) + 0.3 * dobScore(d1, d2) + 0.2 * addressScore(a1, a2);
        return Math.round(s * 10000.0) / 100.0;
    }

    public static String matched(String n1, LocalDate d1, String a1, String n2, LocalDate d2, String a2) {
        StringBuilder sb = new StringBuilder();
        if (nameScore(n1, n2) >= 0.9) sb.append("NAME,");
        if (dobScore(d1, d2) >= 1.0) sb.append("DOB,");
        if (addressScore(a1, a2) >= 0.9) sb.append("ADDRESS,");
        return sb.length() == 0 ? "PARTIAL" : sb.substring(0, sb.length() - 1);
    }
}
