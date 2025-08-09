use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;

static CONFIG: Lazy<Arc<Mutex<ConfigManager>>> = Lazy::new(|| {
    Arc::new(Mutex::new(ConfigManager::new()))
});

pub struct ConfigManager {
    pub server: u8,
    pub second_wait_login: u8,
    pub max_per_ip: usize,
    pub max_player: usize,
    pub rate_exp_server: u8,
    pub event_count_noel: i32,
    pub local: bool,
    
    // Game data collections
    pub map_templates: Vec<MapTemplate>,
    pub npc_templates: Vec<NpcTemplate>,
    pub mob_templates: Vec<MobTemplate>,
    pub skill_templates: Vec<SkillTemplate>,
    pub item_templates: Vec<ItemTemplate>,
    
    // Maps and collections
    pub list_maps: Vec<Map>,
    pub map_by_id: HashMap<i32, Map>,
    pub item_option_templates: Vec<ItemOptionTemplate>,
    pub mob_rewards: HashMap<i32, MobReward>,
    pub mob_template_by_id: HashMap<i32, MobTemplate>,
    pub lucky_round_rewards: Vec<ItemLuckyRound>,
    pub item_templates_map: HashMap<i32, ItemTemplate>,
    pub arr_head_2_frames: Vec<ArrHead2Frames>,
    pub npc_templates_list: Vec<NpcTemplate>,
    pub captions: Vec<String>,
    pub tasks: Vec<TaskMain>,
    pub side_tasks_template: Vec<SideTaskTemplate>,
    pub intrinsics: Vec<Intrinsic>,
    pub intrinsic_td: Vec<Intrinsic>,
    pub intrinsic_nm: Vec<Intrinsic>,
    pub intrinsic_xd: Vec<Intrinsic>,
    pub head_avatars: Vec<HeadAvatar>,
    pub flags_bags: Vec<FlagBag>,
    pub nclass: Vec<NClass>,
    pub npcs: Vec<Npc>,
    pub clans: Vec<Clan>,
    pub notify: Vec<String>,
    pub list_dhvt: Vec<DaiHoiVoThuat>,
    pub achievement_template: Vec<AchievementTemplate>,
    pub ruby_rewards: Vec<Item>,
    
    // Top queries (matching Java Manager)
    pub query_top_sm: String,
    pub query_top_sd: String,
    pub query_top_hp: String,
    pub query_top_ki: String,
    pub query_top_nv: String,
    pub query_top_sk: String,
    pub query_top_pvp: String,
    pub query_top_nhs: String,
    pub query_top_yari: String,
    pub query_top_whis: String,
    pub query_top_noel: String,
    
    // Top lists
    pub top_sm: Vec<Top>,
    pub top_sd: Vec<Top>,
    pub top_hp: Vec<Top>,
    pub top_ki: Vec<Top>,
    pub top_nv: Vec<Top>,
    pub top_sk: Vec<Top>,
    pub top_pvp: Vec<Top>,
    pub top_nhs: Vec<Top>,
    pub top_yari: Vec<Top>,
    pub top_sieu_hang: Vec<Top>,
    pub top_whis: Vec<Top>,
    pub top_noel: Vec<Top>,
    
    pub time_real_top: u64,
    
    // Item arrays (matching Java Manager)
    pub trang_bi_kich_hoat: Vec<Vec<i16>>,
    pub item_ids_tl: Vec<i16>,
    pub do_huy_diet: Vec<i16>,
    pub item_ids_nr_vip: Vec<u8>,
    pub item_ids_hd: Vec<i16>,
    pub item_da_detu: i16,
    pub hop_qua_1629: Vec<i16>,
    pub item_da_giai_khat: i16,
    pub item_ids_nr_sb: Vec<u8>,
    pub item_dns_ngk: Vec<i16>,
    pub items_cap_2: Vec<i16>,
    pub item_dc12: Vec<i16>,
    pub item_ids_kaio_awj: Vec<i16>,
    pub item_ids_tl_awj: Vec<i16>,
    pub item_ids_tl_gn: Vec<i16>,
    pub item_ids_kaio_gn: Vec<i16>,
    pub item_ids_luonglong_awj: Vec<i16>,
    pub item_ids_luonglong_gn: Vec<i16>,
    pub item_spl_vip: Vec<i16>,
    pub aotd: Vec<i16>,
    pub quantd: Vec<i16>,
    pub gangtd: Vec<i16>,
    pub giaytd: Vec<i16>,
    pub aoxd: Vec<i16>,
    pub quanxd: Vec<i16>,
    pub gangxd: Vec<i16>,
    pub giayxd: Vec<i16>,
    pub aonm: Vec<i16>,
    pub quannm: Vec<i16>,
    pub gangnm: Vec<i16>,
    pub giaynm: Vec<i16>,
    pub rada_skh_vip: Vec<i16>,
    pub manhts: Vec<i16>,
    pub thucan: Vec<i16>,
    pub do_skh_vip: Vec<Vec<Vec<i16>>>,
}

