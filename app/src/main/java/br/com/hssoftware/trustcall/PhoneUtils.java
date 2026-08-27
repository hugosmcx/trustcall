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

    /**
     * Extrai o DDD (2 dígitos) de um número brasileiro em qualquer formato comum
     * (com/sem +55, com/sem 0 de prefixo). Retorna null se não der pra determinar com segurança.
     */
    public static String extrairDDD(String numeroOriginal) {
        if (numeroOriginal == null) return null;
        String digitos = normalizar(numeroOriginal);
        if (digitos.isEmpty()) return null;

        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            digitos = digitos.substring(2);
        } else if (digitos.startsWith("0") && digitos.length() >= 3) {
            digitos = digitos.substring(1);
        }

        if (digitos.length() < 10) return null;
        return digitos.substring(0, 2);
    }
}
