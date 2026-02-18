package zalo.services;

import zalo.utils.Apis;
import zalo.apis.SendMessageApi;
import zalo.models.ThreadType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;

public class NroNotifyService {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static class BossInfo {
        String bossName;
        String mapName;
        String zoneInfo;
        
        BossInfo(String bossName, String mapName, String zoneInfo) {
            this.bossName = bossName;
            this.mapName = mapName;
            this.zoneInfo = zoneInfo;
        }
    }
    
    private static NroNotifyService instance;
    private final Set<String> registeredGroups;
    private final Set<String> firstSpawnBosses;
    private Apis api;
    private long serverStartTime;
    private long lastRequestTime;
    private final List<BossInfo> pendingBosses;
    private Timer batchTimer;
    private static final long BATCH_DELAY_MS = 2000;
    private static final long FIRST_SPAWN_WINDOW_MS = 300000;
    private static final long RESET_WINDOW_MS = 600000;
    
    private NroNotifyService() {
        this.registeredGroups = ConcurrentHashMap.newKeySet();
        this.firstSpawnBosses = ConcurrentHashMap.newKeySet();
        this.serverStartTime = System.currentTimeMillis();
        this.lastRequestTime = System.currentTimeMillis();
        this.pendingBosses = Collections.synchronizedList(new ArrayList<>());
    }
    
    public static NroNotifyService gI() {
        if (instance == null) {
            instance = new NroNotifyService();
        }
        return instance;
    }
    
    public void setApi(Apis api) {
        this.api = api;
    }
    
    public void registerGroup(String threadId) {
        this.registeredGroups.add(threadId);
        DatabaseService.gI().updateBossNotify(threadId, true);
    }
    
    public void unregisterGroup(String threadId) {
        this.registeredGroups.remove(threadId);
        DatabaseService.gI().updateBossNotify(threadId, false);
    }
    
    public void loadFromDatabase() {
        List<String> groups = DatabaseService.gI().loadBossNotifyGroups();
        this.registeredGroups.clear();
        this.registeredGroups.addAll(groups);
    }
    
    public boolean isRegistered(String threadId) {
        return this.registeredGroups.contains(threadId);
    }
    
    public int getRegisteredCount() {
        return this.registeredGroups.size();
    }
    
    public void notifyBossSpawn(String bossName, String mapName) {
        notifyBossSpawn(bossName, mapName, null);
    }
    
    public void notifyBossSpawn(String bossName, String mapName, String zoneInfo) {
        if (api == null) {
            return;
        }
        
        if (registeredGroups.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        lastRequestTime = currentTime;
        
        if (timeSinceLastRequest > RESET_WINDOW_MS) {
            firstSpawnBosses.clear();
            serverStartTime = currentTime;
        }
        
        String bossKey = bossName + "|" + mapName;
        long timeSinceStart = currentTime - serverStartTime;
        
        if (timeSinceStart < FIRST_SPAWN_WINDOW_MS) {
            if (firstSpawnBosses.contains(bossKey)) {
                firstSpawnBosses.remove(bossKey);
            } else {
                firstSpawnBosses.add(bossKey);
                return;
            }
        }
        
        synchronized (pendingBosses) {
            pendingBosses.add(new BossInfo(bossName, mapName, zoneInfo));
        }
        
        scheduleBatchSend();
    }
    
    private void scheduleBatchSend() {
        synchronized (this) {
            if (batchTimer != null) {
                batchTimer.cancel();
            }
            
            batchTimer = new Timer();
            batchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    flushPendingBosses();
                }
            }, BATCH_DELAY_MS);
        }
    }
    
    private void flushPendingBosses() {
        List<BossInfo> bossesToSend;
        
        synchronized (pendingBosses) {
            if (pendingBosses.isEmpty()) {
                return;
            }
            bossesToSend = new ArrayList<>(pendingBosses);
            pendingBosses.clear();
        }
        
        if (bossesToSend.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String timeStr = sdf.format(new Date(currentTime));
        
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("🔥 ");
        
        if (bossesToSend.size() == 1) {
            BossInfo boss = bossesToSend.get(0);
            messageBuilder.append("BOSS ").append(boss.bossName).append(" ĐANG SPAWN Ở MAP ").append(boss.mapName);
            if (boss.zoneInfo != null && !boss.zoneInfo.isEmpty()) {
                messageBuilder.append(" ").append(boss.zoneInfo);
            }
        } else {
            messageBuilder.append("CÓ ").append(bossesToSend.size()).append(" BOSS ĐANG SPAWN:\n");
            for (int i = 0; i < bossesToSend.size(); i++) {
                BossInfo boss = bossesToSend.get(i);
                messageBuilder.append(i + 1).append(". BOSS ").append(boss.bossName)
                    .append(" Ở MAP ").append(boss.mapName);
                if (boss.zoneInfo != null && !boss.zoneInfo.isEmpty()) {
                    messageBuilder.append(" ").append(boss.zoneInfo);
                }
                if (i < bossesToSend.size() - 1) {
                    messageBuilder.append("\n");
                }
            }
        }
        
        messageBuilder.append("\n⏰ ").append(timeStr);
        
        String message = messageBuilder.toString();
        
        for (String threadId : registeredGroups) {
            try {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg(message);
                api.sendMessage.sendMessage(msgContent, threadId, ThreadType.GROUP).get();
            } catch (Exception e) {
            }
        }
    }
    
    public void resetFirstSpawn() {
        firstSpawnBosses.clear();
        serverStartTime = System.currentTimeMillis();
    }
}

