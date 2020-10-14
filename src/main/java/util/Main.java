package util;

import bolsa.BolsaDeValores;

public class Main {

    public static final String SERVER_URI = "amqp://vkcgnjke:ixmgI_OE-PXdTPDEob3vTlOXLfc2Iu7E@woodpecker.rmq.cloudamqp.com/vkcgnjke";
    public static final String QUEUE_NAME = "BROKER";
    public static final String NOTIFICATION_QUEUE = "BOLSA";
    public static final String DATE_FORMAT = "dd/MM/yyyy hh:mm";
    public static final BolsaDeValores BOLSA_DE_VALORES = new BolsaDeValores();

}
