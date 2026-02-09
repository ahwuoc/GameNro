use std::collections::HashMap;
use std::sync::LazyLock;

use tokio::sync::RwLock;

use crate::shop::shop_dao::ShopDao;

// Sử dụng LazyLock + tokio::sync::RwLock (async-safe)
static SHOP_CACHE: LazyLock<RwLock<HashMap<i32, Vec<ShopMenuItem>>>> =
    LazyLock::new(|| RwLock::new(HashMap::new()));

#[derive(Clone, Debug)]
pub struct ShopMenuItem {
    pub tag_name: String,
    pub display_name: String,
    pub npc_id: i32,
    pub is_gender: Option<i32>,
}

impl ShopMenuItem {
    /// Kiểm tra shop có phù hợp với gender của player không
    pub fn is_available_for_gender(&self, player_gender: i32) -> bool {
        match self.is_gender {
            None => true,                                // Không yêu cầu gender -> Bán cho tất cả
            Some(required) => required == player_gender, // Phải đúng hành tinh yêu cầu
        }
    }

    /// Lấy message từ chối
    pub fn get_gender_reject_message(&self) -> &'static str {
        match self.is_gender {
            Some(0) => "Xin lỗi, shop này chỉ dành cho người Trái Đất!",
            Some(1) => "Xin lỗi, shop này chỉ dành cho người Namek!",
            Some(2) => "Xin lỗi, shop này chỉ dành cho người Xayda!",
            _ => "Xin lỗi, bạn không thể mua hàng ở đây!",
        }
    }
}

pub struct ShopMenuManager;

impl ShopMenuManager {
    /// Load tất cả shop của NPC (không filter gender)
    pub async fn load_shops_for_npc(npc_id: i32) -> anyhow::Result<Vec<ShopMenuItem>> {
        // Check cache first
        {
            let cache = SHOP_CACHE.read().await;
            if let Some(items) = cache.get(&npc_id) {
                return Ok(items.clone());
            }
        }

        // Load from database
        let shops = ShopDao::get_shop_by_npc_id(npc_id).await?;
        let items: Vec<ShopMenuItem> = shops
            .into_iter()
            .filter_map(|s| {
                let tag = s.tag_name?;
                let display = s.display_name.unwrap_or_else(|| tag.clone());
                Some(ShopMenuItem {
                    tag_name: tag,
                    display_name: display,
                    npc_id: s.npc_id,
                    is_gender: s.is_gender,
                })
            })
            .collect();

        // Update cache
        {
            let mut cache = SHOP_CACHE.write().await;
            cache.insert(npc_id, items.clone());
        }

        Ok(items)
    }

    /// Lấy danh sách shop phù hợp với gender của player
    pub async fn get_shops_for_player(
        npc_id: i32,
        player_gender: i32,
    ) -> anyhow::Result<Vec<ShopMenuItem>> {
        let items = Self::load_shops_for_npc(npc_id).await?;
        Ok(items
            .into_iter()
            .filter(|i| i.is_available_for_gender(player_gender))
            .collect())
    }

    /// Lấy danh sách tên hiển thị (filter theo gender)
    pub async fn get_menu_options(
        npc_id: i32,
        player_gender: Option<i32>,
    ) -> anyhow::Result<Vec<String>> {
        let items = Self::load_shops_for_npc(npc_id).await?;
        tracing::info!(
            "ShopMenuManager: Loaded {} items for npc_id={}",
            items.len(),
            npc_id
        );

        // Nếu có player_gender, filter theo nó
        let filtered: Vec<String> = match player_gender {
            Some(gender) => items
                .into_iter()
                .filter(|i| i.is_available_for_gender(gender))
                .map(|i| i.display_name)
                .collect(),
            None => items.into_iter().map(|i| i.display_name).collect(),
        };

        Ok(filtered)
    }

    /// Lấy shop item theo index (filter theo gender)
    pub async fn get_shop_by_index(
        npc_id: i32,
        index: usize,
        player_gender: Option<i32>,
    ) -> anyhow::Result<Option<ShopMenuItem>> {
        let items = Self::load_shops_for_npc(npc_id).await?;

        let filtered: Vec<ShopMenuItem> = match player_gender {
            Some(gender) => items
                .into_iter()
                .filter(|i| i.is_available_for_gender(gender))
                .collect(),
            None => items,
        };

        Ok(filtered.get(index).cloned())
    }

    /// Lấy tag_name từ index (deprecated - use get_shop_by_index)
    pub async fn get_shop_tag_by_index(
        npc_id: i32,
        index: usize,
    ) -> anyhow::Result<Option<String>> {
        let items = Self::load_shops_for_npc(npc_id).await?;
        Ok(items.get(index).map(|i| i.tag_name.clone()))
    }

    pub async fn clear_cache() {
        let mut cache = SHOP_CACHE.write().await;
        cache.clear();
    }

    pub async fn invalidate_npc(npc_id: i32) {
        let mut cache = SHOP_CACHE.write().await;
        cache.remove(&npc_id);
    }
}
