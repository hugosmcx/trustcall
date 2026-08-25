package br.com.hssoftware.trustcall;

public class PhoneUtils {

    public static String normalizar(String numero) {
        if (numero == null) return "";
        return numero.replaceAll("[^0-9]", "");
    }

    public static boolean correspondem(String numeroA, String numeroB) {
        if (numeroA == null || numeroB == null || numeroA.isEmpty() || numeroB.isEmpty()) {
            return false;
        }
        return numeroA.endsWith(numeroB) || numeroB.endsWith(numeroA);
    }

    public static boolean isInternacional(String numeroOriginal) {
        if (numeroOriginal == null) return false;
        String valor = numeroOriginal.trim();
        return valor.startsWith("+") && !valor.startsWith("+55");
    }
}
