use std::sync::LazyLock;
use std::fs;

pub static TILE_TOP_DATA: LazyLock<Vec<Vec<i32>>> = LazyLock::new(|| TileLoader::load_tile_top_data());

pub struct TileLoader;

impl TileLoader {
    fn load_tile_top_data() -> Vec<Vec<i32>> {
        let path = "data/arc/map/tile_set_info";
        let data = match fs::read(path) {
            Ok(d) => d,
            Err(e) => {
                tracing::error!("Failed to read tile_set_info: {}", e);
                return Vec::new();
            }
        };

        let mut idx = 0;
        if data.is_empty() {
            tracing::error!("tile_set_info is empty");
            return Vec::new();
        }
        let num_tile_map = data[idx] as usize;
        idx += 1;
        tracing::info!("Loading {} tilesets from tile_set_info", num_tile_map);

        let mut tile_index_tile_type = vec![Vec::new(); num_tile_map];
        let tile_type_focus = 2; // TILE_TOP

        for i in 0..num_tile_map {
            if idx >= data.len() {
                break;
            }
            let num_tile_of_map = data[idx] as usize;
            idx += 1;

            for _ in 0..num_tile_of_map {
                if idx + 4 > data.len() {
                    break;
                }
                let tile_type =
                    i32::from_be_bytes([data[idx], data[idx + 1], data[idx + 2], data[idx + 3]]);
                idx += 4;

                if idx >= data.len() {
                    break;
                }
                let num_index = data[idx] as usize;
                idx += 1;

                if tile_type == tile_type_focus {
                    let mut indices = Vec::with_capacity(num_index);
                    for _ in 0..num_index {
                        if idx >= data.len() {
                            break;
                        }
                        indices.push(data[idx] as i32);
                        idx += 1;
                    }
                    tile_index_tile_type[i] = indices;
                } else {
                    idx += num_index;
                }
            }
        }
        let total_loaded = tile_index_tile_type
            .iter()
            .filter(|v| !v.is_empty())
            .count();
        tracing::info!(
            "Successfully loaded {} tilesets with top tiles",
            total_loaded
        );
        tile_index_tile_type
    }

    pub fn read_tile_map_file(map_id: i32) -> Option<(i32, i32, Vec<Vec<i32>>)> {
        let path = format!("data/arc/map/tile_map_data/{}", map_id);
        let data = fs::read(&path).ok()?;
        if data.len() < 3 {
            return None;
        }
        let w = data[1] as usize;
        let h = data[2] as usize;
        let expected = 3 + w * h;
        if data.len() < expected {
            tracing::warn!(
                "tile_map_data/{}: expected {} bytes, got {}",
                map_id,
                expected,
                data.len()
            );
            return None;
        }
        let mut tiles: Vec<Vec<i32>> = Vec::with_capacity(h);
        let mut idx = 3;
        for _row in 0..h {
            let mut row: Vec<i32> = Vec::with_capacity(w);
            for _col in 0..w {
                if idx < data.len() {
                    row.push(data[idx] as i32);
                    idx += 1;
                }
            }
            tiles.push(row);
        }
        tracing::debug!("Loaded tile_map for map {}: {}x{} tiles", map_id, w, h);
        Some((w as i32, h as i32, tiles))
    }

    pub fn load_tile_data(map_id: i32, tile_id: i32) -> Option<TileData> {
        let (w, h, tiles) = Self::read_tile_map_file(map_id)?;

        let tile_top = if tile_id > 0 && (tile_id as usize) <= TILE_TOP_DATA.len() {
            TILE_TOP_DATA[(tile_id - 1) as usize].clone()
        } else {
            Vec::new()
        };

        Some(TileData {
            width: w,
            height: h,
            tile_map: tiles,
            tile_top,
        })
    }
}

#[derive(Debug, Clone)]
pub struct TileData {
    pub width: i32,
    pub height: i32,
    pub tile_map: Vec<Vec<i32>>,
    pub tile_top: Vec<i32>,
}