impl ConfigManager {
    pub fn new() -> Self {
        ConfigManager {
            // Server constants
            server: 1,
            second_wait_login: 5,
            max_per_ip: 5,
            max_player: 1000,
            rate_exp_server: 2,
            event_count_noel: 0,
            local: false,
            
            // Initialize collections
            map_templates: Vec::new(),
            npc_templates: Vec::new(),
            mob_templates: Vec::new(),
            skill_templates: Vec::new(),
            item_templates: Vec::new(),
            list_maps: Vec::new(),
            map_by_id: HashMap::new(),
            item_option_templates: Vec::new(),
            mob_rewards: HashMap::new(),
            mob_template_by_id: HashMap::new(),
            lucky_round_rewards: Vec::new(),
            item_templates_map: HashMap::new(),
            arr_head_2_frames: Vec::new(),
            npc_templates_list: Vec::new(),
            captions: Vec::new(),
            tasks: Vec::new(),
            side_tasks_template: Vec::new(),
            intrinsics: Vec::new(),
            intrinsic_td: Vec::new(),
            intrinsic_nm: Vec::new(),
            intrinsic_xd: Vec::new(),
            head_avatars: Vec::new(),
            flags_bags: Vec::new(),
            nclass: Vec::new(),
            npcs: Vec::new(),
            clans: Vec::new(),
            notify: Vec::new(),
            list_dhvt: Vec::new(),
            achievement_template: Vec::new(),
            ruby_rewards: Vec::new(),
            
            // Top queries
            query_top_sm: "SELECT p.id, CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(p.data_point, ',', 2), ',', -1) AS UNSIGNED) AS sm FROM player p INNER JOIN account a ON p.account_id = a.id WHERE a.is_admin = 0 ORDER BY sm DESC LIMIT 20;".to_string(),
            query_top_sd: "SELECT id, CAST( split_str(data_point,',',8) AS UNSIGNED) AS sd FROM player ORDER BY CAST( split_str(data_point,',',8)  AS UNSIGNED) DESC LIMIT 20;".to_string(),
            query_top_hp: "SELECT id, CAST( split_str(data_point,',',6) AS UNSIGNED) AS hp FROM player ORDER BY CAST( split_str(data_point,',',6)  AS UNSIGNED) DESC LIMIT 20;".to_string(),
            query_top_ki: "SELECT id, CAST( split_str(data_point,',',7) AS UNSIGNED) AS ki FROM player ORDER BY CAST( split_str(data_point,',',7)  AS UNSIGNED) DESC LIMIT 20;".to_string(),
            query_top_nv: "SELECT p.id, CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(p.data_task, ',', 1), '[', -1) AS UNSIGNED) AS nv, CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(p.data_task, ',', 2), ',', -1) AS UNSIGNED) AS second_value FROM player p INNER JOIN account a ON p.account_id = a.id WHERE a.is_admin = 0 ORDER BY nv DESC, second_value DESC, CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(p.data_point, ',', 2), ',', -1) AS UNSIGNED) DESC LIMIT 50;".to_string(),
            query_top_sk: "SELECT id, CAST( SUBSTRING_INDEX(SUBSTRING_INDEX(data_inventory, ',', 5), ',', -1) AS UNSIGNED) AS event FROM player ORDER BY event DESC LIMIT 20;".to_string(),
            query_top_pvp: "SELECT id, CAST( pointPvp AS UNSIGNED) AS pointPvp FROM player ORDER BY CAST( pointPvp AS UNSIGNED) DESC LIMIT 50;".to_string(),
            query_top_nhs: "SELECT p.id, CAST(p.NguHanhSonPoint AS UNSIGNED) AS NguHanhSonPoint FROM player p INNER JOIN account a ON p.account_id = a.id WHERE a.is_admin = 0 ORDER BY NguHanhSonPoint DESC LIMIT 50;".to_string(),
            query_top_yari: "SELECT p.id, CAST(p.cap_yari AS UNSIGNED) AS topYari FROM player p INNER JOIN account a ON p.account_id = a.id WHERE a.is_admin = 0 ORDER BY topYari DESC LIMIT 20".to_string(),
            query_top_whis: "SELECT name, player.id, gender, items_body, CAST( JSON_EXTRACT(data_luyentap, '$[5]') AS UNSIGNED) AS top, CAST( JSON_EXTRACT(data_luyentap, '$[6]') AS UNSIGNED) AS time, CAST( JSON_EXTRACT(data_luyentap, '$[7]') AS UNSIGNED) AS lasttime FROM player INNER JOIN account ON account.id = player.account_id WHERE account.ban = 0 AND CAST( JSON_EXTRACT(data_luyentap, '$[5]') AS UNSIGNED) > 0 ORDER BY CAST( JSON_EXTRACT(data_luyentap, '$[5]') AS UNSIGNED) DESC, CAST( JSON_EXTRACT(data_luyentap, '$[6]') AS UNSIGNED) ASC LIMIT 100;".to_string(),
            query_top_noel: "SELECT id, CAST( point_noel AS UNSIGNED) AS noel FROM player ORDER BY CAST( point_noel AS UNSIGNED) DESC LIMIT 20".to_string(),
            
            // Initialize top lists
            top_sm: Vec::new(),
            top_sd: Vec::new(),
            top_hp: Vec::new(),
            top_ki: Vec::new(),
            top_nv: Vec::new(),
            top_sk: Vec::new(),
            top_pvp: Vec::new(),
            top_nhs: Vec::new(),
            top_yari: Vec::new(),
            top_sieu_hang: Vec::new(),
            top_whis: Vec::new(),
            top_noel: Vec::new(),
            
            time_real_top: 0,
            
            // Item arrays (matching Java Manager)
            trang_bi_kich_hoat: vec![vec![0, 6, 21, 27], vec![1, 7, 22, 28], vec![2, 8, 23, 29]],
            item_ids_tl: vec![555, 557, 559, 556, 558, 560, 562, 564, 566, 563, 565, 567, 561],
            do_huy_diet: vec![233, 237, 241, 245, 249, 253, 257, 261, 265, 269, 273, 277],
            item_ids_nr_vip: vec![14, 15],
            item_ids_hd: vec![2003, 2004, 2005],
            item_da_detu: 1015,
            hop_qua_1629: vec![1499, 1015],
            item_da_giai_khat: 1499,
            item_ids_nr_sb: vec![16, 17],
            item_dns_ngk: vec![1499, 674],
            items_cap_2: vec![1100, 1101, 1102, 1103],
            item_dc12: vec![233, 237, 241, 245, 249, 253, 257, 261, 265, 269, 273, 277],
            item_ids_kaio_awj: vec![232, 236, 240, 244, 248, 252, 268, 272, 276],
            item_ids_tl_awj: vec![555, 557, 559, 556, 558, 560, 563, 565, 567],
            item_ids_tl_gn: vec![562, 564, 566, 561],
            item_ids_kaio_gn: vec![256, 260, 264, 280],
            item_ids_luonglong_awj: vec![233, 237, 241, 245, 249, 253, 269, 273, 277],
            item_ids_luonglong_gn: vec![257, 261, 265, 281],
            item_spl_vip: vec![233, 237, 241, 245, 249, 253, 257, 261, 265, 269, 273, 277],
            aotd: vec![136, 137, 138, 139, 230, 231, 232, 233],
            quantd: vec![140, 141, 142, 143, 242, 243, 244, 245],
            gangtd: vec![144, 145, 146, 147, 254, 255, 256, 257],
            giaytd: vec![148, 149, 150, 151, 266, 267, 268, 269],
            aoxd: vec![168, 169, 170, 171, 238, 239, 240, 241],
            quanxd: vec![172, 173, 174, 175, 250, 251, 252, 253],
            gangxd: vec![176, 177, 178, 179, 262, 263, 264, 265],
            giayxd: vec![180, 181, 182, 183, 274, 275, 276, 277],
            aonm: vec![152, 153, 154, 155, 234, 235, 236, 237],
            quannm: vec![156, 157, 158, 159, 246, 247, 248, 249],
            gangnm: vec![160, 161, 162, 163, 258, 259, 260, 261],
            giaynm: vec![164, 165, 166, 167, 270, 271, 272, 273],
            rada_skh_vip: vec![184, 185, 186, 187, 278, 279, 280, 281],
            manhts: vec![1067, 1068, 1069, 1070, 1066],
            thucan: vec![663, 664, 665, 666, 667],
            do_skh_vip: vec![
                vec![vec![136, 137, 138, 139, 230, 231, 232, 233], vec![140, 141, 142, 143, 242, 243, 244, 245], vec![144, 145, 146, 147, 254, 255, 256, 257], vec![148, 149, 150, 151, 266, 267, 268, 269]],
                vec![vec![152, 153, 154, 155, 234, 235, 236, 237], vec![156, 157, 158, 159, 246, 247, 248, 249], vec![160, 161, 162, 163, 258, 259, 260, 261], vec![164, 165, 166, 167, 270, 271, 272, 273]],
                vec![vec![168, 169, 170, 171, 238, 239, 240, 241], vec![172, 173, 174, 175, 250, 251, 252, 253], vec![176, 177, 178, 179, 262, 263, 264, 265], vec![180, 181, 182, 183, 274, 275, 276, 277]]
            ],
        }
    }

