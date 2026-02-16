package bot;

/**
 * Enum định nghĩa các trạng thái của Bot
 */
public enum BotStatus {
    REST, // Chờ respawn
    RESPAWN, // Đang respawn
    JOIN_MAP, // Join vào map
    IDLE, // Đứng yên, tìm mục tiêu
    MOVING, // Di chuyển
    ATTACKING, // Đang tấn công
    DIE, // Chết
    LEAVE_MAP, // Rời map
    AFK // AFK (cho bot đệ tử)
}
