package com.winterapocalypse.client;

/** Последние значения, пришедшие от сервера. Без клиентских импортов. */
public class ClientTempData {
    public static float body = 36.6f;
    public static float ambient = 20.0f;
    public static float day = 0.0f;
    public static float blizzard = 0.0f;   // 0..1, сила метели над игроком
    public static float protection = 0.0f; // 0..1, защита одеждой

    public static void set(float b, float a, float d, float bl, float pr) {
        body = b;
        ambient = a;
        day = d;
        blizzard = bl;
        protection = pr;
    }
}
