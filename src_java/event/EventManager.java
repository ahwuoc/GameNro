package event;

import event.event_manifest.*;
import nro.server.Manager;

public class EventManager {

    private static EventManager instance;

    // Danh sách cờ sự kiện (Event Flags)
    public static boolean LUNAR_NEW_YEAR;
    public static boolean INTERNATIONAL_WOMANS_DAY;
    public static boolean CHRISTMAS;
    public static boolean HALLOWEEN;
    public static boolean HUNG_VUONG;
    public static boolean TRUNG_THU;
    public static boolean TOP_UP;
    public static boolean EVENT_POKEMON;
    public static boolean TEACHERS_DAY;
    public static boolean PHO_ANH_HAI;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void init() {
        System.out.println(">> [EventManager] Initializing Events...");
        
        // 1. Reset tất cả sự kiện về false
        resetAllEventFlags();

        // 2. Kích hoạt cờ dựa trên Config
        for (int eventId : Manager.ACTIVE_EVENTS) {
            enableEventFlag(eventId);
        }

        // 3. Khởi chạy sự kiện mặc định
        System.out.println("[EventManager] Default Event Active");
        new Default().init();

        // 4. Khởi chạy các sự kiện đã được kích hoạt
        startActiveEvents();
    }

    /**
     * Bật cờ sự kiện dựa trên ID
     */
    private void enableEventFlag(int eventId) {
        switch (eventId) {
            case 1 -> HALLOWEEN = true;
            case 2 -> INTERNATIONAL_WOMANS_DAY = true;
            case 3 -> CHRISTMAS = true;
            case 4 -> LUNAR_NEW_YEAR = true;
            case 5 -> TRUNG_THU = true;
            case 6 -> HUNG_VUONG = true;
            case 7 -> TOP_UP = true;
            case 8 -> EVENT_POKEMON = true;
            case 9 -> TEACHERS_DAY = true;
            case 10 -> PHO_ANH_HAI = true;
            default -> System.err.println("[EventManager] Unknown event ID: " + eventId);
        }
    }

    /**
     * Thực thi logic khởi tạo của các sự kiện đang bật
     */
    private void startActiveEvents() {
        if (LUNAR_NEW_YEAR) {
            logAndInit("LUNAR NEW YEAR", new LunarNewYear()::init);
        }
        if (INTERNATIONAL_WOMANS_DAY) {
            logAndInit("INTERNATIONAL WOMENS DAY", new InternationalWomensDay()::init);
        }
        if (HALLOWEEN) {
            logAndInit("HALLOWEEN", new Halloween()::init);
        }
        if (CHRISTMAS) {
            logAndInit("CHRISTMAS", new Christmas()::init);
        }
        if (HUNG_VUONG) {
            logAndInit("HUNG VUONG", new HungVuong()::init);
        }
        if (TRUNG_THU) {
            logAndInit("TRUNG THU", new TrungThu()::init);
        }
        if (TOP_UP) {
            logAndInit("TOP UP", new TopUp()::init);
        }
        if (EVENT_POKEMON) {
            logAndInit("EVENT POKEMON", new Po_Ke_Mon()::init);
        }
        if (TEACHERS_DAY) {
            logAndInit("TEACHERS DAY", new InternationalTeachersDay()::init);
        }
        if (PHO_ANH_HAI) {
            logAndInit("PHO ANH HAI", new Pho_Anh_Hai()::init);
        }
    }

    /**
     * Helper để in log và chạy init gọn gàng hơn
     */
    private void logAndInit(String eventName, Runnable initAction) {
        System.out.println("[EventManager] " + eventName);
        initAction.run();
    }

    private void resetAllEventFlags() {
        LUNAR_NEW_YEAR = false;
        INTERNATIONAL_WOMANS_DAY = false;
        CHRISTMAS = false;
        HALLOWEEN = false;
        HUNG_VUONG = false;
        TRUNG_THU = false;
        TOP_UP = false;
        EVENT_POKEMON = false;
        TEACHERS_DAY = false;
        PHO_ANH_HAI = false;
    }
}