    pub fn get_instance() -> Arc<Mutex<ConfigManager>> {
        Arc::clone(&CONFIG)
    }
}

#[derive(Debug, Clone)]
pub struct MapTemplate {
    pub id: i32,
    pub name: String,
    pub planet_id: i32,
    pub bg_type: i32,
    pub tile_id: i32,
    pub bg_id: i32,
    pub zones: i32,
    pub max_player: i32,
}

#[derive(Debug, Clone)]
pub struct NpcTemplate {
    pub id: i32,
    pub name: String,
    pub head: i32,
    pub body: i32,
    pub leg: i32,
    pub avatar: i32,
}

#[derive(Debug, Clone)]
pub struct MobTemplate {
    pub id: i32,
    pub name: String,
    pub hp: i64,
    pub level: i32,
    pub damage: i32,
    pub defense: i32,
    pub exp: i32,
}

#[derive(Debug, Clone)]
pub struct SkillTemplate {
    pub id: i32,
    pub name: String,
    pub level: i32,
    pub damage: i32,
    pub mp_cost: i32,
    pub cooldown: i32,
}

#[derive(Debug, Clone)]
pub struct ItemTemplate {
    pub id: i32,
    pub name: String,
    pub item_type: i32,
    pub level: i32,
    pub damage: i32,
    pub defense: i32,
    pub hp: i32,
    pub mp: i32,
}

#[derive(Debug, Clone)]
pub struct Map {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct ItemOptionTemplate {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct MobReward {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct ItemLuckyRound {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct ArrHead2Frames {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct Intrinsic {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct HeadAvatar {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct FlagBag {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct NClass {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct Npc {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct Clan {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct DaiHoiVoThuat {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct AchievementTemplate {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct Item {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct Top {
    pub id: i32,
    pub name: String,
    pub value: i64,
}

#[derive(Debug, Clone)]
pub struct TaskMain {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct SideTaskTemplate {
    pub id: i32,
    pub name: String,
}

impl SideTaskTemplate {
    pub fn new() -> Self {
        Self {
            id: -1,
            name: String::new(),
        }
    }
}
