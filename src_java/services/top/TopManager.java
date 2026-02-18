package services.top;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import utils.Logger;

/**
 * Quản lý các Top Ranking và Auto Gift
 * Refactored by Gemini
 */
public class TopManager {

    // Sử dụng List interface để linh hoạt hơn
    public static final List<TopTemplate> TOP_TEMPLATE = new ArrayList<>();
    public static final List<TOPAUTO> AUTO_GIFT_TOPS = new ArrayList<>();
    public static String[] SELECT;

    // Inner class - Data Holder
    public static class TopTemplate {
        public int id;
        public String name;
        public String query;
        public final List<TOP> tops = new ArrayList<>();

        public TopTemplate(int id, String name, String query) {
            this.id = id;
            this.name = name;
            this.query = query;
        }
    }

    /**
     * Tải dữ liệu Top từ Database
     */
    public static void loadTop(Connection con) {
        // Xóa dữ liệu cũ trước khi load để tránh duplicate khi reload
        TOP_TEMPLATE.clear();
        AUTO_GIFT_TOPS.clear();

        String sql = "SELECT * FROM top_template";
        try (PreparedStatement ps = con.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Tạo template
                TopTemplate template = new TopTemplate(
                    rs.getInt("id"), 
                    rs.getString("name"), 
                    rs.getString("query")
                );
                
                // Add vào list quản lý
                TOP_TEMPLATE.add(template);
                
                // Khởi tạo Auto Gift cho Top tương ứng
                AUTO_GIFT_TOPS.add(new TOPAUTO(rs, template));
            }

            // Log thông tin
            Logger.log("TOPS -> (" + TOP_TEMPLATE.size() + ")");
            Logger.log("AUTO_GIFT_TOPS -> (" + AUTO_GIFT_TOPS.size() + ")");

            // Load chi tiết danh sách người chơi trong Top
            TopService.gI().loadListTop(con);

            // Cập nhật menu NPC
            setSelectNpc();

        } catch (Exception e) {
            Logger.logException(TopManager.class, e);
        }
    }

    /**
     * Cập nhật danh sách hiển thị cho NPC
     */
    public static void setSelectNpc() {
        SELECT = TOP_TEMPLATE.stream()
                .map(t -> "Top\n" + t.name + "\n[" + t.id + "]")
                .toArray(String[]::new);
    }
}